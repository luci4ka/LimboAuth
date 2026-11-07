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

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.j256.ormlite.dao.Dao;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.migration.MigrationHash;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.model.SQLRuntimeException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Единая точка для проверки и генерации хешей паролей.
 */
public final class PasswordService {

    private static final BCrypt.Verifyer HASH_VERIFIER = BCrypt.verifyer();
    private static final BCrypt.Hasher HASHER = BCrypt.withDefaults();

    @Nullable
    private static MigrationHash migrationHash;

    private PasswordService() {
    }

    public static void reload() {
        migrationHash = Settings.IMP.MAIN.MIGRATION_HASH;
    }

    /**
     * Проверяет пароль против хеша игрока. При успешной миграции со старого алгоритма
     * обновляет хеш в БД.
     */
    public static boolean checkPassword(String password, RegisteredPlayer player, Dao<RegisteredPlayer, String> playerDao) {
        String hash = player.getHash();

        // 1. Основная проверка BCrypt — выполняется всегда, время фиксировано.
        boolean isCorrect = HASH_VERIFIER.verify(
                password.getBytes(StandardCharsets.UTF_8),
                hash.replace("BCRYPT$", "$2a$").getBytes(StandardCharsets.UTF_8)
        ).verified;

        if (isCorrect) {
            return true;
        }

        // 2. Миграция — только если хеш структурно похож на старый формат.
        if (migrationHash != null && migrationHash.looksLike(hash)) {
            boolean migrated = migrationHash.checkPassword(hash, password);
            if (migrated) {
                player.setPassword(password);
                try {
                    playerDao.update(player);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
                return true;
            }
        }

        return false;
    }

    public static String genHash(String password) {
        return HASHER.hashToString(Settings.IMP.MAIN.BCRYPT_COST, password.toCharArray());
    }
}