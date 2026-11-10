package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.model.PartsLot
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Mission 026, décision de Pascal du 2026-11-10 : un `.marsync` sans `partsLot` (fichier ancien,
 * ou Nemeton) fusionné sur un contexte **déjà présent** garde les cases locales, comme
 * `partHouppier`. Une clé présente l'emporte. Le fichier entrant est toujours plus récent, pour que
 * la fusion « dernière écriture gagne » le retienne.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarsyncPartsLotFusionTest {
    private lateinit var db: MarculusDatabase
    private lateinit var repo: SauvegardeRepository

    private val locales = mapOf("Chêne" to PartsLot(tige = false, houppier = true))

    @Before
    fun ouvrir() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
        repo = SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), db.mergeDao(), ReferentielsRepository(ctx))
        runBlocking {
            db.contexteDao().inserer(
                ContexteEntity(
                    id = "c1", nom = "Affouage", mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
                    essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
                    operateur = null, tarif = "SCHAEFFER_RAPIDE", tarifNumero = 8, coefficientForme = 0.5,
                    modifie = 1, affouage = true, volumeMaxLotM3 = 14.0, partsLot = encodePartsLot(locales),
                ),
            )
        }
    }

    @After
    fun fermer() = db.close()

    /** Exporte `c1`, retouche le contexte du fichier (nom et `modifie` plus récents), le refusionne. */
    private fun fusionner(retoucher: (JSONObject) -> Unit) = runBlocking {
        val json = JSONObject(repo.exporterContexteJson("c1"))
        val c = json.getJSONArray("contextes").getJSONObject(0)
        c.put("nom", "Affouage bis"); c.put("modifie", 2L)
        retoucher(c)
        repo.fusionnerJson(json.toString())
        db.contexteDao().parId("c1")!!.versDomaine()
    }

    @Test
    fun `fichier ancien sans cases sur un contexte connu - les cases locales sont gardees`() {
        val c = fusionner { it.remove("partsLot") }
        assertEquals("Affouage bis", c.nom) // la fusion a bien eu lieu
        assertEquals(locales, c.partsLot)
    }

    @Test
    fun `fichier avec cases - elles l'emportent`() {
        val autres = fusionner {
            it.put("partsLot", JSONObject().put("Chêne", JSONObject().put("tige", true).put("houppier", false)))
        }
        assertEquals(mapOf("Chêne" to PartsLot(tige = true, houppier = false)), autres.partsLot)
    }

    @Test
    fun `fichier avec cases vides - retour au defaut`() {
        val c = fusionner { it.put("partsLot", JSONObject()) }
        assertEquals(emptyMap(), c.partsLot)
    }

    @Test
    fun `booleen absent d'une essence - coche par defaut`() {
        val c = fusionner { it.put("partsLot", JSONObject().put("Chêne", JSONObject().put("tige", false))) }
        assertEquals(mapOf("Chêne" to PartsLot(tige = false, houppier = true)), c.partsLot)
    }
}
