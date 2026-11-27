package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.LectureVolumes
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 094 : le `.marsync` nomme ses volumes (`volumeBoisFortTotalM3`, `volumeAerienTotalM3`) et n'écrit
 * plus `volumeTotalM3` ; un ancien fichier se relit via [LectureVolumes] (ancien nom = bois fort total).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarsyncVolumesEmergeTest {
    private lateinit var db: MarculusDatabase
    private lateinit var repo: SauvegardeRepository

    @Before
    fun ouvrir() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
        repo = SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), db.mergeDao(), ReferentielsRepository(ctx))
    }

    @After
    fun fermer() = db.close()

    private fun contexte(tarif: String, numero: Int) = ContexteEntity(
        id = "c1", nom = "Volumes", mode = "CIRCONFERENCE", classeMin = 20, classeMax = 140, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
        operateur = null, tarif = tarif, tarifNumero = numero, coefficientForme = 0.5, modifie = 1,
    )

    private fun tige() = TigeEntity(
        uuid = "u1", contexteId = "c1", essence = "Hêtre", classe = 60, action = "PLUS", horodatage = 1000L,
        quantite = 1, hauteurTexte = "10", qualiteArbre = null, latitude = null, longitude = null, operateur = null,
    )

    private fun exporter(tarif: String, numero: Int): JSONObject = runBlocking {
        db.contexteDao().inserer(contexte(tarif, numero))
        db.tigeDao().inserer(tige())
        JSONObject(repo.exporterJson())
    }

    @Test
    fun `EMERGE - quatre volumes nommes, plus de volumeTotalM3`() {
        val json = exporter("EMERGE", 0)
        val c = json.getJSONArray("contextes").getJSONObject(0)
        val t = json.getJSONArray("tiges").getJSONObject(0)
        for (o in listOf(c, t)) {
            assertFalse(o.has("volumeTotalM3"), "l'ancien nom n'est plus écrit")
            assertTrue(o.getDouble("volumeAerienTotalM3") > o.getDouble("volumeBoisFortTotalM3"))
        }
        assertEquals(c.getDouble("volumeBoisFortTotalM3"), c.getDouble("volumeTigeTotalM3") + c.getDouble("volumeHouppierTotalM3"), 1e-9)
        assertEquals("EMERGE", t.getString("cubage"))
    }

    @Test
    fun `Schaeffer - pas de volume aerien total`() {
        val json = exporter("SCHAEFFER_RAPIDE", 8)
        assertFalse(json.getJSONArray("contextes").getJSONObject(0).has("volumeAerienTotalM3"))
        assertFalse(json.getJSONArray("tiges").getJSONObject(0).has("volumeAerienTotalM3"))
    }

    @Test
    fun `une ancienne sauvegarde se relit - volumeTotalM3 est le bois fort total`() {
        val ancien = JSONObject("""{"volumeTigeTotalM3":0.1557,"volumeTotalM3":0.1808,"volumeHouppierTotalM3":0.0251}""")
        val champs = ancien.keys().asSequence().associateWith { ancien.getString(it) }
        assertEquals(0.1808, LectureVolumes.boisFortTotal(champs))
    }
}
