package rigatoni534.apps.PastaPlayer.presentation.discord.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.ui.theme.PastaPlayTheme

/**
 * Hosts Discord's login page in a real Activity instead of a dialog window.
 *
 * Dialog windows do not give the WebView a hardware-accelerated surface reliably, which is
 * why the page used to accept taps but paint nothing. An Activity renders the same WebView
 * on the normal window layer, so Chromium composites normally.
 *
 * The signed-in token is read from the page's localStorage and returned to the caller as
 * [EXTRA_TOKEN] in the result Intent.
 */
class DiscordLoginActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TOKEN = "extra_discord_token"
        private const val LOGIN_URL = "https://discord.com/login"
        private const val BACKGROUND_COLOR = Color.rgb(49, 51, 56)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PastaPlayTheme {
                DiscordLoginScreen(
                    onClose = { finish() },
                    onToken = { token -> deliverToken(token) }
                )
            }
        }
    }

    private fun deliverToken(token: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TOKEN, token))
        finish()
    }

    @Composable
    private fun DiscordLoginScreen(onClose: () -> Unit, onToken: (String) -> Unit) {
        var webView by remember { mutableStateOf<WebView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                webView?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    destroy()
                }
                webView = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.rigakizzy_login_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.rigakizzy_login_dialog_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.rigakizzy_close)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.rigakizzy_login_dialog_waiting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AndroidView(
                factory = { context ->
                    createWebView(context, onToken = onToken).also { webView = it }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.rigakizzy_close))
            }
        }

        // Discord stores the token asynchronously after the login form submits, so keep
        // looking for it while this screen is open (a redirect alone is not enough).
        LaunchedEffect(webView) {
            val view = webView ?: return@LaunchedEffect
            repeat(80) {
                delay(1_500)
                injectTokenIfDiscord(view, view.url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: android.content.Context, onToken: (String) -> Unit): WebView {
        return WebView(context).apply {
            setBackgroundColor(BACKGROUND_COLOR)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onToken(token: String) {
                        Handler(Looper.getMainLooper()).post { onToken(token) }
                    }
                },
                "PastaPlayerDiscord"
            )
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    injectTokenIfDiscord(view, url)
                }

                override fun doUpdateVisitedHistory(
                    view: WebView,
                    url: String?,
                    isReload: Boolean,
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    injectTokenIfDiscord(view, url)
                }
            }
            loadUrl(LOGIN_URL)
        }
    }
}

/** Only touches Discord pages; the login window never runs scripts on anything else. */
private fun injectTokenIfDiscord(webView: WebView, url: String?) {
    val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }.orEmpty()
    if (host.endsWith("discord.com")) {
        webView.evaluateJavascript(TOKEN_SNIPPET, null)
    }
}

private const val TOKEN_SNIPPET = """
(function() {
  try {
    var token = null;
    try { token = window.localStorage.getItem('token'); } catch (e) {}
    if (!token) {
      try {
        var frame = document.createElement('iframe');
        frame.style.display = 'none';
        document.body.appendChild(frame);
        token = frame.contentWindow.localStorage.getItem('token');
        document.body.removeChild(frame);
      } catch (e) {}
    }
    if (token) {
      token = token.replace(/^"|"${'$'}/g, '');
      if (token.length > 20) { PastaPlayerDiscord.onToken(token); }
    }
  } catch (e) {}
})();
"""
