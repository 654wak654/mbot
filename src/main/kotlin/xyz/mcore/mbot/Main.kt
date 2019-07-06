/*
 * Copyright © 2018 Ozan Eğitmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.mcore.mbot

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    JDABuilder(AccountType.BOT)
            .setToken(Conf.get("bot.token"))
            .setEventManager(AnnotatedEventManager())
            .addEventListener(MantarBot())
            .build()
}

class MantarBot {
    private lateinit var activityChannel: TextChannel

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

        activityChannel = event.jda.getGuildById(151408461412827136L).getTextChannelById(597016805445337108L)

        /*
        // Ar da kicker
        // Not implemented because event.jda can be null after this event ends,
        // but the scheduler will keep going long after the event ends
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
        event.guild.controller.addSingleRoleToMember(event.member, event.guild.getRoleById(279359336315092992L)).queue()
    }

    @SubscribeEvent
    fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent) {
        activityChannel.sendMessage("`${event.member.effectiveName}` joined _${event.channelJoined.name}_").queue()
    }

    // TODO: Can't distinguish between user moving and admin moving them
    @SubscribeEvent
    fun onGuildVoiceMoveEvent(event: GuildVoiceMoveEvent) {
        activityChannel.sendMessage("`${event.member.effectiveName}` moved to _${event.channelJoined.name}_").queue()
    }

    // TODO: Can't distinguish between user leaving and admin disconnecting them
    @SubscribeEvent
    fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent) {
        activityChannel.sendMessage("`${event.member.effectiveName}` left").queue()
    }

    @SubscribeEvent
    fun onGuildMemberNickChangeEvent(event: GuildMemberNickChangeEvent) {
        activityChannel.sendMessage("`${event.prevNick}` changed their nickname to `${event.newNick}`").queue()
    }

    @SubscribeEvent
    fun onMessageReceivedEvent(event: MessageReceivedEvent) {
        val message = event.message
        val cm = CommandManager(message.contentRaw)

        cm.check("help") {
            val embed = Embed("Commands:")
            embed.add(".roll <count> <type> + <add>", "Roll a dnd dice. Example: `.roll d6` or `.roll 2d20 + 3`")
            embed.add(".echo <anything>", "mantarBot repeats what you said. Example: `.echo retarded screeching`")
            embed.add(".poke <user>", "\"Pokes\" the tagged user. Example: `.poke @GuerillaTime#3102`")
            embed.add(".channel <name>", "Creates a temporary channel. Example: `.channel \uD83C\uDF7B Chill Room`")
            embed.add(".deleteMessages <count>", "Deletes <count> times messages from current channel.")
            embed.sendTo(message.channel)
        }

        cm.check(".echo\\s+([ -~ÇçĞğİıÖöŞşÜü]+)") { respondTo(message, it[0]) }

        cm.check(".roll\\s+([0-9]|)(\\s*)d(4|6|8|10|12|20)((\\s*)\\+(\\s*)([0-9])|)") {
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

        cm.check(".poke\\s+<@(!|)([0-9]+)>") {
            val privateChannel = message.mentionedUsers[0].openPrivateChannel().complete()
            privateChannel.sendMessage("You've been poked by ${message.author.asMention} in ${message.guild.name}!").queue()
        }

        // TODO: Get rid of regex for this one, or use more inclusive regex
        cm.check(".channel\\s+([ -~ÇçĞğİıÖöŞşÜü]+)") {
            if (message.member.voiceState.channel == null) {
                respondTo(message, "You have to be in a voice channel before you can use the `.channel` command.")
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

        cm.check(".deleteMessages\\s+([0-9]+)") { it ->
            if (message.member.hasPermission(Permission.MESSAGE_MANAGE)) {
                message.channel.iterableHistory.takeAsync(it[0].toInt() - 1).thenAccept { message.channel.purgeMessages(it) }
            }
        }
    }

    private fun respondTo(message: Message, response: String) {
        message.channel.sendMessage("${message.author.asMention}: $response").queue()
    }
}
