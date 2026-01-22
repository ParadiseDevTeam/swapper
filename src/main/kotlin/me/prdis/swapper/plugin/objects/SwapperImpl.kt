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
import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.data.SwapperData
import me.prdis.swapper.plugin.data.SwapperSuccessionData
import me.prdis.swapper.plugin.name.NameManager
import me.prdis.swapper.plugin.name.NameManager.fakeName
import me.prdis.swapper.plugin.name.NameManager.originalName
import me.prdis.swapper.plugin.objects.SwapperConsts.server
import me.prdis.swapper.plugin.objects.SwapperGameManager.isHunter
import me.prdis.swapper.plugin.tasks.SwapperTimer
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Difficulty
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import java.util.*

/**
 * @author aroxu, Medlar Wreak
 */

object SwapperImpl {
    /**
     * 플레이어에게 부여된 번호 순서대로 저장하는 UUID 목록입니다.
     */
    val numberedPlayers = mutableListOf<UUID>()

    /**
     * 게임 진행 상태에서 차단되지 않고 입장할 수 있는 플레이어 UUID 목록입니다.
     */
    val bypassExclusions = mutableListOf(
        "389c4c9b-6342-42fc-beb3-922a7d7a72f9",
        "d5226a1c-d720-456a-a0ba-a642ad07ef02",
        "762dea11-9c45-4b18-95fc-a86aab3b39ee",
        "07b373d1-d7b7-49a3-bc87-4ed74271da72"
    )

    /**
     * 게임 일시정지 상태에서 움직일 수 있는 플레이어 목록입니다.
     */
    val meltedPlayers = mutableListOf<String>()

    /**
     * 플레이어들에게 번호를 부여합니다.
     */
    fun grantPlayerNumbers() {
        val viewerPlayers = getViewerPlayers().shuffled()
        numberedPlayers.addAll(viewerPlayers.map { it.uniqueId })
        updateConfigPlayers()
    }

    /**
     * 부여된 플레이어 번호를 초기화합니다.
     */
    fun clearPlayerNumbers() {
        numberedPlayers.clear()
    }

    /**
     * 시청자 플레이어들의 상태를 초기화합니다. 게임을 새로 시작하기 이전에 호출합니다.
     */
    fun initializeViewerPlayer() {
        getViewerPlayers().forEach { player ->
            player.closeInventory()
            player.gameMode = GameMode.SURVIVAL
            player.health = 20.0
            player.foodLevel = 20
            player.saturation = 20f
            player.clearActivePotionEffects()
            player.closeInventory()
            player.inventory.clear()
        }
    }

    /**
     * 플레이어에게 번호가 부여된 이름을 지급합니다.
     */
    fun assignPlayerNames() {
        numberedPlayers.forEachIndexed { index, uuid ->
            val player = uuid.toPlayer
            if (player?.isConnected == true) {
                val value = "${index + 1}"

                player.fakeName = value
                player.originalName = player.name

                NameManager.modifyProfile(player, value)
                player.displayName(text(value))

                player.sendMessage(text("당신은 ${value}번 플레이어입니다.", NamedTextColor.GREEN))
            }
        }
    }

    /**
     * 플레이어에게 부여된 번호를 초기화하고, 원래 이름으로 복구합니다.
     */
    fun revokePlayerNames() {
        numberedPlayers.forEach { uuid ->
            val player = uuid.toPlayer
            if (player?.isConnected == true) {
                val originalName = player.originalName ?: player.name

                player.fakeName = null

                NameManager.modifyProfile(player, originalName)
                player.displayName(text(originalName))

                player.originalName = null
            }
        }
    }

    /**
     * 스코어보드를 초기화합니다.
     */
    fun initializeScoreboard() {
        val scoreboard = server.scoreboardManager.mainScoreboard

        val swapperObjective = scoreboard.getObjective("swapper") ?: scoreboard.registerNewObjective(
            "swapper", Criteria.DUMMY, text("60.0초")
        )

        swapperObjective.displaySlot = DisplaySlot.SIDEBAR

        SwapperTimer.setSwapperObjective(swapperObjective)
    }

    /**
     * 스코어보드를 제거합니다.
     */
    fun destroyScoreboard() {
        val scoreboard = server.scoreboardManager.mainScoreboard
        val swapperObjective = scoreboard.getObjective("swapper")

        swapperObjective?.unregister()
        SwapperTimer.setSwapperObjective(null)
    }

    /**
     * 플레이어를 번호 순서에 따라 순서대로 스왑합니다.
     */
    fun swapPlayers() {
        val reorderList = mutableListOf<SwapperData>()

        numberedPlayers.forEachIndexed { index, uuid ->
            val player = uuid.toPlayer

            if (player?.isConnected == true) {
                val searchIndex = getNextIndex(index)

                player.closeInventory()
                player.updateInventory()

                if (searchIndex != -1) {
                    val targetPlayer = numberedPlayers[searchIndex].toPlayer

                    if (targetPlayer?.isConnected == true) {
                        targetPlayer.closeInventory()
                        targetPlayer.updateInventory()

                        val succession = createSuccessionData(targetPlayer)
                        reorderList.add(
                            SwapperData(
                                searchIndex,
                                player,
                                targetPlayer,
                                targetPlayer.name,
                                succession,
                                targetPlayer.location.clone(),
                            )
                        )
                    } else {
                        player.sendMessage(text("오류: 교체할 플레이어를 찾을 수 없습니다. [TP_NOT_CONNECTED]", NamedTextColor.RED))
                    }
                } else {
                    player.sendMessage(text("오류: 교체할 플레이어를 찾을 수 없습니다. [IDX_ERR]", NamedTextColor.RED))
                }
            }
        }

        reorderList.forEach { data ->
            data.player.closeInventory()
            data.player.updateInventory()

            applySuccession(data.player, data.successionData)

            data.player.updateInventory()

            data.player.teleportAsync(data.targetLocation)
        }

        updateConfigPlayers()
    }

    /**
     * 플레이어의 승계 데이터를 생성합니다.
     */
    fun createSuccessionData(player: Player): SwapperSuccessionData {
        return SwapperSuccessionData(
            inventory = player.inventory.contents,
            armorContents = player.inventory.armorContents,
            health = player.health,
            foodLevel = player.foodLevel,
            saturation = player.saturation,
            exhaustion = player.exhaustion,
            level = player.level,
            exp = player.exp,
            gameMode = player.gameMode,
            potionEffects = player.activePotionEffects
        )
    }

    /**
     * 플레이어에게 승계 데이터를 적용합니다.
     */
    fun applySuccession(to: Player, successionData: SwapperSuccessionData) {
        to.inventory.contents = successionData.inventory
        to.inventory.armorContents = successionData.armorContents
        to.health = successionData.health
        to.foodLevel = successionData.foodLevel
        to.saturation = successionData.saturation
        to.exhaustion = successionData.exhaustion
        to.level = successionData.level
        to.exp = successionData.exp
        to.gameMode = successionData.gameMode
        to.clearActivePotionEffects()
        to.addPotionEffects(successionData.potionEffects)
    }

    /**
     * 관리자와 헌터를 제외한 시청자 플레이어 목록을 반환합니다.
     */
    fun getViewerPlayers(): List<Player> {
        return server.onlinePlayers.filter { it != null && !it.isHunter && !it.isOp }
    }

    /**
     * 게임 승리 조건을 확인합니다.
     */
    fun checkWinningCriteria(): Boolean {
        val viewerPlayers = getViewerPlayers().filter { it.isConnected }

        return viewerPlayers.isEmpty()
    }

    /**
     * 다음에 올 플레이어 인덱스를 반환합니다.
     * 모든 플레이어가 오프라인이거나 IndexOutOfBoundsException을 대비해 안전장치로 -1을 반환합니다.
     * 예) 99 -> 98 ... 1 -> 0, 0 -> 마지막 인덱스
     */
    private fun getNextIndex(num: Int): Int {
        try {
            var targetNum = (num - 1) % numberedPlayers.size // -1 to get the previous index

            if (targetNum < 0) {
                targetNum = numberedPlayers.lastIndex
            }

            // Safe condition
            if (numberedPlayers.all { it.toPlayer == null || it.toPlayer?.isConnected == false }) {
                return -1
            }

            val targetPlayer = numberedPlayers[targetNum].toPlayer

            if (targetPlayer?.isConnected == true) {
                return targetNum
            } else {
                targetNum = (targetNum - 1) % numberedPlayers.size
                return getNextIndex(targetNum + 1) // +1 to offset the -1 in the function call
            }
        } catch (e: IndexOutOfBoundsException) {
            instance.componentLogger.error("[getNextIndex] 플레이어 인덱스 범위를 벗어났습니다.")
            e.printStackTrace()
            return -1
        } catch (e: Exception) {
            // 완전히 예외이기 때문에, 디버깅을 위해 콘솔 로깅으로 개발자에게 알림.
            instance.componentLogger.error("[getNextIndex] 알 수 없는 오류가 발생했습니다.")
            e.printStackTrace()
            return -1
        }
    }

    /**
     * numberedPlayers 설정 파일 데이터를 업데이트합니다.
     */
    fun updateConfigPlayers() {
        SwapperConfig.setNumberedPlayers(numberedPlayers)
    }

    /**
     * 서버 시작 시 실행할 작업들을 설정합니다.
     */
    fun setupStartupTask() {
        setupWorldSettings()
    }

    /**
     * 월드 설정을 초기화합니다.
     */
    private fun setupWorldSettings() {
        for (world in instance.server.worlds) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
            world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true)
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
            world.difficulty = Difficulty.EASY
        }
    }

    /**
     * 서버에서 UUID와 일치하는 플레이어를 불러오고, 서버에 존재하지 않는 경우 null을 반환합니다.
     */
    val UUID.toPlayer
        get() = server.getPlayer(this)
}