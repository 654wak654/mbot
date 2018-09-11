/*
 * Copyright (C) 2018 Ozan Egitmen
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package xyz.mcore.mbot

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    JDABuilder(AccountType.BOT)
            .setToken(Conf.get("bot.token"))
            .setEventManager(AnnotatedEventManager())
            .addEventListener(MantarBot())
            .build()
}

class MantarBot {
    @SubscribeEvent
    fun onReadyEvent(event: ReadyEvent) {
        val name = Conf.get("presence.name")
        val game = when (Conf.get("presence.activity").toLowerCase()) {
            "playing" -> Game.playing(name)
            "watching" -> Game.watching(name)
            "listening" -> Game.listening(name)
            else -> null
        }
        if (game != null) {
            event.jda.presence.setPresence(OnlineStatus.ONLINE, game)
        }

        /*
        // Ar da kicker
        // Not implemented because event.jda is null after this event ends,
        // but the scheduler needs to keep going after the event
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
            val mco = event.jda.getGuildById(151408461412827136L)
            val arda = mco.getMemberById(279702081168998400L)

            if (arda.voiceState.channel != null && arda.voiceState.channel.idLong != 239708818592759810L) {
                mco.controller.moveVoiceMember(arda, mco.getVoiceChannelById(239708818592759810L)).queue()
            }
        }, 0, 72, TimeUnit.HOURS)
        */
    }

    @SubscribeEvent
    fun onGuildMemberJoinEvent(event: GuildMemberJoinEvent) {
        event.member.roles.add(event.guild.getRoleById(279359336315092992L))
    }

    @SubscribeEvent
    fun onMessageReceivedEvent(event: MessageReceivedEvent) {
        val message = event.message
        val cm = CommandManager(message.contentRaw)

        cm.register("#help") {
            val embed = Embed("Commands:")
            embed.add("#roll <count> <type> + <add>", "Roll a dnd dice. Example: `#roll d6` or `#roll 2d20 + 3`")
            embed.add("#echo <anything>", "mantarBot repeats what you said. Example: `#echo retarded screeching`")
            embed.add("#poke <user>", "\"Pokes\" the tagged user. Example: `#poke @GuerillaTime#3102`")
            embed.add("#channel <name>", "Creates a temporary channel. Example: `#channel \uD83C\uDF7B Chill Room`")
            embed.add("#kick <user>", "Kicks user only from voice channel. Example: `#kick @TheDeadlyRacoon#1826`")
            embed.add("#deleteMessages <count>", "Deletes <count> times messages from current channel.")
            embed.sendTo(message.channel)
        }

        cm.register("#echo ([ -~ÇçĞğİıÖöŞşÜü]+)") { respondTo(message, it[0]) }

        cm.register("#roll ([0-9]|)( |)d(4|6|8|10|12|20)(( |)\\+( |)([0-9])|)") { it ->
            val count = if (it[0].isEmpty()) 1 else it[0].toInt()
            val dice = it[2].toInt()
            val add = if (it[6].isEmpty()) 0 else it[6].toInt()

            var totalRoll = 0
            var rolls = "\uD83C\uDFB2 "
            var crit = false
            for (i in 1..count) {
                val roll = Random().nextInt(dice) + 1
                totalRoll += roll
                rolls += if (roll == dice) {
                    crit = true
                    "**($roll)** + "
                } else {
                    "($roll) + "
                }
            }

            if (add == 0) {
                rolls = rolls.substring(0, rolls.length - 2)
            } else {
                rolls += add
            }

            respondTo(message, "$rolls = ${totalRoll + add}${if (crit) ", critical!" else ""}")
        }

        // Commands below don't work in private messages since they need a guild to work
        if (!message.isFromType(ChannelType.TEXT)) {
            return
        }

        cm.register("#poke <@!([0-9]+)>") {
            val privateChannel = message.mentionedUsers[0].openPrivateChannel().complete()
            privateChannel.sendMessage("You've been poked by ${message.author.asMention} in ${message.guild.name}!").queue()
        }

        cm.register("#channel ([ -~ÇçĞğİıÖöŞşÜü]+)") {
            if (message.member.voiceState.channel == null) {
                respondTo(message, "You have to be in a voice channel before you can use the `#channel` command.")
            } else {
                val voiceChannel = message.guild.getVoiceChannelById(message.guild.controller.createVoiceChannel(it[0]).complete().idLong)
                message.guild.controller.moveVoiceMember(message.member, voiceChannel).complete()

                val scheduler = Executors.newSingleThreadScheduledExecutor()
                scheduler.scheduleAtFixedRate({
                    if (voiceChannel == null) {
                        scheduler.shutdown()
                    } else if (voiceChannel.members.isEmpty()) {
                        voiceChannel.delete().queue()
                        scheduler.shutdown()
                    }
                }, 1, 1, TimeUnit.SECONDS)
            }
        }

        cm.register("#kick <@!([0-9]+)>") {
            if (message.member.hasPermission(Permission.KICK_MEMBERS)) {
                val member = message.mentionedMembers[0]
                if (member.voiceState.channel != null) {
                    val voiceChannel = message.guild.getVoiceChannelById(message.guild.controller.createVoiceChannel("temp").complete().idLong)
                    message.guild.controller.moveVoiceMember(message.member, voiceChannel).complete()
                    voiceChannel.delete().queue()
                }
            }
        }

        cm.register("#deleteMessages ([0-9]+)") { it ->
            if (message.member.hasPermission(Permission.MESSAGE_MANAGE)) {
                message.channel.getHistoryBefore(message, it[0].toInt() - 1).queue { messageHistory ->
                    message.textChannel.deleteMessages(messageHistory.retrievedHistory)
                }
            }
        }
    }

    private fun respondTo(message: Message, response: String) {
        message.channel.sendMessage("${message.author.asMention}: $response").queue()
    }
}
