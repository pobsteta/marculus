package io.github.pobsteta.marculus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MarculusDatabase
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResumeMarsyncTest {
    private var n = 0
    private fun tige(op: String? = null, action: String = "PLUS", quantite: Int? = null) =
        """{"uuid":"u${n++}","action":"$action"${quantite?.let { ",\"quantite\":$it" } ?: ""}${op?.let { ",\"operateur\":\"$it\"" } ?: ""}}"""

    @Test
    fun `un contexte nomme avec operateur et trois tiges`() {
        val json = """{"version":1,"contextes":[{"id":"a","nom":"Affouage Les Hauts","operateur":"Pascal"}],"tiges":[${tige("Pascal")},${tige()},${tige()}]}"""
        val r = FichierMarsync.resume(json)!!
        assertEquals(1, r.nbContextes)
        assertEquals(listOf("Affouage Les Hauts"), r.noms)
        assertEquals(3, r.nbTiges)
        assertEquals(listOf("Pascal"), r.operateurs)
    }

    @Test
    fun `plusieurs contextes gardent l'ordre des noms et dedoublonnent les operateurs`() {
        val json = """{"contextes":[{"nom":"A","operateur":"Zoe"},{"nom":"B","operateur":"Zoe"},{"nom":"C"}],"tiges":[${tige("Luc")}]}"""
        val r = FichierMarsync.resume(json)!!
        assertEquals(3, r.nbContextes)
        assertEquals(listOf("A", "B", "C"), r.noms)
        assertEquals(listOf("Zoe", "Luc"), r.operateurs)
    }

    @Test
    fun `contexte sans nom ou nom vide`() {
        val r = FichierMarsync.resume("""{"contextes":[{"id":"a"},{"id":"b","nom":"  "}],"tiges":[]}""")!!
        assertEquals(2, r.nbContextes)
        assertEquals(emptyList<String>(), r.noms)
        assertEquals(0, r.nbTiges)
        assertEquals(emptyList<String>(), r.operateurs)
    }

    @Test
    fun `tiges seules sans contexte`() {
        val r = FichierMarsync.resume("""{"tiges":[${tige()},${tige()}]}""")!!
        assertEquals(0, r.nbContextes)
        assertEquals(2, r.nbTiges)
    }

    // Modifié par le dev (ancien test : « une annulation compte comme une tige ») : le nombre est désormais le net.
    @Test
    fun `une annulation retire une tige du net`() {
        val r = FichierMarsync.resume("""{"tiges":[${tige()},${tige()},${tige(action = "ANNULATION")}]}""")!!
        assertEquals(1, r.nbTiges)
    }

    @Test
    fun `la quantite compte`() {
        val r = FichierMarsync.resume("""{"tiges":[${tige(quantite = 5)},${tige(action = "ANNULATION", quantite = 2)}]}""")!!
        assertEquals(3, r.nbTiges)
    }

    @Test
    fun `plusieurs contextes donnent le net total`() {
        val t = { c: String, a: String, q: Int ->
            """{"uuid":"u${n++}","contexteId":"$c","action":"$a","quantite":$q}"""
        }
        val json = """{"contextes":[{"id":"a"},{"id":"b"}],"tiges":[${t("a", "PLUS", 4)},${t("b", "PLUS", 3)},${t("b", "ANNULATION", 1)}]}"""
        assertEquals(6, FichierMarsync.resume(json)!!.nbTiges)
    }

    @Test
    fun `un uuid repete ne compte qu'une fois comme a la fusion`() {
        val t = """{"uuid":"x","action":"PLUS","quantite":2}"""
        assertEquals(2, FichierMarsync.resume("""{"tiges":[$t,$t]}""")!!.nbTiges)
    }

    @Test
    fun `contenu non reconnu donne null`() {
        assertNull(FichierMarsync.resume("pas du json"))
        assertNull(FichierMarsync.resume("""{"autre":1}"""))
    }

    @Test
    fun `la proposition expose le resume de son contenu`() {
        val p = ReceptionMarsync.Proposition("x.marsync", """{"contextes":[{"nom":"A"}],"tiges":[${tige()}]}""")
        assertEquals(1, p.resume!!.nbTiges)
    }

    @Test
    fun `le meme fichier donne le meme nombre que la liste des contextes apres fusion`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        fun base() = Room.inMemoryDatabaseBuilder(ctx, MarculusDatabase::class.java).allowMainThreadQueries().build()
        fun depot(db: MarculusDatabase) =
            SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), db.mergeDao(), ReferentielsRepository(ctx))
        fun contexte(id: String) = ContexteEntity(
            id = id, nom = id, mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
            essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
            operateur = "A", modifie = 1,
        )
        fun tige(uuid: String, c: String, action: String, q: Int) = TigeEntity(
            uuid = uuid, contexteId = c, essence = "CHE", classe = 40, action = action, horodatage = 1,
            quantite = q, hauteurTexte = null, qualiteArbre = null, latitude = null, longitude = null,
            operateur = "A", modifie = 1,
        )
        val a = base(); val b = base()
        try {
            listOf("c1", "c2").forEach { a.contexteDao().inserer(contexte(it)) }
            a.tigeDao().inserer(tige("t1", "c1", "PLUS", 5))
            a.tigeDao().inserer(tige("t2", "c1", "ANNULATION", 2))
            a.tigeDao().inserer(tige("t3", "c2", "PLUS", 1))
            a.tigeDao().inserer(tige("t4", "c2", "PLUS", 3))
            val json = depot(a).exporterJson()
            val annonce = FichierMarsync.resume(json)!!.nbTiges
            depot(b).fusionnerJson(json)
            val liste = listOf("c1", "c2").sumOf { c ->
                b.tigeDao().listeParContexte(c).sumOf { if (it.action == "PLUS") it.quantite else -it.quantite }
            }
            assertEquals(7, liste)
            assertEquals(liste, annonce)
        } finally { a.close(); b.close() }
    }
}
