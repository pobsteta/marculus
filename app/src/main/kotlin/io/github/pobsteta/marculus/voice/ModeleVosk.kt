package io.github.pobsteta.marculus.voice

import android.content.Context
import fr.nemeton.terrain.voix.InstallationModele
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cycle de vie du modèle acoustique Vosk `vosk-model-small-fr-0.22` (Apache 2.0, ~42 Mo).
 *
 * **Choix : téléchargement au premier usage plutôt qu'embarquement dans les assets.** L'APK
 * release pèse ~11 Mo ; embarquer le modèle le porterait à ~53 Mo, soit cinq fois plus lourd à
 * publier, à télécharger et à installer — pour une fonction optionnelle, désactivée par défaut.
 * Le modèle est donc tiré une seule fois depuis les Paramètres (Wi-Fi conseillé, avant la sortie
 * en forêt) et vit ensuite dans le stockage interne : la reconnaissance reste 100 % hors ligne.
 *
 * Téléchargement, décompression et témoin : [InstallationModele] (terrain-voix).
 */
object ModeleVosk {

    const val URL_MODELE = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"
    const val TAILLE_MO = 42

    private const val DOSSIER = "vosk-model-fr"

    private fun installation(context: Context) =
        InstallationModele(File(context.filesDir, DOSSIER), context.cacheDir, URL_MODELE)

    /** Dossier d'installation du modèle (stockage interne privé de l'application). */
    fun dossier(context: Context): File = installation(context).dossier

    /** Le modèle est présent et complet (le témoin n'est écrit qu'en fin de décompression). */
    fun estInstalle(context: Context): Boolean = installation(context).estInstalle

    /** Supprime le modèle installé (libère ~42 Mo). */
    fun supprimer(context: Context) = installation(context).supprimer()

    /**
     * Télécharge et décompresse le modèle. [progression] reçoit un ratio 0..1 (ou -1 tant que la
     * taille annoncée est inconnue). Renvoie un [Result] : l'appelant affiche l'erreur telle quelle.
     * Annulable — un téléchargement interrompu laisse le dossier incomplet, donc sans témoin.
     */
    suspend fun telecharger(context: Context, progression: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val actif = { coroutineContext.isActive }
            installation(context).telecharger(progression, continuer = actif)
        }
}
