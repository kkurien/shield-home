/*
 * Shield Home — a Home Assistant home screen for NVIDIA Shield TV.
 * Copyright (C) 2026 Shield Home contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3, as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of it
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.shieldhome

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * The launcher is a single full-screen WebView showing a local page
 * (assets/home.html) that talks to Home Assistant over its WebSocket API.
 *
 * It used to embed a Lovelace dashboard instead. That was abandoned for two
 * reasons a TV makes unavoidable: HA lays out for the ~960 CSS px this panel
 * reports, so cards came out enormous; and Android's WebView has no spatial
 * navigation, so a D-pad could not move focus between Lovelace's cards. Owning
 * the markup fixes both — the page is authored at 1920x1080 and does its own
 * arrow-key navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dash: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android only materialises an app's external files directory once the
        // app asks for it. Do that at launch so `adb push` has somewhere to put
        // an entities file before the app has ever been configured.
        runCatching { getExternalFilesDir(null)?.mkdirs() }

        if (!Config.isConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        dash = WebView(this)
        setContentView(dash)
        Log.i(TAG, "launcher starting")
        loadedSig = configSig()
        setupWebView()
    }

    /**
     * Fingerprint of the settings the current page was built from. The token is
     * reduced to its length so it never reaches the log.
     */
    private fun configSig(): String =
        Config.url(this) + "|" + Config.path(this) + "|" +
            Config.token(this).length + "|" + Config.entitiesJson(this).hashCode()

    private var loadedSig: String = ""

    /**
     * This activity is singleTask, so returning from the settings screen
     * delivers onNewIntent rather than onCreate — and settings are read in
     * onCreate. Without this check a reconfigure appeared to do nothing.
     */
    override fun onResume() {
        super.onResume()
        if (loadedSig.isNotEmpty() && configSig() != loadedSig) {
            Log.i(TAG, "settings changed, reloading")
            recreate()
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        dash.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // The page declares width=1920; honouring it and scaling to fit is
            // what makes a fixed TV layout land correctly on any panel.
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            // home.html is local; HA is plain http on the LAN.
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        dash.setBackgroundColor(0xFF0A0C11.toInt())
        dash.isFocusable = true
        dash.isFocusableInTouchMode = true
        dash.addJavascriptInterface(HaBridge(this), "ShieldHome")

        dash.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.i(TAG, "page finished: $url")
                view.requestFocus()
            }

            override fun onReceivedError(
                view: WebView,
                req: WebResourceRequest,
                err: WebResourceError
            ) {
                Log.w(TAG, "load error " + err.errorCode + " " + err.description + " for " + req.url)
            }
        }

        dash.loadUrl("file:///android_asset/home.html")
    }

    fun launchApp(pkg: String) {
        val pm = packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    /** MENU opens settings; everything else is the page's own business. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_MENU ||
             event.keyCode == KeyEvent.KEYCODE_SETTINGS)
        ) {
            startActivity(Intent(this, SetupActivity::class.java))
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** A launcher has nowhere to go back to; the page handles BACK itself. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* no-op */ }

    companion object {
        private const val TAG = "ShieldHome"
    }
}
