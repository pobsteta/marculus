package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 016 « Ouvrir avec Marculus » : tests d'acceptation écrits depuis la fiche
 * `pilotage/missions/016-ouvrir-avec-marculus.md` (rubrique « Tests »), sans lire l'implémentation.
 *
 * Signatures imposées au `dev` (module data, package `io.github.pobsteta.marculus.data`) :
 *  - `object FichierMarsync { fun estMarsync(nom: String?, contenu: String): Boolean }`
 *  - `class ReceptionMarsync(sauvegarde: SauvegardeRepository)` avec
 *      `val enAttente: ReceptionMarsync.Proposition?`        (`data class Proposition(val nom: String?, val contenu: String)`)
 *      `fun recevoir(nom: String?, contenu: String): Boolean` (false et rien en attente si non reconnu)
 *      `suspend fun confirmer(): Boolean`                     (fusionne via fusionnerJson ; false s'il n'y a rien en attente)
 *      `fun annuler()`
 *  `confirmer()` et `annuler()` vident `enAttente` : un Intent n'est consommé qu'une fois.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OuvrirAvecMarculusTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val bases = mutableListOf<MarculusDatabase>()

    private fun base(): MarculusDatabase =
        Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build().also { bases += it }

    private fun depot(db: MarculusDatabase) =
        SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), db.mergeDao(), ReferentielsRepository(ctx))

    @After
    fun fermer() = bases.forEach { it.close() }

    private fun contexte(id: String, nom: String) = ContexteEntity(
        id = id, nom = nom, mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
        operateur = "A", modifie = 1,
    )

    private fun tige(uuid: String, contexteId: String) = TigeEntity(
        uuid = uuid, contexteId = contexteId, essence = "CHE", classe = 40, action = "PLUS", horodatage = 1,
        quantite = 1, hauteurTexte = null, qualiteArbre = null, latitude = null, longitude = null,
        operateur = "A", modifie = 1,
    )

    /** Téléphone A : un contexte « Parcelle 12 » de trois tiges, exporté comme « Partager (.marsync) ». */
    private fun marsyncDeA(): String = runBlocking {
        val a = base()
        a.contexteDao().inserer(contexte("p12", "Parcelle 12"))
        listOf("t1", "t2", "t3").forEach { a.tigeDao().inserer(tige(it, "p12")) }
        depot(a).exporterContexteJson("p12")
    }

    // ---- Reconnaissance (fonction pure) ----

    @Test
    fun `reconnait un marsync produit par exporterContexteJson, avec son nom`() {
        assertTrue(FichierMarsync.estMarsync("Parcelle 12-A-2026-11-09_1030.marsync", marsyncDeA()))
    }

    @Test
    fun `sans nom lisible, le contenu decide - accepte`() {
        assertTrue(FichierMarsync.estMarsync(null, marsyncDeA()))
    }

    @Test
    fun `accepte une sauvegarde complete`() = runBlocking {
        val a = base()
        a.contexteDao().inserer(contexte("p12", "Parcelle 12"))
        assertTrue(FichierMarsync.estMarsync("sauvegarde.json", depot(a).exporterJson()))
    }

    @Test
    fun `accepte un JSON avec seulement des tiges, ou seulement des contextes`() {
        assertTrue(FichierMarsync.estMarsync(null, """{"version":1,"tiges":[]}"""))
        assertTrue(FichierMarsync.estMarsync(null, """{"version":1,"contextes":[]}"""))
    }

    @Test
    fun `refuse un CSV, meme nomme en marsync`() {
        val csv = "contexte;essence;classe;quantite\nParcelle 12;CHE;40;3\n"
        assertFalse(FichierMarsync.estMarsync("martelage.csv", csv))
        assertFalse(FichierMarsync.estMarsync(null, csv))
        assertFalse(FichierMarsync.estMarsync("faux.marsync", csv))
    }

    @Test
    fun `refuse un fichier vide ou blanc`() {
        assertFalse(FichierMarsync.estMarsync("vide.marsync", ""))
        assertFalse(FichierMarsync.estMarsync(null, ""))
        assertFalse(FichierMarsync.estMarsync(null, "  \n "))
    }

    @Test
    fun `refuse un binaire`() {
        val binaire = String(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x7F), Charsets.ISO_8859_1)
        assertFalse(FichierMarsync.estMarsync("photo.jpg", binaire))
        assertFalse(FichierMarsync.estMarsync(null, binaire))
    }

    @Test
    fun `refuse un JSON sans contextes ni tiges ou qui n'est pas un objet`() {
        assertFalse(FichierMarsync.estMarsync(null, "{}"))
        assertFalse(FichierMarsync.estMarsync(null, """{"version":1,"configs":[]}"""))
        assertFalse(FichierMarsync.estMarsync(null, """{"nom":"autre chose"}"""))
        assertFalse(FichierMarsync.estMarsync(null, "[1,2,3]"))
        assertFalse(FichierMarsync.estMarsync(null, """{"contextes":"pas un tableau"}"""))
        assertFalse(FichierMarsync.estMarsync(null, """{"contextes":[}"""))
    }

    // ---- Réception : confirmation, annulation, consommation unique ----

    @Test
    fun `recevoir ne fusionne pas, confirmer fusionne - le contexte arrive avec ses tiges`() = runBlocking {
        val json = marsyncDeA()
        val b = base()
        val reception = ReceptionMarsync(depot(b))

        assertTrue(reception.recevoir("Parcelle 12-A.marsync", json))
        assertEquals("Parcelle 12-A.marsync", reception.enAttente?.nom)
        assertTrue(b.contexteDao().toutes().isEmpty(), "rien n'est écrit avant la confirmation")

        assertTrue(reception.confirmer())
        assertEquals(listOf("p12"), b.contexteDao().toutes().map { it.id })
        assertEquals(3, b.tigeDao().listeParContexte("p12").size)
        assertNull(reception.enAttente, "l'Intent est consommé")
    }

    @Test
    fun `annuler n'ecrit rien en base`() = runBlocking {
        val b = base()
        b.contexteDao().inserer(contexte("local", "Local"))
        b.tigeDao().inserer(tige("tl1", "local"))
        val reception = ReceptionMarsync(depot(b))

        assertTrue(reception.recevoir("x.marsync", marsyncDeA()))
        reception.annuler()

        assertNull(reception.enAttente)
        assertEquals(listOf("local"), b.contexteDao().toutes().map { it.id })
        assertEquals(listOf("tl1"), b.tigeDao().toutes().map { it.uuid })
        assertFalse(reception.confirmer(), "rien à confirmer après Annuler")
        assertEquals(listOf("local"), b.contexteDao().toutes().map { it.id })
    }

    @Test
    fun `un fichier etranger n'est pas mis en attente et ne change rien`() = runBlocking {
        val b = base()
        b.contexteDao().inserer(contexte("local", "Local"))
        val reception = ReceptionMarsync(depot(b))

        assertFalse(reception.recevoir("martelage.csv", "a;b\n1;2\n"))
        assertNull(reception.enAttente)
        assertFalse(reception.confirmer())
        assertEquals(listOf("local"), b.contexteDao().toutes().map { it.id })
    }

    @Test
    fun `confirmer deux fois ne refusionne pas`() = runBlocking {
        val b = base()
        val reception = ReceptionMarsync(depot(b))
        reception.recevoir("x.marsync", marsyncDeA())
        assertTrue(reception.confirmer())
        b.tigeDao().supprimerParContexte("p12")
        assertFalse(reception.confirmer(), "l'Intent déjà traité ne refusionne pas (rotation, retour dans l'appli)")
        assertTrue(b.tigeDao().listeParContexte("p12").isEmpty())
    }

    @Test
    fun `une nouvelle reception remplace l'attente precedente`() {
        val b = base()
        val reception = ReceptionMarsync(depot(b))
        reception.recevoir("un.marsync", """{"version":1,"contextes":[]}""")
        reception.recevoir("deux.marsync", """{"version":1,"tiges":[]}""")
        assertNotNull(reception.enAttente)
        assertEquals("deux.marsync", reception.enAttente?.nom)
    }

    // ---- Fusion : cas du terrain ----

    @Test
    fun `un contexte local absent du fichier survit a la fusion`() = runBlocking {
        val b = base()
        b.contexteDao().inserer(contexte("local", "Local"))
        b.tigeDao().inserer(tige("tl1", "local"))
        val reception = ReceptionMarsync(depot(b))

        reception.recevoir("x.marsync", marsyncDeA())
        reception.confirmer()

        assertEquals(setOf("local", "p12"), b.contexteDao().toutes().map { it.id }.toSet())
        assertEquals(listOf("tl1"), b.tigeDao().listeParContexte("local").map { it.uuid })
        assertEquals(3, b.tigeDao().listeParContexte("p12").size)
    }

    @Test
    fun `recevoir deux fois le meme fichier ne cree pas de doublon`() = runBlocking {
        val json = marsyncDeA()
        val b = base()
        val reception = ReceptionMarsync(depot(b))

        repeat(2) {
            assertTrue(reception.recevoir("x.marsync", json))
            assertTrue(reception.confirmer())
        }

        assertEquals(1, b.contexteDao().toutes().size)
        assertEquals(setOf("t1", "t2", "t3"), b.tigeDao().toutes().map { it.uuid }.toSet())
        assertEquals(3, b.tigeDao().toutes().size)
    }
}
