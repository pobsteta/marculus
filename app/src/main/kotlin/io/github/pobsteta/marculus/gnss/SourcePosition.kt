package io.github.pobsteta.marculus.gnss

import fr.marculus.core.model.FixGnss
import kotlinx.coroutines.flow.Flow

/**
 * Source de position **unifiée** : émet des [FixGnss] quelle que soit l'origine (GNSS interne du
 * téléphone ou récepteur RTK externe). L'UI s'y abonne sans connaître la source.
 */
interface SourcePosition {
    /** Flux des fix ; émet `null` tant qu'aucun fix n'est disponible. Se referme à l'annulation. */
    fun fixs(): Flow<FixGnss?>
}

/**
 * Lien octets bidirectionnel vers un récepteur GNSS externe. La **lecture** est un flux d'octets ;
 * l'**écriture** (renvoi des corrections RTCM, étape G2) est suspendue. Implémentations : Bluetooth
 * SPP (G1), USB / TCP ultérieurement.
 */
interface Transport {
    /** Flux des octets reçus ; se termine à la fermeture du lien ou sur erreur d'E/S. */
    fun lire(): Flow<ByteArray>

    /** Envoie des octets au récepteur (corrections RTCM). No-op tant que G2 n'est pas implémenté. */
    suspend fun ecrire(donnees: ByteArray) {}
}
