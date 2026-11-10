package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.TotauxMartelage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Tige
import fr.nemeton.terrain.geo.Position
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Mission 025, côté `:data` : la liste des contextes (`ResumeContexte.nbTiges`, net en SQL, mission
 * 018) et la boîte de fusion d'un `.marsync` (`FichierMarsync.resume`, mission 016) donnent, pour
 * le même jeu de journaux que `fr.marculus.core.EgaliteDesTotauxTest` (recopié ici : un module de
 * test ne voit pas les tests de l'autre), le **même nombre net que Statut**, calculé à la main.
 * Aucun volume n'est affiché à ces deux endroits : seul le nombre est comparé.
 *
 * Non couvert ici : l'export `.marsync` des totaux de volume (`SauvegardeRepository`, l. 187) et
 * l'écran Statut lui-même (`:app`, non testable en JVM ici).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EgaliteDesTotauxDataTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db: MarculusDatabase =
        Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
    private val depot = MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao())

    @After
    fun fermer() = db.close()

    private fun plus(
        u: String, t: Long, e: String = "CHE", c: Int = 30, q: Int = 1, lat: Double? = null, lot: Int? = null,
    ) = Tige(u, "c1", e, c, ActionTige.PLUS, horodatage = t, quantite = q, position = lat?.let { Position(it, 5.0) }, lot = lot)

    private fun moins(u: String, t: Long, e: String = "CHE", c: Int = 30, q: Int = 1) =
        Tige(u, "c1", e, c, ActionTige.ANNULATION, horodatage = t, quantite = q)

    /** (nom, journal, nombre net calculé à la main — mêmes commentaires que dans le test de `:core`). */
    private val cas: List<Triple<String, List<Tige>, Int>> = listOf(
        Triple("vide", emptyList(), 0),
        Triple("tout annulé", listOf(plus("a", 1, lat = 45.0, lot = 1), moins("x", 2)), 0),
        // a(1) c(2) b(3, q2) x(4) retire 1 de b : 1+1+1.
        Triple(
            "désordre",
            listOf(plus("b", 3, q = 2, lat = 46.0, lot = 2), moins("x", 4), plus("c", 2, lot = 1), plus("a", 1, lat = 45.0, lot = 1)),
            3,
        ),
        // a(5) b(3) ; x(4) ; y(2) : reste a(2).
        Triple(
            "paquets partiellement annulés",
            listOf(
                plus("a", 1, "HET", 40, 5, 45.0, 1), plus("b", 2, "HET", 40, 3, 46.0, 1),
                moins("x", 3, "HET", 40, 4), moins("y", 4, "HET", 40, 2),
            ),
            2,
        ),
        // a(2) x(5) : 3 orphelins ; y(1) orphelin ; z(3) : 3 - 4 = -1.
        Triple(
            "annulations orphelines",
            listOf(
                plus("a", 1, q = 2, lat = 45.0, lot = 1), moins("x", 2, q = 5), moins("y", 3, "HET", 40),
                plus("z", 4, "CHE", 45, 3, 46.0, 1),
            ),
            -1,
        ),
        // 1 + 2 + 1 + 2.
        Triple(
            "plusieurs cases et lots",
            listOf(
                plus("a", 1, "CHE", 30, 2, 45.0, 1), plus("b", 2, "HET", 30, 3, 46.0, 1),
                plus("c", 3, "CHE", 50, 1, 45.0, 2), plus("d", 4, "HET", 50, 4, 10.0, 2),
                moins("w", 5, "CHE", 30), moins("n", 6, "HET", 50, 2),
                plus("e", 7, "HET", 30, 1, null, 3), moins("m", 8, "HET", 30, 2),
            ),
            6,
        ),
        // a(1) + b(1) + c(3).
        Triple(
            "tiges sans position",
            listOf(
                plus("a", 1, q = 2, lot = 1), plus("b", 2, "HET", 40, 1, null, null),
                plus("c", 3, "HET", 40, 3, 10.0, 2), moins("x", 4),
            ),
            5,
        ),
        // Même horodatage : uuid a, m, z : m retire a, reste z.
        Triple("égalité d'horodatage", listOf(plus("z", 1, lat = 45.0, lot = 1), moins("m", 1), plus("a", 1, lat = 46.0, lot = 2)), 1),
    )

    private fun contexte(id: String) = ContexteEntity(
        id = id, nom = "Contexte $id", mode = "DIAMETRE", classeMin = 20, classeMax = 80, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
        operateur = "A", modifie = 1,
    )

    private fun entite(t: Tige, contexteId: String) = TigeEntity(
        uuid = "$contexteId-${t.uuid}", contexteId = contexteId, essence = t.essence, classe = t.classe,
        action = t.action.name, horodatage = t.horodatage, quantite = t.quantite, hauteurTexte = null,
        qualiteArbre = null, latitude = t.position?.latitude, longitude = t.position?.longitude,
        operateur = "A", modifie = 1, lot = t.lot,
    )

    private fun json(tiges: List<Tige>, contexteId: String = "c1") = tiges.joinToString(",") {
        """{"uuid":"$contexteId-${it.uuid}","contexteId":"$contexteId","essence":"${it.essence}","classe":${it.classe},""" +
            """"action":"${it.action.name}","horodatage":${it.horodatage},"quantite":${it.quantite}}"""
    }

    @Test
    fun `liste des contextes - nbTiges egale le net calcule a la main, contexte par contexte`() = runBlocking {
        cas.forEachIndexed { i, (_, j, _) ->
            db.contexteDao().inserer(contexte("k$i"))
            j.forEach { db.tigeDao().inserer(entite(it, "k$i")) }
        }
        val resumes = depot.resumes().first().associateBy { it.contexte.id }
        cas.forEachIndexed { i, (nom, j, net) ->
            val r = resumes.getValue("k$i")
            assertEquals(net, r.nbTiges, "$nom : nbTiges")
            assertEquals(net, TotauxMartelage(j).totalContexte(), "$nom : Statut")
            assertEquals(j.size, r.nbEvenements, "$nom : nbEvenements compte les lignes du journal")
        }
    }

    @Test
    fun `boite de fusion - le nombre de tiges egale le net calcule a la main`() {
        cas.forEach { (nom, j, net) ->
            val contenu = """{"version":1,"contextes":[{"id":"c1","nom":"Essai"}],"tiges":[${json(j)}]}"""
            assertEquals(net, FichierMarsync.resume(contenu)!!.nbTiges, nom)
        }
    }

    @Test
    fun `boite de fusion - plusieurs contextes donnent la somme des nets`() {
        val tous = cas.flatMapIndexed { i, (_, j, _) -> j.map { it to "k$i" } }
        val tiges = tous.joinToString(",") { (t, k) -> json(listOf(t), k) }
        assertEquals(cas.sumOf { it.third }, FichierMarsync.resume("""{"tiges":[$tiges]}""")!!.nbTiges)
    }
}
