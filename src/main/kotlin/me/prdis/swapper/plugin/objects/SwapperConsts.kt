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

import me.prdis.swapper.plugin.SwapperPlugin.Companion.instance
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper
import org.incendo.cloud.paper.util.sender.Source

/**
 * @author Medlar Wreak
 */

@Suppress("UnstableApiUsage")
object SwapperConsts {
    val plugin = instance
    val server = plugin.server

    val PLAYER_TRACKER_KEY = NamespacedKey(instance, "player_tracker")

    val PLAYER_TRACKER = ItemStack.of(Material.COMPASS).apply {
        editMeta {
            it.itemName(text("플레이어 추적기", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            it.persistentDataContainer.set(PLAYER_TRACKER_KEY, PersistentDataType.BOOLEAN, true)
        }
    }

    private val executionCoordinator = ExecutionCoordinator.simpleCoordinator<Source>()
    val commandManager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
        .executionCoordinator(executionCoordinator).buildOnEnable(plugin)
}