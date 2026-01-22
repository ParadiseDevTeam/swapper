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

package me.prdis.swapper.plugin.commands

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.ban.BanListType
import me.prdis.swapper.plugin.SwapperPlugin.Companion.instance
import me.prdis.swapper.plugin.config.SwapperConfig
import me.prdis.swapper.plugin.objects.SwapperConsts.PLAYER_TRACKER
import me.prdis.swapper.plugin.objects.SwapperConsts.commandManager
import me.prdis.swapper.plugin.objects.SwapperConsts.server
import me.prdis.swapper.plugin.objects.SwapperGameManager
import me.prdis.swapper.plugin.objects.SwapperGameManager.isHunter
import me.prdis.swapper.plugin.objects.SwapperImpl
import me.prdis.swapper.plugin.objects.SwapperImpl.bypassExclusions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.BanEntry
import org.incendo.cloud.bukkit.data.SinglePlayerSelector
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser
import org.incendo.cloud.paper.util.sender.PlayerSource
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.permission.PredicatePermission
import java.util.*

/**
 * @author Medlar Wreak
 */

object SwapperCommand {
    fun registerCommands() {
        registerSwapper()
        registerTracker()
        registerBypass()
        registerMelted()
//        registerNamedTp()
        registerResban()
    }

    private fun registerSwapper() {
        val swapper = commandManager.commandBuilder("swapper", { "A Swapper Command." }, "s")
            .permission(PredicatePermission.of { ctx -> ctx.source().isOp })

        commandManager.command(swapper.handler { ctx ->
            ctx.sender().source().sendMessage(text("""
                Swapper 플러그인
                by Paradise Dev Team
                
                도움말
                - 게임 관리 명령어: /swapper game <start|stop|freeze|resume|hunter>
                - 플레이어 추적기 지급: /swapper tracker
                - 재접속 제한 우회 대상 설정: /swapper bypass <UUID>
                - 일시정지 움직임 제한 예외 대상 설정: /swapper melted <player>
                - 서버 내 모든 차단 초기화: /swapper resban
            """.trimIndent()))
        })

        val gameBuilder = swapper.literal("game")

        commandManager.command(gameBuilder.literal("start").handler { ctx ->
            if (SwapperGameManager.isGameRunning) {
                ctx.sender().source().sendMessage(text("게임이 이미 진행중입니다.", NamedTextColor.RED))
                return@handler
            }

            if (SwapperGameManager.hunterUUID.isBlank()) {
                ctx.sender().source().sendMessage(
                    text(
                        "헌터가 설정되지 않았습니다. '/swapper game hunter' 명령을 통해 헌터를 설정한 후 게임을 시작해주세요.",
                        NamedTextColor.RED
                    )
                )
                return@handler
            }

            SwapperGameManager.startGame()
            server.broadcast(text("게임을 시작합니다."))
        })

        commandManager.command(gameBuilder.literal("resume").handler { ctx ->
            if (!SwapperGameManager.isGameRunning) {
                ctx.sender().source().sendMessage(text("진행중인 게임이 없습니다.", NamedTextColor.RED))
                return@handler
            }

            if (!SwapperGameManager.isResumeNeeded) {
                ctx.sender().source().sendMessage(text("게임이 진행 재개 대기 상태가 아닙니다.", NamedTextColor.RED))
                return@handler
            }

            SwapperGameManager.resumeGame()

            server.broadcast(text("게임이 재개되었습니다."))
        })

        commandManager.command(gameBuilder.literal("freeze").handler { ctx ->
            if (!SwapperGameManager.isGameRunning) {
                ctx.sender().source().sendMessage(text("진행중인 게임이 없습니다.", NamedTextColor.RED))
                return@handler
            }

            SwapperGameManager.isGameFrozen = !SwapperGameManager.isGameFrozen

            if (SwapperGameManager.isGameFrozen) {
                server.broadcast(text("게임이 일시정지 상태로 전환되었습니다."))
            } else {
                server.broadcast(text("게임 일시정지가 해제되었습니다."))
            }
        })

        commandManager.command(gameBuilder.literal("stop").handler { ctx ->
            if (!SwapperGameManager.isGameRunning) {
                ctx.sender().source().sendMessage(text("진행중인 게임이 없습니다.", NamedTextColor.RED))
                return@handler
            }

            SwapperGameManager.stopGame()
            ctx.sender().source().sendMessage(text("게임을 종료합니다."))
        })

        commandManager.command(
            gameBuilder.literal("hunter").required(
                "player",
                SinglePlayerSelectorParser.singlePlayerSelectorParser()
            ).handler { ctx ->
                val hunterPlayer = ctx.get<SinglePlayerSelector>("player").single()

                if (SwapperGameManager.isGameRunning) {
                    ctx.sender().source().sendMessage(text("게임 진행중에서는 헌터 설정을 변경할 수 없습니다.", NamedTextColor.RED))
                    return@handler
                }

                SwapperGameManager.hunterUUID = hunterPlayer.uniqueId.toString()
                SwapperConfig.setHunterUUIDString(hunterPlayer.uniqueId.toString())

                ctx.sender().source()
                    .sendMessage(text("${hunterPlayer.name}(UUID ${hunterPlayer.uniqueId})(을)를 헌터로 설정하였습니다."))
            })
    }

    /**
     * 관리자가 직접 재접속 시 "게임 진행중 접속 불가"에 대한 제한을 우회할 대상을 정하는 명령어
     * 헌터가 재접속 하거나 타 관리진이 같이 중간에 관전이 필요한 사람을 접속시키고 싶을 때, 또는
     * 시청자중 마인크래프트 내부적인 오류로 재접속이 꼭 필요한 상황이 확인될 경우에
     * 게임에서 부득이하게 탈락되지 않도록 도울 수 있는 명령어.
     */
    private fun registerBypass() {
        val bypass = commandManager.commandBuilder("bypass", { "A Bypass Command." })
            .permission(PredicatePermission.of { ctx -> ctx.source().isOp })

        commandManager.command(bypass.handler { ctx ->
            ctx.sender().source().sendMessage(text("재접속 제한 우회 대상 UUID 목록:"))
            ctx.sender().source().sendMessage(text(bypassExclusions.joinToString("\n")))
        })

        commandManager.command(bypass.required("uuid", StringParser.quotedStringParser()).handler { ctx ->
            val sender = ctx.sender().source()
            val uuid = ctx.get<String>("uuid")

            try {
                // UUID Validation
                UUID.fromString(uuid)
            } catch (_: IllegalArgumentException) {
                instance.componentLogger.error("Invalid UUID Detected: $uuid")
                sender.sendMessage(text("유효한 UUID 형식이 아닙니다.", NamedTextColor.RED))
                return@handler
            }

            bypassExclusions.add(uuid)
            sender.sendMessage(text("UUID ${uuid}의 플레이어를 재접속 제한 우회 대상으로 적용하였습니다."))
        })
    }

    /**
     * 게임 일시정지 상태에서 움직임 제한이 해제된 플레이어를 지정하는 명령어
     */
    private fun registerMelted() {
        val melted = commandManager.commandBuilder("melted", { "A Melted Command." })
            .required("target", SinglePlayerSelectorParser.singlePlayerSelectorParser())
            .permission(PredicatePermission.of { ctx -> ctx.source().isOp })

        commandManager.command(melted.handler { ctx ->
            val target = ctx.get<SinglePlayerSelector>("target").single()

            SwapperImpl.meltedPlayers.add(target.uniqueId.toString())
            ctx.sender().source()
                .sendMessage(text("${target.name}(UUID ${target.uniqueId})(을)를 일시정지 움직임 제한 예외 목록에 추가하였습니다."))
        })
    }

    /**
     * 플레이어 추적기 명령어 등록
     */
    private fun registerTracker() {
        val tracker = commandManager.commandBuilder("tracker", { "A Tracker Command." }, "t", "나침반")
            .senderType(PlayerSource::class.java)
            .permission(PredicatePermission.of { ctx -> ctx.source().isHunter })

        commandManager.command(tracker.handler { ctx ->
            val player = ctx.sender().source()
            player.give(PLAYER_TRACKER)
            player.sendMessage(text("플레이어 추적기를 지급했습니다."))
        })
    }

    /**
     * 번호별 이름 텔레포트 명령어 등록
     */
//    private fun registerNamedTp() {
//        val tpBuilder = commandManager.commandBuilder("namedtp", { "A TP Command." }, "ntp")
//            .permission(PredicatePermission.of { ctx -> ctx.source().isOp }).senderType(PlayerSource::class.java)
//
//        commandManager.command(
//            tpBuilder.required("num1", IntegerParser.integerParser(1, 100)).optional(
//                "num2", IntegerParser.integerParser(0, 100)
//            ).handler { ctx ->
//                val player = ctx.sender().source()
//                val num1 = ctx.get<Int>("num1")
//                val num2 = ctx.getOrDefault("num2", -1)
//
//                val player1 = server.onlinePlayers.find { it.fakeName == "$num1" }
//                val player2 = server.onlinePlayers.find { it.fakeName == "$num2" }
//
//                if (player1 != null) {
//                    if (player2 != null) {
//                        player1.teleportAsync(player2.location)
//                        player1.sendMessage(text("Teleported ${player1.name} to ${player2.name}"))
//                        return@handler
//                    }
//
//                    player.teleportAsync(player1.location)
//                    player.sendMessage(text("Teleported to ${player1.name}"))
//                } else {
//                    player.sendMessage(text("$num1 번호의 플레이어를 서버에서 찾을 수 없습니다.", NamedTextColor.RED))
//                    return@handler
//                }
//            })
//    }

    /**
     * 서버 내 모든 차단을 초기화하는 명령어 등록
     */
    private fun registerResban() {
        val resbanBuilder = commandManager.commandBuilder("resban", { "A Resban Command." }, "banreset", "unbanall")
            .permission(PredicatePermission.of { ctx -> ctx.source().isOp })
        commandManager.command(resbanBuilder.handler { ctx ->
            val banlist = server.getBanList(BanListType.PROFILE)

            banlist.getEntries<BanEntry<PlayerProfile>>().forEach {
                it.remove()
            }

            ctx.sender().source().sendMessage(text("서버 내 차단을 전부 초기화하였습니다."))
        })
    }
}