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

package me.prdis.swapper.plugin.tasks

import me.prdis.swapper.plugin.name.NameManager.fakeName
import me.prdis.swapper.plugin.objects.SwapperConsts.plugin
import me.prdis.swapper.plugin.objects.SwapperConsts.server
import me.prdis.swapper.plugin.objects.SwapperGameManager
import me.prdis.swapper.plugin.objects.SwapperImpl
import net.kyori.adventure.text.Component.text
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Objective

/**
 * @author Medlar Wreak
 */

object SwapperTimer {
    private var timer: BukkitTask? = null
    private var swapperObjective: Objective? = null

    var remainingTick = 20 * 60 // 60 seconds in tick

    fun initiate() {
        timer = server.scheduler.runTaskTimer(plugin, Runnable {
            if (SwapperGameManager.isGameRunning) {
                if (SwapperGameManager.isGameFrozen) {
                    server.sendActionBar(text("현재 게임 일시정지 상태입니다."))
                } else if (SwapperGameManager.isResumeNeeded) {
                    server.sendActionBar(text("게임 재개 대기중입니다."))
                } else {
                    remainingTick--

                    val remainingSeconds = remainingTick.toDouble() / 20
                    val formattedTime = String.format("%.1f", remainingSeconds)

                    swapperObjective?.displayName(text(formattedTime))

                    if (remainingTick <= 0) {
                        SwapperImpl.swapPlayers()
                        remainingTick = 20 * 60
                    }

                    server.onlinePlayers.toSet().filter { it.fakeName != null }.forEach {
                        it.sendActionBar(text("당신은 ${it.fakeName}번 플레이어입니다."))
                    }
                }
            }
        }, 0, 1)
    }

    fun destroy() {
        timer?.cancel()
        timer = null
        remainingTick = 20 * 60
    }

    fun setSwapperObjective(objective: Objective?) {
        swapperObjective = objective
    }
}