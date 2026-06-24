package fr.marculus.core

import fr.marculus.core.model.Position
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NtripTest {

    private fun proche(attendu: Double, obtenu: Double?, tol: Double = 1e-4) =
        assertTrue(obtenu != null && kotlin.math.abs(attendu - obtenu) <= tol, "attendu ~$attendu, obtenu $obtenu")

    @Test
    fun `requete GET avec ntrip-version et auth basic`() {
        val r = Ntrip.requete("CT", "caster.centipede.fr", 2101, "user", "pass")
        assertTrue(r.startsWith("GET /CT HTTP/1.1\r\n"))
        assertTrue(r.contains("Ntrip-Version: Ntrip/2.0\r\n"))
        assertTrue(r.endsWith("\r\n\r\n"))
        val b64 = r.lineSequence().first { it.startsWith("Authorization: Basic ") }
            .removePrefix("Authorization: Basic ").trim()
        assertEquals("user:pass", String(Base64.getDecoder().decode(b64)))
    }

    @Test
    fun `le mountpoint recoit un slash initial une seule fois`() {
        assertTrue(Ntrip.requete("/CT", "h", 2101, "u", "p").startsWith("GET /CT HTTP/1.1"))
    }

    @Test
    fun `gga est une trame valide et reparseable`() {
        val gga = Ntrip.gga(Position(48.1173, 11.516667), heureUtc = "123519.00")
        assertTrue(NmeaParser.checksumValide(gga))
        val t = NmeaParser.parseGga(gga)!!
        proche(48.1173, t.position.latitude)
        proche(11.516667, t.position.longitude)
    }

    @Test
    fun `gga gere les hemispheres sud et ouest`() {
        val t = NmeaParser.parseGga(Ntrip.gga(Position(-48.1173, -11.516667)))!!
        proche(-48.1173, t.position.latitude)
        proche(-11.516667, t.position.longitude)
    }

    @Test
    fun `sourcetable extrait les points de montage STR`() {
        val st = """
            SOURCETABLE 200 OK
            CAS;caster.centipede.fr;2101;Centipede;
            STR;CT;Centipede;RTCM 3.2;1004(1),1012(1);2;GPS+GLO;SNIP;FRA;
            STR;LIENSS;La Rochelle;RTCM 3.3;;2;GPS+GLO+GAL;Centipede;FRA;
            NET;Centipede;centipede;B;N;
            ENDSOURCETABLE
        """.trimIndent()
        val e = Ntrip.parseSourcetable(st)
        assertEquals(2, e.size)
        assertEquals("CT", e[0].mountpoint)
        assertEquals("Centipede", e[0].identifiant)
        assertEquals("RTCM 3.2", e[0].format)
        assertEquals("LIENSS", e[1].mountpoint)
    }

    @Test
    fun `statut de reponse selon la premiere ligne`() {
        assertEquals(StatutNtrip.OK, Ntrip.statutReponse("ICY 200 OK"))
        assertEquals(StatutNtrip.OK, Ntrip.statutReponse("HTTP/1.1 200 OK"))
        assertEquals(StatutNtrip.NON_AUTORISE, Ntrip.statutReponse("HTTP/1.1 401 Unauthorized"))
        assertEquals(StatutNtrip.SOURCETABLE, Ntrip.statutReponse("SOURCETABLE 200 OK"))
        assertEquals(StatutNtrip.INCONNU, Ntrip.statutReponse("n'importe quoi"))
    }
}
