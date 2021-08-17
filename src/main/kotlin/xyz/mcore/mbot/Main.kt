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

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    JDABuilder.createLight(Conf.get("bot.token"))
        .addEventListeners(MantarBot())
        .build()
}

class MantarBot : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        val name = Conf.get("presence.name")
        val game = when (Conf.get("presence.activity").lowercase(Locale.getDefault())) {
            "playing" -> Activity.playing(name)
            "watching" -> Activity.watching(name)
            "listening" -> Activity.listening(name)
            else -> null
        }
        event.jda.presence.setPresence(OnlineStatus.ONLINE, game)
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.addRoleToMember(event.member, event.guild.getRoleById(279359336315092992L)!!).queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        val cm = CommandManager(message.contentRaw.trim())

        cm.check("help") {
            val embed = Embed("Commands:")
            embed.add(".echo <anything>", "mantarBot repeats what you said. Example: `.echo retarded screeching`")
            embed.add(".roll <count> <type> + <add>", "Roll a dnd dice. Example: `.roll d6` or `.roll 2d20 + 3`")
            embed.add(".channel <name>", "Creates a temporary channel. Example: `.channel \uD83C\uDF7B Chill Room`")
            embed.add(".deleteMessages <count>", "Deletes <count> times messages from current channel.")
            embed.sendTo(message.channel)
        }

        cm.check("^\\.echo\\s+([\\w ]+)\$") { respondTo(message, it[0]) }

        cm.check("^\\.roll\\s+([0-9]|)(\\s*)d(4|6|8|10|12|20)((\\s*)\\+(\\s*)([0-9])|)\$") {
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

        cm.check("^\\.channel\\s+([\\w ]+)\$") {
            if (message.member!!.voiceState!!.channel == null) {
                respondTo(message, "You have to be in a voice channel before you can use the `.channel` command.")
            } else {
                val voiceChannel = message.guild.getVoiceChannelById(message.guild.createVoiceChannel(it[0]).complete().idLong)
                message.guild.moveVoiceMember(message.member!!, voiceChannel).complete()

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

        cm.check("^\\.deleteMessages\\s+([0-9]+)$") { it ->
            if (message.member!!.hasPermission(Permission.MESSAGE_MANAGE)) {
                message.channel.iterableHistory.takeAsync(it[0].toInt() + 1).thenAccept { message.channel.purgeMessages(it) }
            }
        }
    }

    private fun respondTo(message: Message, response: String) {
        message.channel.sendMessage("${message.author.asMention}: $response").queue()
    }
}
