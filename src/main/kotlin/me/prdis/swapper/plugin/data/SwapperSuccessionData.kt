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

package me.prdis.swapper.plugin.data

import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

/**
 * @author Medlar Wreak
 */

data class SwapperSuccessionData(
    val inventory: Array<ItemStack?>,
    val armorContents: Array<ItemStack?>,
    val health: Double,
    val foodLevel: Int,
    val saturation: Float,
    val exhaustion: Float,
    val level: Int,
    val exp: Float,
    val gameMode: GameMode,
    val potionEffects: Collection<PotionEffect>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SwapperSuccessionData

        if (health != other.health) return false
        if (foodLevel != other.foodLevel) return false
        if (saturation != other.saturation) return false
        if (exhaustion != other.exhaustion) return false
        if (level != other.level) return false
        if (exp != other.exp) return false
        if (!inventory.contentEquals(other.inventory)) return false
        if (!armorContents.contentEquals(other.armorContents)) return false
        if (gameMode != other.gameMode) return false
        if (potionEffects != other.potionEffects) return false

        return true
    }

    override fun hashCode(): Int {
        var result = health.hashCode()
        result = 31 * result + foodLevel
        result = 31 * result + saturation.hashCode()
        result = 31 * result + exhaustion.hashCode()
        result = 31 * result + level
        result = 31 * result + exp.hashCode()
        result = 31 * result + inventory.contentHashCode()
        result = 31 * result + armorContents.contentHashCode()
        result = 31 * result + gameMode.hashCode()
        result = 31 * result + potionEffects.hashCode()
        return result
    }
}
