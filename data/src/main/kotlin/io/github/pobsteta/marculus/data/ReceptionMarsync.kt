package io.github.pobsteta.marculus.data

/**
 * Fichier `.marsync` reçu, en attente de confirmation. [recevoir] ne touche pas la base ;
 * [confirmer] fusionne par [SauvegardeRepository.fusionnerJson] (seule écriture) ; [annuler] et
 * [confirmer] vident l'attente : un Intent n'est consommé qu'une fois.
 */
class ReceptionMarsync(private val sauvegarde: SauvegardeRepository) {
    data class Proposition(val nom: String?, val contenu: String) {
        /** Ce que contient le fichier, lu dans [contenu]. */
        val resume: FichierMarsync.Resume? = FichierMarsync.resume(contenu)
    }

    @Volatile
    var enAttente: Proposition? = null
        private set

    /** Met en attente si le contenu est reconnu (remplace l'attente précédente) ; sinon ne change rien. */
    fun recevoir(nom: String?, contenu: String): Boolean {
        if (!FichierMarsync.estMarsync(nom, contenu)) return false
        enAttente = Proposition(nom, contenu)
        return true
    }

    /** Fusionne la proposition en attente ; false s'il n'y en a pas ou si la fusion échoue. */
    suspend fun confirmer(): Boolean {
        val p = enAttente ?: return false
        enAttente = null
        return runCatching { sauvegarde.fusionnerJson(p.contenu) }.isSuccess
    }

    fun annuler() {
        enAttente = null
    }
}
