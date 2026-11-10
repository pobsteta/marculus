package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.TotauxMartelage
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 018 « la liste des contextes compte les tiges nettes » : tests d'acceptation écrits
 * depuis `pilotage/missions/018-liste-tiges-nettes.md` (rubrique « Tests »), sans lire l'implémentation.
 *
 * Signature imposée au `dev` (module data, package `io.github.pobsteta.marculus.data`) :
 *  - `ResumeContexte` gagne `val nbTiges: Int` (net : PLUS moins ANNULATION, pondéré par `quantite`),
 *    à côté de `nbEvenements` (compte des lignes du journal, inchangé) ; les tests ne lisent
 *    ces champs que par `MartelageRepository.resumes()` (pas par le constructeur, dont l'ordre reste libre).
 *  - `verrouille` et `nbEvenements` gardent leur sens actuel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListeTigesNettesTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db: MarculusDatabase =
        Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
    private val depot = MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao())

    @After
    fun fermer() = db.close()

    private fun contexte(id: String, exporte: Boolean = false) = ContexteEntity(
        id = id, nom = "Contexte $id", mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = exporte, dateCreation = 0,
        operateur = "A", modifie = 1,
    )

    private var n = 0
    private fun evt(contexteId: String, action: String = "PLUS", quantite: Int = 1, essence: String = "CHE", classe: Int = 40) =
        TigeEntity(
            uuid = "u${n++}", contexteId = contexteId, essence = essence, classe = classe, action = action,
            horodatage = n.toLong(), quantite = quantite, hauteurTexte = null, qualiteArbre = null,
            latitude = null, longitude = null, operateur = "A", modifie = 1,
        )

    private suspend fun resume(id: String) = depot.resumes().first().single { it.contexte.id == id }

    @Test
    fun `plus, plus, annulation - la liste annonce 1, le journal compte 3 evenements`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        listOf(evt("c"), evt("c"), evt("c", "ANNULATION")).forEach { db.tigeDao().inserer(it) }
        val r = resume("c")
        assertEquals(1, r.nbTiges)
        assertEquals(3, r.nbEvenements)
    }

    @Test
    fun `quantite 3 en increment - la liste annonce 3`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        db.tigeDao().inserer(evt("c", quantite = 3))
        val r = resume("c")
        assertEquals(3, r.nbTiges)
        assertEquals(1, r.nbEvenements)
    }

    @Test
    fun `annulation ponderee par quantite - plus 5 moins 2 donne 3`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        db.tigeDao().inserer(evt("c", quantite = 5))
        db.tigeDao().inserer(evt("c", "ANNULATION", quantite = 2))
        assertEquals(3, resume("c").nbTiges)
    }

    @Test
    fun `plus puis annulation - net 0, journal non vide, contexte reste verrouille tant que non exporte`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        db.tigeDao().inserer(evt("c"))
        db.tigeDao().inserer(evt("c", "ANNULATION"))
        val r = resume("c")
        assertEquals(0, r.nbTiges)
        assertEquals(2, r.nbEvenements)
        assertTrue(r.verrouille)
    }

    @Test
    fun `plus puis annulation sur contexte exporte - net 0, non verrouille`() = runBlocking {
        db.contexteDao().inserer(contexte("c", exporte = true))
        db.tigeDao().inserer(evt("c"))
        db.tigeDao().inserer(evt("c", "ANNULATION"))
        val r = resume("c")
        assertEquals(0, r.nbTiges)
        assertEquals(2, r.nbEvenements)
        assertFalse(r.verrouille)
    }

    @Test
    fun `contexte sans tige - 0 et 0, non verrouille`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        val r = resume("c")
        assertEquals(0, r.nbTiges)
        assertEquals(0, r.nbEvenements)
        assertFalse(r.verrouille)
    }

    @Test
    fun `plusieurs contextes - chacun son net, sans melange`() = runBlocking {
        listOf("a", "b", "c", "vide").forEach { db.contexteDao().inserer(contexte(it)) }
        listOf(evt("a"), evt("a"), evt("a", "ANNULATION")).forEach { db.tigeDao().inserer(it) }   // net 1, 3 evt
        db.tigeDao().inserer(evt("b", quantite = 4))                                              // net 4, 1 evt
        listOf(evt("c"), evt("c", "ANNULATION")).forEach { db.tigeDao().inserer(it) }             // net 0, 2 evt
        val par = depot.resumes().first().associateBy { it.contexte.id }
        assertEquals(4, par.size)
        assertEquals(1 to 3, par.getValue("a").let { it.nbTiges to it.nbEvenements })
        assertEquals(4 to 1, par.getValue("b").let { it.nbTiges to it.nbEvenements })
        assertEquals(0 to 2, par.getValue("c").let { it.nbTiges to it.nbEvenements })
        assertEquals(0 to 0, par.getValue("vide").let { it.nbTiges to it.nbEvenements })
    }

    @Test
    fun `le net de la liste egale TotauxMartelage totalContexte sur le meme journal`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        listOf(
            evt("c", essence = "CHE", classe = 40), evt("c", essence = "CHE", classe = 40, quantite = 3),
            evt("c", essence = "HET", classe = 35, quantite = 2), evt("c", "ANNULATION", essence = "CHE", classe = 40),
            evt("c", essence = "FRE", classe = 50), evt("c", "ANNULATION", essence = "HET", classe = 35, quantite = 2),
        ).forEach { db.tigeDao().inserer(it) }
        val attendu = TotauxMartelage(depot.journalInstantane("c")).totalContexte()
        assertEquals(4, attendu) // garde-fou : 1+3+2-1+1-2
        assertEquals(attendu, resume("c").nbTiges)
    }

    @Test
    fun `net negatif - affiche tel quel, pas masque`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        db.tigeDao().inserer(evt("c", "ANNULATION"))
        val r = resume("c")
        assertEquals(-1, r.nbTiges)
        assertEquals(1, r.nbEvenements)
        assertEquals(TotauxMartelage(depot.journalInstantane("c")).totalContexte(), r.nbTiges)
    }

    @Test
    fun `le Flow suit les ajouts et annulations sans relancer la collecte`() = runBlocking {
        db.contexteDao().inserer(contexte("c"))
        val vus = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val collecte = launch(Dispatchers.IO) {
            depot.resumes().collect { l -> l.singleOrNull { it.contexte.id == "c" }?.let { vus += it.nbTiges } }
        }
        suspend fun attendre(net: Int) = withTimeout(10_000) { while (vus.lastOrNull() != net) delay(20) }
        try {
            attendre(0)
            db.tigeDao().inserer(evt("c", quantite = 2)); attendre(2)
            db.tigeDao().inserer(evt("c")); attendre(3)
            db.tigeDao().inserer(evt("c", "ANNULATION")); attendre(2)
        } finally {
            collecte.cancel()
        }
    }
}
