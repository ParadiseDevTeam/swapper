/*
 * Copyright (C) 2026 Paradise Dev Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package me.prdis.swapper.plugin.config

import me.prdis.swapper.plugin.objects.SwapperConsts.plugin
import java.util.*

/**
 * @author Medlar Wreak
 */

object SwapperConfig {
    fun getIsGameRunning(): Boolean {
        return plugin.config.getBoolean("isGameRunning")
    }

    fun setIsGameRunning(enabled: Boolean) {
        plugin.config.set("isGameRunning", enabled)
        plugin.saveConfig()
    }

    fun getHunterUUIDString(): String? {
        return plugin.config.getString("hunterUUID")
    }

    fun setHunterUUIDString(hunterUUIDString: String?) {
        plugin.config.set("hunterUUID", hunterUUIDString)
        plugin.saveConfig()
    }

    fun getNumberedPlayers(): List<UUID>? {
        val configNumPlayers = plugin.config.getStringList("numberedPlayers")

        return if (configNumPlayers.isEmpty()) {
            null
        } else {
            try {
                configNumPlayers.map { UUID.fromString(it) }
            } catch (e: IllegalArgumentException) {
                plugin.componentLogger.error("An error occurred while retrieving numbered players from config: invalid UUID format.")
                e.printStackTrace()
                null
            }
        }
    }

    fun setNumberedPlayers(numberedPlayers: List<UUID>?) {
        plugin.config.set("numberedPlayers", numberedPlayers?.map { it.toString() })
        plugin.saveConfig()
    }

    fun getRemainingTicks(): Int {
        return plugin.config.getInt("remainingTicks", 0)
    }

    fun setRemainingTicks(ticks: Int) {
        plugin.config.set("remainingTicks", ticks)
        plugin.saveConfig()
    }

    fun getFakeName(uniqueId: String): String? {
        return plugin.config.getString("name.${uniqueId}")
    }

    fun setFakeName(uniqueId: String, name: String?) {
        plugin.config.set("name.${uniqueId}", name)
        plugin.saveConfig()
    }

    fun getOriginalName(uniqueId: String): String? {
        return plugin.config.getString("originalName.${uniqueId}")
    }

    fun setOriginalName(uniqueId: String, name: String?) {
        plugin.config.set("originalName.${uniqueId}", name)
        plugin.saveConfig()
    }
}