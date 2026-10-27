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

import android.content.Context

/**
 * Connection settings, stored on-device rather than compiled in, so changing
 * them needs no rebuild and the APK carries no credential.
 */
object Config {
    private const val PREFS = "shieldhome"
    const val KEY_URL = "ha_url"
    const val KEY_PATH = "ha_path"
    const val KEY_TOKEN = "ha_token"
    const val KEY_ENTITIES = "ha_entities"

    /**
     * Which entities fill each panel of the home screen. Overridable with
     *   adb shell am start -n com.shieldhome/.SetupActivity -e entities '<json>'
     * so the layout can be retargeted without a rebuild.
     */
    private const val DEFAULT_ENTITIES = """{
      "weather": "weather.forecast_home",
      "appOrder": ["Netflix", "YouTube", "Plex", "Prime Video", "Disney+"],
      "appLast": [],
      "controlGroups": [
        {"name": "Lights", "items": [
          {"entity": "light.living_room", "name": "Living room"},
          {"entity": "light.bedroom",     "name": "Bedroom"},
          {"entity": "switch.ceiling_light", "name": "Ceiling"}
        ]},
        {"name": "Devices", "items": [
          {"entity": "switch.desktop_pc", "name": "Desktop PC",
           "status": "binary_sensor.desktop_pc_online"}
        ]}
      ],
      "climate": "climate.thermostat",
      "appliances": [
        {"entity": "sensor.washer_machine_state", "name": "Washer",
         "detail": "sensor.washer_completion_time"},
        {"entity": "sensor.dryer_machine_state",  "name": "Dryer",
         "detail": "sensor.dryer_completion_time"}
      ],
      "vacuum": {
        "entity": "vacuum.robot",
        "name": "Vacuum"
      },
      "cameras": [
        {"entity": "camera.front_door", "name": "Front door"},
        {"entity": "camera.back_yard",  "name": "Back yard"}
      ]
    }"""

    fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun url(c: Context): String = prefs(c).getString(KEY_URL, "").orEmpty().trimEnd('/')
    fun path(c: Context): String = normalizePath(prefs(c).getString(KEY_PATH, "/lovelace/0"))
    fun token(c: Context): String = prefs(c).getString(KEY_TOKEN, "").orEmpty()

    fun entitiesJson(c: Context): String =
        prefs(c).getString(KEY_ENTITIES, DEFAULT_ENTITIES).orEmpty().ifBlank { DEFAULT_ENTITIES }

    /** "dashboard-custom/0" and "/dashboard-custom/0" must behave the same. */
    private fun normalizePath(raw: String?): String {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return "/lovelace/0"
        return if (t.startsWith("/")) t else "/$t"
    }

    fun isConfigured(c: Context) = url(c).isNotEmpty() && token(c).isNotEmpty()

    fun save(
        c: Context,
        url: String?,
        path: String?,
        token: String?,
        entities: String? = null
    ) {
        prefs(c).edit().apply {
            url?.let { putString(KEY_URL, it.trim().trimEnd('/')) }
            path?.let { putString(KEY_PATH, normalizePath(it)) }
            token?.let { putString(KEY_TOKEN, it.trim()) }
            entities?.takeIf { it.isNotBlank() }?.let { putString(KEY_ENTITIES, it.trim()) }
        }.apply()
    }
}
