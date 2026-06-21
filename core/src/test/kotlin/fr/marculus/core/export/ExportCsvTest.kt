package fr.marculus.core.export

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertTrue

class ExportCsvTest {

    private val contexte = Contexte(
        id = "c1",
        nom = "Parcelle 12",
        mode = ModeMesure.DIAMETRE,
        axe = AxeClasses(20, 30, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        increment = 1,
    )

    private fun tige(essence: String, classe: Int, action: ActionTige, t: Long) =
        Tige("u$t", "c1", essence, classe, action, horodatage = t, quantite = 1)

    @Test
    fun `le csv contient les sections totaux et journal`() {
        val journal = listOf(
            tige("Chêne", 20, ActionTige.PLUS, 1000L),
            tige("Chêne", 20, ActionTige.PLUS, 2000L),
            tige("Hêtre", 25, ActionTige.PLUS, 3000L),
        )
        val csv = ExportCsv.contexteCsv(contexte, journal)
        assertTrue(csv.contains("TOTAUX"))
        assertTrue(csv.contains("JOURNAL"))
        assertTrue(csv.contains("Chêne;20;2")) // total dérivé
        assertTrue(csv.contains("Parcelle 12"))
    }

    @Test
    fun `un champ contenant le separateur est entoure de guillemets`() {
        val ctx = contexte.copy(nom = "Bois; du Roi")
        val csv = ExportCsv.contexteCsv(ctx, emptyList())
        assertTrue(csv.contains("\"Bois; du Roi\""))
    }
}
