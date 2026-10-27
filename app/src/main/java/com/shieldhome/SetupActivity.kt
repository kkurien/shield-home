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

/**
 * First-run settings. Can also be driven entirely over adb, which beats typing
 * a long-lived token with a TV remote:
 *
 *   adb shell am start -n com.shieldhome/.SetupActivity \
 *     -e url "http://192.168.1.10:8123" \
 *     -e path "/lovelace-tv/0" \
 *     -e token "eyJhbGciOi..." \
 *     -e finish true
 */
class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Headless configuration via intent extras
        val i = intent
        if (i?.hasExtra("url") == true || i?.hasExtra("token") == true ||
            i?.hasExtra("path") == true || i?.hasExtra("entities") == true
        ) {
            Config.save(
                this,
                i.getStringExtra("url"),
                i.getStringExtra("path"),
                i.getStringExtra("token"),
                i.getStringExtra("entities")
            )
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            if (i.getStringExtra("finish") == "true") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
        }

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
