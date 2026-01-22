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

package me.prdis.swapper.plugin

import me.prdis.swapper.plugin.commands.SwapperCommand
import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.events.SwapperEvent
import me.prdis.swapper.plugin.objects.SwapperConsts.plugin
import me.prdis.swapper.plugin.objects.SwapperGameManager
import me.prdis.swapper.plugin.objects.SwapperImpl
import me.prdis.swapper.plugin.objects.SwapperImpl.setupStartupTask
import me.prdis.swapper.plugin.tasks.SwapperTimer
import net.kyori.adventure.text.Component.text
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author aroxu, Medlar Wreak
 */

class SwapperPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: SwapperPlugin
            private set
    }

    override fun onEnable() {
        instance = this

        if (SwapperConfig.getIsGameRunning()) {
            SwapperGameManager.isGameRunning = true
            SwapperGameManager.isResumeNeeded = true

            val configNumPlayers = SwapperConfig.getNumberedPlayers()
            if (configNumPlayers != null) SwapperImpl.numberedPlayers.addAll(configNumPlayers)
            else {
                plugin.componentLogger.error(text("An error occurred while resuming the game: config numberedPlayers is null"))
                return
            }

            val hunterUUIDString = SwapperConfig.getHunterUUIDString()

            if (hunterUUIDString != null) {
                SwapperGameManager.hunterUUID = hunterUUIDString
            } else {
                plugin.componentLogger.error(text("An error occurred while resuming the game: config hunterUUID is null"))
                return
            }

            SwapperTimer.remainingTick = SwapperConfig.getRemainingTicks()
            SwapperTimer.initiate()
        }

        server.pluginManager.registerEvents(SwapperEvent, this)

        SwapperCommand.registerCommands()

        setupStartupTask()
    }

    override fun onDisable() {
        SwapperGameManager.stopGame(SwapperGameManager.isGameRunning)
        saveConfig()
    }
}