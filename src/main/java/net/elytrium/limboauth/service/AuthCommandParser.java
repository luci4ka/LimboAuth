/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.service;

import net.elytrium.limboauth.Settings;

/**
 * Парсит первую «команду» из сообщения игрока в лимбо
 * (например, "/register" → REGISTER).
 */
public final class AuthCommandParser {

    public enum Command {
        INVALID,
        REGISTER,
        LOGIN,
        TOTP
    }

    private AuthCommandParser() {
    }

    public static Command parse(String command) {
        if (Settings.IMP.MAIN.REGISTER_COMMAND.contains(command)) {
            return Command.REGISTER;
        } else if (Settings.IMP.MAIN.LOGIN_COMMAND.contains(command)) {
            return Command.LOGIN;
        } else if (Settings.IMP.MAIN.TOTP_COMMAND.contains(command)) {
            return Command.TOTP;
        } else {
            return Command.INVALID;
        }
    }
}