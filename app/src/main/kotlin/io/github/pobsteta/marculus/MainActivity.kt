package io.github.pobsteta.marculus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.data.FichierMarsync
import io.github.pobsteta.marculus.data.ReceptionMarsync
import io.github.pobsteta.marculus.ui.AppRoot
import io.github.pobsteta.marculus.ui.ToucheVolume
import io.github.pobsteta.marculus.ui.theme.MarculusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Applique la langue choisie avant la création des vues (locale du contexte de base).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Langue.appliquer(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as MarculusApplication).repository
        val reglagesRepo = (application as MarculusApplication).reglages
        val referentielsRepo = (application as MarculusApplication).referentiels
        val sauvegardeRepo = (application as MarculusApplication).sauvegarde
        val gpkgRepo = (application as MarculusApplication).gpkg
        val lotRepo = (application as MarculusApplication).lot
        val tarifsIfnRepo = (application as MarculusApplication).tarifsIfn
        val reception = (application as MarculusApplication).receptionMarsync
        // Un Intent déjà traité (recréation de l'activité) ne se rejoue pas.
        if (savedInstanceState == null) recevoirMarsync(intent)
        setContent {
            val reglages by reglagesRepo.reglages.collectAsStateWithLifecycle(Reglages())

            // Anti-veille : garder l'écran allumé.
            LaunchedEffect(reglages.antiVeille) {
                if (reglages.antiVeille) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            // Plein écran : masquer les barres système (réapparaissent au glissement).
            LaunchedEffect(reglages.pleinEcran) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (reglages.pleinEcran) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MarculusTheme(darkTheme = reglages.theme.estSombre(isSystemInDarkTheme())) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(repository, reglagesRepo, reglages, referentielsRepo, sauvegardeRepo, gpkgRepo, lotRepo, tarifsIfnRepo)
                    proposition?.let { p ->
                        val resume = p.resume
                        val titre = when {
                            resume != null && resume.noms.size == 1 && resume.nbContextes == 1 ->
                                stringResource(R.string.marsync_confirmer_titre, resume.noms[0])
                            resume != null && resume.nbContextes > 1 ->
                                stringResource(R.string.marsync_confirmer_titre_contextes, resume.nbContextes)
                            else -> stringResource(R.string.marsync_confirmer_titre_fichier)
                        }
                        val detail = resume?.let {
                            val tiges = pluralStringResource(R.plurals.marsync_tiges, it.nbTiges, it.nbTiges)
                            if (it.operateurs.isEmpty()) {
                                tiges
                            } else {
                                tiges + " · " + stringResource(R.string.marsync_de_operateur, it.operateurs.joinToString(", "))
                            }
                        }
                        AlertDialog(
                            onDismissRequest = { reception.annuler(); proposition = null },
                            title = { Text(titre) },
                            text = {
                                Column {
                                    if (detail != null) Text(detail, style = MaterialTheme.typography.titleSmall)
                                    if (resume != null && resume.nbContextes > 1 && resume.noms.isNotEmpty()) {
                                        Text(resume.noms.joinToString(", "))
                                    }
                                    Text(stringResource(R.string.marsync_confirmer_texte))
                                    p.nom?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    proposition = null
                                    lifecycleScope.launch {
                                        val ok = reception.confirmer()
                                        toast(if (ok) R.string.sync_msg_fusion else R.string.param_msg_illisible)
                                    }
                                }) { Text(stringResource(R.string.marsync_fusionner)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { reception.annuler(); proposition = null }) {
                                    Text(stringResource(R.string.liste_dialog_annuler))
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /** Proposition de fusion affichée dans la boîte de confirmation. */
    private var proposition by mutableStateOf<ReceptionMarsync.Proposition?>(null)

    // L'appli est déjà ouverte : « Ouvrir avec » arrive ici.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recevoirMarsync(intent)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()

    /** Lit le fichier de l'Intent (VIEW ou SEND), le met en attente de confirmation, et consomme l'Intent. */
    private fun recevoirMarsync(intent: Intent?) {
        // Relance depuis les applis récentes : l'Intent d'origine est rejoué, on l'ignore.
        if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        val uri = uriDe(intent) ?: return
        // Seul content:// est accepté : un file:// serait lu avec les droits de Marculus (fichiers privés compris).
        if (uri.scheme != "content") return
        // Consommé : ni rotation ni retour dans l'appli ne le rejouent.
        intent?.data = null
        intent?.removeExtra(Intent.EXTRA_STREAM)
        intent?.action = null
        val reception = (application as MarculusApplication).receptionMarsync
        lifecycleScope.launch {
            val (nom, texte) = withContext(Dispatchers.IO) {
                var taille: Long? = null
                val n = runCatching {
                    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
                        if (it.moveToFirst()) {
                            if (!it.isNull(1)) taille = it.getLong(1)
                            it.getString(0)
                        } else null
                    }
                }.getOrNull() ?: uri.lastPathSegment
                // Refus avant lecture au-delà du plafond ; la lecture elle-même est bornée.
                val t = if (!FichierMarsync.taillePermise(taille)) null else runCatching {
                    contentResolver.openInputStream(uri)?.use { FichierMarsync.lireBorne(it) }
                }.getOrNull()
                n to t
            }
            if (texte != null && reception.recevoir(nom, texte)) {
                proposition = reception.enAttente
            } else {
                toast(R.string.param_msg_illisible)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun uriDe(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        else -> null
    }

    /** Une session push-to-talk est ouverte par l'appui long en cours sur le volume bas. */
    private var pttEnCours = false

    // Comptage par boutons de volume : la feuille enregistre le relais quand le réglage est actif.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Dictée vocale : le volume bas devient court/long, donc l'action attend le relâchement.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && ToucheVolume.onPtt != null) {
            if (event != null && event.repeatCount == 0) event.startTracking() // arme onKeyLongPress
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val consomme = ToucheVolume.onVolume?.invoke(keyCode == KeyEvent.KEYCODE_VOLUME_UP) ?: false
            if (consomme) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Appui long sur le volume bas ≈ 0,5 s : ouverture du micro tant que la touche est tenue.
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        val ptt = ToucheVolume.onPtt
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && ptt != null) {
            pttEnCours = true
            ptt(true)
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    // Consomme aussi le relâchement pour éviter l'OSD de volume du système.
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val ptt = ToucheVolume.onPtt
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && ptt != null) {
            if (pttEnCours) {
                pttEnCours = false
                ptt(false)
            } else {
                // Appui court : le comptage se comporte comme avant, au relâchement près.
                ToucheVolume.onVolume?.invoke(false)
            }
            return true
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            ToucheVolume.onVolume != null
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
