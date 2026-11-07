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

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.sql.SQLException;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.model.SQLRuntimeException;

/**
 * Общие операции над записью игрока в БД + кешем.
 */
public final class PlayerService {

    private final LimboAuth plugin;
    private final Dao<RegisteredPlayer, String> playerDao;

    public PlayerService(LimboAuth plugin, Dao<RegisteredPlayer, String> playerDao) {
        this.plugin = plugin;
        this.playerDao = playerDao;
    }

    /**
     * Обновляет хеш пароля и сбрасывает кеш. Возвращает старый хеш.
     */
    public String updatePasswordHash(String lowercaseNickname, String newHash) throws SQLException {
        RegisteredPlayer player = AuthSessionHandler.fetchInfoLowercased(this.playerDao, lowercaseNickname);
        String oldHash = player != null ? player.getHash() : "";

        UpdateBuilder<RegisteredPlayer, String> updateBuilder = this.playerDao.updateBuilder();
        updateBuilder.where().eq(RegisteredPlayer.LOWERCASE_NICKNAME_FIELD, lowercaseNickname);
        updateBuilder.updateColumnValue(RegisteredPlayer.HASH_FIELD, newHash);
        updateBuilder.update();

        this.plugin.removePlayerFromCacheLowercased(lowercaseNickname);
        return oldHash;
    }

    /**
     * Устанавливает TOTP-секрет (или очищает, если secret пустой).
     */
    public void setTotpSecret(String lowercaseNickname, String secret) throws SQLException {
        UpdateBuilder<RegisteredPlayer, String> updateBuilder = this.playerDao.updateBuilder();
        updateBuilder.where().eq(RegisteredPlayer.LOWERCASE_NICKNAME_FIELD, lowercaseNickname);
        updateBuilder.updateColumnValue(RegisteredPlayer.TOTP_TOKEN_FIELD, secret);
        updateBuilder.update();
    }

    /**
     * Удаляет запись игрока, сбрасывает кеш и (опционально) кикает его с прокси.
     * Возвращает true, если запись была удалена.
     */
    public boolean unregister(String nickname, String lowercaseNickname) {
        try {
            this.playerDao.deleteById(lowercaseNickname);
            this.plugin.removePlayerFromCacheLowercased(lowercaseNickname);
            return true;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}