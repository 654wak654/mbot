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

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageChannel
import java.awt.Color

class Embed(title: String) : EmbedBuilder() {
    init {
        setColor(Color(255, 0, 0))
        setTitle(title)
        setFooter("mantarBot is powered by JDA: https://git.io/fAwSD", null)
    }

    fun add(title: String, content: String) {
        addField(title, content, false)
    }

    fun sendTo(channel: MessageChannel) {
        channel.sendMessage(build()).queue()
    }
}
