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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 026, règle 4 : les cases « tige » / « houppier » voyagent dans le `.marsync`, **au
 * contexte** (comme `partHouppier`). Forme proposée par le testeur, à valider par Pascal :
 * `"partsLot": {"Chêne": {"tige": false, "houppier": true}}` ; entité `ContexteEntity.partsLot:
 * String` (défaut `""`), `encodePartsLot(Map<String, PartsLot>)` / `decodePartsLot(String)`
 * (miroirs de `encodePartsHouppier` / `decodePartsHouppier`, `Conversions.kt`).
 * Une clé absente (fichier d'avant la mission 026) donne « les deux cochées » pour un contexte
 * inconnu de l'appareil.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarsyncPartsLotTest {
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

    private fun contexte(parts: Map<String, PartsLot>) = ContexteEntity(
        id = "c1", nom = "Affouage", mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
        operateur = null, tarif = "SCHAEFFER_RAPIDE", tarifNumero = 8, coefficientForme = 0.5,
        modifie = 1, affouage = true, volumeMaxLotM3 = 14.0, partsLot = encodePartsLot(parts),
    )

    /** Exporte `c1`, l'efface de la base, puis fusionne le JSON (éventuellement retouché). */
    private fun allerRetour(parts: Map<String, PartsLot>, retoucher: (JSONObject) -> Unit = {}): JSONObject = runBlocking {
        db.contexteDao().inserer(contexte(parts))
        val json = JSONObject(repo.exporterContexteJson("c1"))
        retoucher(json.getJSONArray("contextes").getJSONObject(0))
        db.contexteDao().supprimer("c1")
        repo.fusionnerJson(json.toString())
        json
    }

    private fun relu() = runBlocking { db.contexteDao().parId("c1")!!.versDomaine() }

    @Test
    fun `le marsync porte les cases au contexte et l'aller-retour les rend`() {
        val parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true), "Hêtre" to PartsLot(tige = true, houppier = false))
        val json = allerRetour(parts)
        val o = json.getJSONArray("contextes").getJSONObject(0).getJSONObject("partsLot")
        assertFalse(o.getJSONObject("Chêne").getBoolean("tige"))
        assertTrue(o.getJSONObject("Chêne").getBoolean("houppier"))
        assertTrue(o.getJSONObject("Hêtre").getBoolean("tige"))
        assertFalse(o.getJSONObject("Hêtre").getBoolean("houppier"))
        assertEquals(parts, relu().partsLot)
    }

    @Test
    fun `les deux decochees voyagent et ne sont pas confondues avec le defaut`() {
        val parts = mapOf("Chêne" to PartsLot(tige = false, houppier = false))
        allerRetour(parts)
        assertEquals(PartsLot(tige = false, houppier = false), relu().partsLot["Chêne"])
    }

    @Test
    fun `fichier ancien sans cases - contexte inconnu, les deux cochees`() {
        allerRetour(mapOf("Chêne" to PartsLot(tige = false, houppier = false))) { it.remove("partsLot") }
        val c = relu()
        assertEquals(PartsLot(tige = true, houppier = true), c.partsLot["Chêne"] ?: PartsLot())
        assertTrue(c.partsLot.values.all { it.tige && it.houppier })
        assertEquals(14.0, c.volumeMaxLotM3) // le reste du contexte est importé
    }

    @Test
    fun `encodage aller-retour`() {
        val parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true), "Sapin pectiné" to PartsLot(tige = false, houppier = false))
        assertEquals(parts, decodePartsLot(encodePartsLot(parts)))
        assertEquals(emptyMap(), decodePartsLot(""))
    }
}
