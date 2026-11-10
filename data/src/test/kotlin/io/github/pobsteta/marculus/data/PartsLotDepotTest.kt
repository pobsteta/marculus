package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.PartsLot
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** Mission 027 : les cases tige / houppier écrites puis relues ; une édition sans cases garde les reçues. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PartsLotDepotTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
    private val depot = MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao())

    @After
    fun fermer() = db.close()

    private val essences = listOf(EssenceColonne("CHE", 0, 0))
    private val axe = AxeClasses(20, 90, 5)
    private suspend fun relire(id: String) = db.contexteDao().parId(id)!!.partsLot

    @Test
    fun creer_ecrit_les_cases_puis_les_relit() = runBlocking {
        val parts = mapOf("CHE" to PartsLot(tige = false, houppier = true))
        val id = depot.creerContexte("A", ModeMesure.DIAMETRE, axe, essences, partsLot = parts)
        assertEquals(parts, decodePartsLot(relire(id)))
    }

    @Test
    fun creer_sans_cases_donne_aucune_entree() = runBlocking {
        val id = depot.creerContexte("A", ModeMesure.DIAMETRE, axe, essences)
        assertEquals(emptyMap(), decodePartsLot(relire(id)))
    }

    @Test
    fun modifier_avec_cases_remplace() = runBlocking {
        val id = depot.creerContexte("A", ModeMesure.DIAMETRE, axe, essences,
            partsLot = mapOf("CHE" to PartsLot(false, true)))
        val neuves = mapOf("CHE" to PartsLot(false, false))
        depot.modifierContexte(id, "A", ModeMesure.DIAMETRE, axe, essences, null, 1, partsLot = neuves)
        assertEquals(neuves, decodePartsLot(relire(id)))
    }

    @Test
    fun modifier_sans_cases_garde_celles_recues() = runBlocking {
        val recues = mapOf("CHE" to PartsLot(true, false))
        val id = depot.creerContexte("A", ModeMesure.DIAMETRE, axe, essences, partsLot = recues)
        depot.modifierContexte(id, "B", ModeMesure.DIAMETRE, axe, essences, null, 1)
        assertEquals(recues, decodePartsLot(relire(id)))
    }
}
