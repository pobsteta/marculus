package fr.marculus.core

import fr.marculus.core.model.QualiteFix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NmeaParserTest {

    // Vecteurs réels, sommes de contrôle calculées (XOR entre `$` et `*`).
    private val ggaAutonome = "\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"
    private val ggaRtkFixe = "\$GNGGA,141322.00,4717.112671,N,00833.914843,E,4,12,0.65,123.456,M,47.5,M,1.0,0000*62"
    private val ggaRtkFloat = "\$GNGGA,141322.00,4717.112671,N,00833.914843,E,5,11,0.80,123.000,M,47.5,M,2.5,0000*6A"
    private val ggaSudOuest = "\$GPGGA,123519,4807.038,S,01131.000,W,1,08,0.9,545.4,M,46.9,M,,*48"
    private val ggaSansFix = "\$GNGGA,235947.00,,,,,0,00,99.99,,M,,M,,*76"
    private val gst = "\$GPGST,172814.0,0.006,0.023,0.020,273.6,0.023,0.020,0.031*6A"
    private val gsv = "\$GPGSV,3,1,11,03,03,111,00,04,15,270,00*7F"
    private val gsa = "\$GNGSA,A,3,01,02,03,04,05,06,07,08,09,10,11,12,1.21,0.65,1.02*1D"

    private fun proche(attendu: Double, obtenu: Double?, tol: Double = 1e-5) =
        assertTrue(obtenu != null && kotlin.math.abs(attendu - obtenu) <= tol, "attendu ~$attendu, obtenu $obtenu")

    @Test
    fun `checksum valide reconnu`() {
        assertTrue(NmeaParser.checksumValide(ggaAutonome))
        assertTrue(NmeaParser.checksumValide(gst))
    }

    @Test
    fun `checksum invalide rejete`() {
        assertFalse(NmeaParser.checksumValide("\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*00"))
        assertFalse(NmeaParser.checksumValide("sans dollar ni etoile"))
        assertNull(NmeaParser.parseGga("\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*00"))
    }

    @Test
    fun `GGA autonome decode position qualite satellites et altitude`() {
        val t = NmeaParser.parseGga(ggaAutonome)!!
        proche(48.1173, t.position.latitude)
        proche(11.516667, t.position.longitude)
        assertEquals(QualiteFix.AUTONOME, t.qualite)
        assertFalse(t.qualite.estRtk)
        assertEquals(8, t.nbSatellites)
        proche(0.9, t.hdop!!, 1e-6)
        proche(545.4, t.altitudeM!!, 1e-6)
        assertNull(t.ageCorrectionsS)
    }

    @Test
    fun `GGA RTK fixe avec age de corrections`() {
        val t = NmeaParser.parseGga(ggaRtkFixe)!!
        assertEquals(QualiteFix.RTK_FIXE, t.qualite)
        assertTrue(t.qualite.estRtk)
        assertEquals(12, t.nbSatellites)
        proche(47.285211, t.position.latitude)
        proche(8.565247, t.position.longitude)
        proche(1.0, t.ageCorrectionsS!!, 1e-6)
    }

    @Test
    fun `GGA RTK flottant`() {
        assertEquals(QualiteFix.RTK_FLOAT, NmeaParser.parseGga(ggaRtkFloat)!!.qualite)
    }

    @Test
    fun `hemispheres sud et ouest donnent des degres negatifs`() {
        val t = NmeaParser.parseGga(ggaSudOuest)!!
        proche(-48.1173, t.position.latitude)
        proche(-11.516667, t.position.longitude)
    }

    @Test
    fun `GGA sans position renvoie null`() {
        assertNull(NmeaParser.parseGga(ggaSansFix))
    }

    @Test
    fun `une trame non GGA n'est pas decodee comme GGA`() {
        assertNull(NmeaParser.parseGga(gsv))
        assertNull(NmeaParser.parseGga(gst))
    }

    @Test
    fun `GST decode les ecarts-types et la precision horizontale`() {
        val t = NmeaParser.parseGst(gst)!!
        proche(0.023, t.ecartTypeLatM, 1e-6)
        proche(0.020, t.ecartTypeLonM, 1e-6)
        proche(0.030480, t.precisionHorizontaleM, 1e-5)
    }

    @Test
    fun `une trame non GST n'est pas decodee comme GST`() {
        assertNull(NmeaParser.parseGst(ggaAutonome))
    }

    @Test
    fun `fixDepuis combine GGA et GST`() {
        val gga = NmeaParser.parseGga(ggaRtkFixe)!!
        val precision = NmeaParser.parseGst(gst)!!
        val fix = NmeaParser.fixDepuis(gga, precision)
        assertEquals(QualiteFix.RTK_FIXE, fix.qualite)
        assertEquals(12, fix.nbSatellites)
        proche(0.030480, fix.precisionHorizontaleM!!, 1e-5)
    }

    @Test
    fun `fixDepuis sans GST laisse la precision nulle`() {
        assertNull(NmeaParser.fixDepuis(NmeaParser.parseGga(ggaAutonome)!!).precisionHorizontaleM)
    }

    @Test
    fun `GGA RTK fixe expose la station de reference`() {
        assertEquals("0000", NmeaParser.parseGga(ggaRtkFixe)!!.stationRef)
        assertNull(NmeaParser.parseGga(ggaAutonome)!!.stationRef)
    }

    @Test
    fun `GSA decode PDOP HDOP VDOP`() {
        val t = NmeaParser.parseGsa(gsa)!!
        proche(1.21, t.pdop!!, 1e-6)
        proche(0.65, t.hdop!!, 1e-6)
        proche(1.02, t.vdop!!, 1e-6)
        assertNull(NmeaParser.parseGsa(ggaAutonome))
    }

    @Test
    fun `fixDepuis integre la GSA (PDOP VDOP)`() {
        val fix = NmeaParser.fixDepuis(
            NmeaParser.parseGga(ggaRtkFixe)!!,
            NmeaParser.parseGst(gst)!!,
            NmeaParser.parseGsa(gsa)!!,
        )
        proche(1.21, fix.pdop!!, 1e-6)
        proche(1.02, fix.vdop!!, 1e-6)
        assertEquals("0000", fix.stationRef)
    }
}
