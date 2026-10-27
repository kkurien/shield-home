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

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Exposed to the page as window.ShieldHome.
 *
 * The UI is a local HTML page rather than a Lovelace dashboard, so everything
 * it needs from Android — the installed app list, their icons, the HA
 * connection details — comes through here.
 */
class HaBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun launchApp(packageName: String) {
        activity.runOnUiThread { activity.launchApp(packageName) }
    }

    @JavascriptInterface
    fun openSettings() {
        activity.runOnUiThread {
            activity.startActivity(Intent(activity, SetupActivity::class.java))
        }
    }

    /** {url, token, entities} — the page connects to HA itself over WebSocket. */
    @JavascriptInterface
    fun config(): String = JSONObject().apply {
        put("url", Config.url(activity))
        put("token", Config.token(activity))
        put("entities", JSONObject(Config.entitiesJson(activity)))
    }.toString()

    /**
     * Every leanback-launchable app, with its icon inlined as a data URI.
     * Icons are small (48dp) so the base64 payload stays modest.
     */
    @JavascriptInterface
    fun apps(): String {
        val pm = activity.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val out = JSONArray()
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != activity.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .forEach { ri ->
                val o = JSONObject()
                o.put("pkg", ri.activityInfo.packageName)
                o.put("label", ri.loadLabel(pm).toString())
                o.put("icon", runCatching { dataUri(ri.activityInfo.loadIcon(pm)) }.getOrDefault(""))
                out.put(o)
            }
        Log.i("ShieldHome", "apps(): " + out.length() + " entries")
        return out.toString()
    }

    private fun dataUri(d: Drawable): String {
        val size = 96
        val bmp = if (d is BitmapDrawable && d.bitmap != null) {
            Bitmap.createScaledBitmap(d.bitmap, size, size, true)
        } else {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
                val c = Canvas(it)
                d.setBounds(0, 0, size, size)
                d.draw(c)
            }
        }
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }
}
