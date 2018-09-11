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

import java.util.regex.Pattern

class CommandManager(private val message: String) {
    fun register(regex: String, command: (args: Array<String>) -> Unit) {
        val matcher = Pattern.compile(regex).matcher(message)
        if (matcher.matches()) {
            command(Array(matcher.groupCount()) { matcher?.group(it + 1) ?: "" })
        }
    }
}
