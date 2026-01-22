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

package me.prdis.swapper.plugin.objects

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.ban.BanListType
import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.objects.SwapperConsts.plugin
import me.prdis.swapper.plugin.objects.SwapperConsts.server
import me.prdis.swapper.plugin.tasks.SwapperTimer
import org.bukkit.BanEntry
import org.bukkit.entity.Player

/**
 * @author Medlar Wreak
 */

object SwapperGameManager {
    var isGameRunning = false
    var isResumeNeeded = false
    var isGameFrozen = false

    var hunterUUID = ""

    val Player.isHunter
        get() = this.uniqueId.toString() == hunterUUID

    fun startGame() {
        isGameRunning = true
        SwapperConfig.setIsGameRunning(true)

        SwapperImpl.initializeViewerPlayer()
        SwapperImpl.setupStartupTask()

        SwapperImpl.grantPlayerNumbers()
        SwapperImpl.assignPlayerNames()

        SwapperImpl.initializeScoreboard()
        SwapperTimer.initiate()
    }

    fun resumeGame() {
        isGameRunning = true
        isResumeNeeded = false

        SwapperConfig.setIsGameRunning(true)

        SwapperImpl.initializeScoreboard()
    }

    fun stopGame(stopWithNextResume: Boolean = false) {
        isGameRunning = false
        isResumeNeeded = stopWithNextResume
        isGameFrozen = false

        hunterUUID = ""

        SwapperConfig.setIsGameRunning(stopWithNextResume)

        if (!stopWithNextResume) {
            SwapperImpl.revokePlayerNames()
            SwapperImpl.clearPlayerNumbers()

            plugin.config.getConfigurationSection("name")?.getKeys(true)?.forEach {
                plugin.config.set("name.${it}", null)
            }

            val banlist = server.getBanList(BanListType.PROFILE)

            banlist.getEntries<BanEntry<PlayerProfile>>().forEach {
                it.remove()
            }

            SwapperConfig.setNumberedPlayers(null)
            SwapperConfig.setRemainingTicks(20 * 60)
            SwapperConfig.setHunterUUIDString(null)
        } else {
            SwapperConfig.setNumberedPlayers(SwapperImpl.numberedPlayers)
            SwapperConfig.setRemainingTicks(SwapperTimer.remainingTick)
        }

        SwapperImpl.destroyScoreboard()
        SwapperTimer.destroy()

        plugin.saveConfig()
    }
}