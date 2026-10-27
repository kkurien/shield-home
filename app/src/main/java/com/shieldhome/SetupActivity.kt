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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.io.File

/**
 * First-run settings. Can also be driven entirely over adb, which beats typing
 * a long-lived token with a TV remote:
 *
 *   adb shell am start -n com.shieldhome/.SetupActivity \
 *     -e url "http://192.168.1.10:8123" \
 *     -e token "eyJhbGciOi..." \
 *     -e finish true
 *
 * The entity map is too long and too full of quotes to survive two shells, so
 * push it as a file instead and name it with entitiesFile:
 *
 *   adb push entities.json /sdcard/Android/data/com.shieldhome/files/
 *   adb shell am start -n com.shieldhome/.SetupActivity \
 *     -e entitiesFile entities.json -e finish true
 */
class SetupActivity : Activity() {

    /**
     * Reads from the app's own external files directory, which needs no
     * storage permission and is writable by `adb push`.
     */
    private fun readEntitiesFile(name: String): String? {
        val f = File(getExternalFilesDir(null), name.substringAfterLast('/'))
        return try {
            if (f.isFile) f.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Applies any configuration carried by the intent. Returns true if the
     * activity has finished and the caller should stop.
     */
    private fun applyIntent(i: Intent?): Boolean {
        if (i == null) return false
        val hasConfig = i.hasExtra("url") || i.hasExtra("token") ||
            i.hasExtra("path") || i.hasExtra("entities") || i.hasExtra("entitiesFile")
        if (!hasConfig) return false

        var entities = i.getStringExtra("entities")
        val fileName = i.getStringExtra("entitiesFile")
        if (fileName != null) {
            entities = readEntitiesFile(fileName)
            if (entities == null) {
                Toast.makeText(
                    this,
                    "Could not read $fileName from the app's files directory",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Config.save(
            this,
            i.getStringExtra("url"),
            i.getStringExtra("path"),
            i.getStringExtra("token"),
            entities
        )
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()

        if (i.getStringExtra("finish") == "true") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return true
        }
        return false
    }

    /**
     * launchMode is singleTask, so a second `am start` while this screen is
     * already open arrives here rather than at onCreate. Without this, the
     * config in that intent is silently dropped — adb even warns about it
     * ("intent has been delivered to currently running top-most instance").
     */
    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        applyIntent(newIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (applyIntent(intent)) return

        setContentView(R.layout.activity_setup)

        val url = findViewById<EditText>(R.id.url)
        val path = findViewById<EditText>(R.id.path)
        val token = findViewById<EditText>(R.id.token)

        url.setText(Config.url(this))
        path.setText(Config.path(this))
        token.setText(Config.token(this))

        findViewById<Button>(R.id.save).setOnClickListener {
            if (url.text.isBlank() || token.text.isBlank()) {
                Toast.makeText(this, "URL and token are both required", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Config.save(this, url.text.toString(), path.text.toString(), token.text.toString())
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
