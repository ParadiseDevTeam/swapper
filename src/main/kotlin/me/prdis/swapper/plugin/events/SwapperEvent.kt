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

package me.prdis.swapper.plugin.events

import io.papermc.paper.ban.BanListType
import io.papermc.paper.event.player.AsyncChatEvent
import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.name.NameManager
import me.prdis.swapper.plugin.name.NameManager.fakeName
import me.prdis.swapper.plugin.name.NameManager.originalName
import me.prdis.swapper.plugin.objects.SwapperConsts.PLAYER_TRACKER_KEY
import me.prdis.swapper.plugin.objects.SwapperConsts.plugin
import me.prdis.swapper.plugin.objects.SwapperConsts.server
import me.prdis.swapper.plugin.objects.SwapperGameManager
import me.prdis.swapper.plugin.objects.SwapperGameManager.isHunter
import me.prdis.swapper.plugin.objects.SwapperImpl
import me.prdis.swapper.plugin.objects.SwapperImpl.bypassExclusions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.meta.CompassMeta
import java.util.*

/**
 * @author aroxu, Medlar Wreak
 */

object SwapperEvent : Listener {
    private val bannedItems = setOf(
        Material.SHIELD,        // 방패
        Material.MACE,          // 철퇴
        Material.WIND_CHARGE    // 돌풍구
    )

    @EventHandler
    fun AsyncPlayerPreLoginEvent.onAsyncPlayerPreLogin() {
        if (SwapperGameManager.isGameRunning) {
            if (SwapperGameManager.isResumeNeeded) {
                val hunterUUIDString = SwapperConfig.getHunterUUIDString()

                if (uniqueId !in SwapperImpl.numberedPlayers && uniqueId.toString() != hunterUUIDString) {
                    disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        text("게임 진행 중에는 서버에 입장하실 수 없습니다.", NamedTextColor.RED)
                    )
                    return
                }
            } else {
                if (uniqueId.toString() !in bypassExclusions) {
                    disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        text("게임 진행 중에는 서버에 입장하실 수 없습니다.", NamedTextColor.RED)
                    )
                    return
                }
            }
        }
    }

    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        val data = player.fakeName

        if (!data.isNullOrEmpty()) {
            joinMessage(
                translatable(
                    "multiplayer.player.joined", NamedTextColor.YELLOW, text(data, NamedTextColor.YELLOW)
                )
            )

            player.displayName(text(data))
            NameManager.modifyProfile(player, data)
        }

        if (player.isOp) joinMessage(null)

        if (player.isHunter || player.isOp) {
            player.playerListOrder = 0

            if (SwapperGameManager.isGameFrozen) player.sendMessage(text("현재 게임이 일시정지 상태입니다.", NamedTextColor.GRAY))
            if (SwapperGameManager.isResumeNeeded) player.sendMessage(
                text(
                    "게임 재개가 필요합니다. '/swapper game resume'을 통해 게임을 재개해주세요.", NamedTextColor.GRAY
                )
            )
        }
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        val data = player.fakeName
        val originalName = player.originalName ?: player.name

        if (SwapperGameManager.isGameRunning && !(SwapperGameManager.isResumeNeeded || SwapperGameManager.isGameFrozen)) {
            val originalProfile = server.createProfile(player.uniqueId, player.name)

            quitMessage(null)

            if (!player.isOp) {
                server.getBanList(BanListType.PROFILE).addBan(originalProfile, "게임에서 퇴장하였습니다.", (null as Date?), null)
            } else {
                if (player.isHunter) {
                    SwapperGameManager.isResumeNeeded = true
                    server.broadcast(text("헌터가 퇴장하였습니다. 게임을 재개 대기 상태로 전환합니다."))
                }

                return
            }

            server.scheduler.runTaskLater(plugin, Runnable {
                server.broadcast(text("${data}번이 사망하였습니다. 남은 시청자: ${SwapperImpl.getViewerPlayers().size}명"))

                val isHunterWon = SwapperImpl.checkWinningCriteria()

                if (isHunterWon) {
                    server.broadcast(text("헌터 승리!"))
                    SwapperGameManager.stopGame()
                }
            }, 2L)
        }

        NameManager.modifyProfile(player, originalName)
    }

    @EventHandler
    fun PlayerKickEvent.onKick() {
        if (SwapperGameManager.isGameRunning) {
            leaveMessage(text(""))
        }
    }

    @EventHandler
    fun PlayerMoveEvent.onMove() {
        if (SwapperGameManager.isResumeNeeded || SwapperGameManager.isGameFrozen) {
            if (!player.isOp && !player.isHunter && player.uniqueId.toString() !in SwapperImpl.meltedPlayers) {
                if (from.x != to.x || from.y != to.y || from.z != to.z) {
                    isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun EntityDamageEvent.onEntityDamage() {
        if (entity is Player) {
            if (SwapperGameManager.isResumeNeeded || SwapperGameManager.isGameFrozen) {
                isCancelled = true
            }
        }
    }

    @EventHandler
    fun EntityMountEvent.onEntityMount() {
        if (entity is Player && (SwapperGameManager.isResumeNeeded || SwapperGameManager.isGameFrozen)) {
            isCancelled = true
        }
    }

    @EventHandler
    fun PlayerDeathEvent.onDeath() {
        if (SwapperGameManager.isGameRunning) {
            if (player.isHunter) {
                server.broadcast(text("헌터가 사망하였습니다. 게임을 종료합니다."))
                SwapperGameManager.stopGame()
                return
            }

            if (player.isOp) return

            deathMessage(null)

            server.scheduler.runTaskLater(plugin, Runnable {
                val viewerPlayers = SwapperImpl.getViewerPlayers()
                if (viewerPlayers.isEmpty()) {
                    server.broadcast(text("헌터 승리!"))
                    SwapperGameManager.stopGame()
                    return@Runnable
                }

                val originalProfile = server.createProfileExact(player.uniqueId, player.name)

                player.kick(text("게임에서 사망하였습니다."))
                server.getBanList(BanListType.PROFILE).addBan(originalProfile, "게임에서 사망하였습니다.", (null as Date?), null)
            }, 2)
        }
    }

    @EventHandler
    fun AsyncChatEvent.onChat() {
        if (!player.isOp) isCancelled = true
    }

    // 아이템 줍기 방지
    @EventHandler
    fun EntityPickupItemEvent.onPickup() {
        if (entity !is Player) return

        if (item.itemStack.type in bannedItems) {
            isCancelled = true
        }

        // 추적기 줍기 방지 - 헌터 전용
        if (item.itemStack.itemMeta.persistentDataContainer.has(PLAYER_TRACKER_KEY)) {
            isCancelled = true
        }
    }

    // 인벤토리에서 아이템 클릭/이동 방지
    @EventHandler
    fun InventoryClickEvent.onInventoryClick() {
        if (whoClicked is Player) {
            val player = whoClicked as Player
            val clickedItem = currentItem ?: return

            if (!(player.isOp || player.isHunter) && clickedItem.type in bannedItems) {
                isCancelled = true
                player.sendMessage(
                    text("이 아이템은 사용할 수 없습니다.", NamedTextColor.RED)
                )
            }
        }
    }

    @EventHandler
    fun PlayerDropItemEvent.onDropItem() {
        if (itemDrop.itemStack.type == Material.COMPASS && (itemDrop.itemStack.itemMeta.persistentDataContainer.has(
                PLAYER_TRACKER_KEY
            ))
        ) {
            isCancelled = true
        }
    }

    @EventHandler
    fun PlayerInteractEvent.onPlayerInteract() {
        val item = item ?: return

        if (player.isHunter) {
            if (item.type == Material.COMPASS && item.itemMeta.persistentDataContainer.has(PLAYER_TRACKER_KEY)) {
                if (action.isRightClick) {
                    item.itemMeta = item.itemMeta.apply {
                        val nearbyTarget = player.world.players.filter { !it.isOp && it.gameMode == GameMode.SURVIVAL }
                            .map { it.location }.minByOrNull { it.distance(player.location) }
                        this as CompassMeta

                        this.isLodestoneTracked = false
                        this.lodestone = nearbyTarget

                        if (lodestone == null) player.sendMessage(
                            text(
                                "동일한 월드 내에 같이 있는 시청자가 없습니다.", NamedTextColor.RED
                            )
                        )
                    }
                    item.apply {
                        editMeta {
                            it.displayName(
                                text("플레이어 추적기", NamedTextColor.GOLD).decoration(
                                    TextDecoration.ITALIC, false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun PlayerCommandPreprocessEvent.onPlayerCommandPreprocessEvent() {
        if (!player.isOp) {
            plugin.componentLogger.info(text("Unauthorized command attempt by ${player.name}: $message"))
            isCancelled = true
        }
    }

    @EventHandler
    fun ProjectileLaunchEvent.onProjectileLaunch() {
        if (entity is EnderPearl || entity is WindCharge) isCancelled = true
    }

    // 아이템 조합 방지
    @EventHandler
    fun PrepareItemCraftEvent.onCraft() {
        val result = inventory.result ?: return

        if (result.type in bannedItems) {
            inventory.result = null
        }
    }
}