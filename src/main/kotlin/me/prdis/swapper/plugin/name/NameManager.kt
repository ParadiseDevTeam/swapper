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

package me.prdis.swapper.plugin.name

import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.objects.SwapperImpl
import org.bukkit.entity.Player
import java.util.*

/**
 * @author Medlar Wreak
 */

object NameManager {
    var Player.fakeName: String?
        get() = SwapperConfig.getFakeName(uniqueId.toString())
        set(value) {
            SwapperConfig.setFakeName(uniqueId.toString(), value)
        }

    var Player.originalName: String?
        get() = SwapperConfig.getOriginalName(uniqueId.toString())
        set(value) {
            SwapperConfig.setOriginalName(uniqueId.toString(), value)
        }

    fun modifyProfile(player: Player, name: String) {
        player.playerProfile = player.playerProfile.apply {
            @Suppress("DEPRECATION", "REMOVAL")
            this.name = name
        }

        player.playerListOrder = findPlayerListOrder(player.uniqueId)
    }

    fun findPlayerListOrder(uuid: UUID): Int {
        val index = SwapperImpl.numberedPlayers.indexOf(uuid)
        return if (index == -1) 0 else SwapperImpl.numberedPlayers.size - index
    }
}