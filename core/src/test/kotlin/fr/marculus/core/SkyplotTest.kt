package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkyplotTest {

    private val gpsv1 = "\$GPGSV,2,1,07,01,40,083,46,02,17,308,41,12,07,344,39,14,22,228,45*7A"
    private val gpsv2 = "\$GPGSV,2,2,07,15,45,050,44,18,12,120,30,24,60,170,48*4E"
    private val glsv = "\$GLGSV,1,1,02,65,30,045,40,66,15,090,35*69"

    @Test
    fun `parseGsv decode les satellites d'un message`() {
        val t = NmeaParser.parseGsv(gpsv1)!!
        assertEquals("GP", t.systeme)
        assertEquals(2, t.nbMessages)
        assertEquals(1, t.numMessage)
        assertEquals(7, t.satellitesEnVue)
        assertEquals(4, t.satellites.size)
        val s = t.satellites.first()
        assertEquals(1, s.prn)
        assertEquals(40, s.elevation)
        assertEquals(83, s.azimut)
        assertEquals(46, s.snr)
    }

    @Test
    fun `une trame non GSV n'est pas decodee`() {
        assertNull(NmeaParser.parseGsv("\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"))
    }

    @Test
    fun `accumulateur rassemble une sequence complete et plusieurs constellations`() {
        val acc = AccumulateurSkyplot()
        acc.pousser(NmeaParser.parseGsv(gpsv1)!!)
        acc.pousser(NmeaParser.parseGsv(gpsv2)!!)
        acc.pousser(NmeaParser.parseGsv(glsv)!!)
        val sats = acc.satellites()
        assertEquals(9, sats.size) // 4 + 3 (GPS) + 2 (GLONASS)
        assertTrue(sats.any { it.prn == 65 && it.systeme == "GL" })
    }

    @Test
    fun `une nouvelle sequence remplace la precedente de la meme constellation`() {
        val acc = AccumulateurSkyplot()
        acc.pousser(NmeaParser.parseGsv(gpsv1)!!)
        acc.pousser(NmeaParser.parseGsv(gpsv2)!!) // 7 sats GPS complets
        // Nouvelle séquence GPS d'1 seul message avec 1 satellite → remplace les 7.
        acc.pousser(NmeaParser.parseGsv("\$GPGSV,1,1,01,30,10,200,33*48")!!)
        assertEquals(1, acc.satellites().count { it.systeme == "GP" })
    }
}
