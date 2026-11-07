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

package net.elytrium.limboauth.command;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Locale;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.model.SQLRuntimeException;
import net.elytrium.limboauth.service.PasswordService;
import net.elytrium.limboauth.service.PlayerService;
import net.elytrium.limboauth.service.TotpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public class TotpCommand extends RatelimitedCommand {

  private final Dao<RegisteredPlayer, String> playerDao;
  private final PlayerService playerService;

  private final Component notPlayer;
  private final Component usage;
  private final boolean needPassword;
  private final Component notRegistered;
  private final Component wrongPassword;
  private final Component alreadyEnabled;
  private final Component errorOccurred;
  private final Component successful;
  private final Component qr;
  private final String token;
  private final String recovery;
  private final Component disabled;
  private final Component wrong;
  private final Component crackedCommand;

  public TotpCommand(LimboAuth plugin, Dao<RegisteredPlayer, String> playerDao) {
    this.playerDao = playerDao;
    this.playerService = new PlayerService(plugin, playerDao);

    Serializer serializer = LimboAuth.getSerializer();
    this.notPlayer = serializer.deserialize(Settings.IMP.MAIN.STRINGS.NOT_PLAYER);
    this.usage = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_USAGE);
    this.needPassword = Settings.IMP.MAIN.TOTP_NEED_PASSWORD;
    this.notRegistered = serializer.deserialize(Settings.IMP.MAIN.STRINGS.NOT_REGISTERED);
    this.wrongPassword = serializer.deserialize(Settings.IMP.MAIN.STRINGS.WRONG_PASSWORD);
    this.alreadyEnabled = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_ALREADY_ENABLED);
    this.errorOccurred = serializer.deserialize(Settings.IMP.MAIN.STRINGS.ERROR_OCCURRED);
    this.successful = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_SUCCESSFUL);
    this.qr = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_QR);
    this.token = Settings.IMP.MAIN.STRINGS.TOTP_TOKEN;
    this.recovery = Settings.IMP.MAIN.STRINGS.TOTP_RECOVERY;
    this.disabled = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_DISABLED);
    this.wrong = serializer.deserialize(Settings.IMP.MAIN.STRINGS.TOTP_WRONG);
    this.crackedCommand = serializer.deserialize(Settings.IMP.MAIN.STRINGS.CRACKED_COMMAND);
  }

  @Override
  public void execute(CommandSource source, String[] args) {
    if (source instanceof Player) {
      if (args.length == 0) {
        source.sendMessage(this.usage);
      } else {
        String username = ((Player) source).getUsername();
        String usernameLowercase = username.toLowerCase(Locale.ROOT);

        RegisteredPlayer playerInfo;
        if (args[0].equalsIgnoreCase("enable")) {
          if (this.needPassword ? args.length == 2 : args.length == 1) {
            playerInfo = AuthSessionHandler.fetchInfoLowercased(this.playerDao, usernameLowercase);
            if (playerInfo == null) {
              source.sendMessage(this.notRegistered);
              return;
            } else if (playerInfo.getHash().isEmpty()) {
              source.sendMessage(this.crackedCommand);
              return;
            } else if (this.needPassword && !PasswordService.checkPassword(args[1], playerInfo, this.playerDao)) {
              source.sendMessage(this.wrongPassword);
              return;
            }

            if (!playerInfo.getTotpToken().isEmpty()) {
              source.sendMessage(this.alreadyEnabled);
              return;
            }

            String secret = TotpService.generateSecret();
            try {
              this.playerService.setTotpSecret(usernameLowercase, secret);
            } catch (SQLException e) {
              source.sendMessage(this.errorOccurred);
              throw new SQLRuntimeException(e);
            }
            source.sendMessage(this.successful);

            String qrUrl = TotpService.buildQrUrl(username, secret);
            source.sendMessage(this.qr.clickEvent(ClickEvent.openUrl(qrUrl)));

            Serializer serializer = LimboAuth.getSerializer();
            source.sendMessage(serializer.deserialize(MessageFormat.format(this.token, secret))
                    .clickEvent(ClickEvent.copyToClipboard(secret)));
            String codes = TotpService.generateRecoveryCodes();
            source.sendMessage(serializer.deserialize(MessageFormat.format(this.recovery, codes))
                    .clickEvent(ClickEvent.copyToClipboard(codes)));
          } else {
            source.sendMessage(this.usage);
          }
        } else if (args[0].equalsIgnoreCase("disable")) {
          if (args.length == 2) {
            playerInfo = AuthSessionHandler.fetchInfoLowercased(this.playerDao, usernameLowercase);

            if (playerInfo == null) {
              source.sendMessage(this.notRegistered);
              return;
            }

            if (TotpService.isValidCode(playerInfo.getTotpToken(), args[1])) {
              try {
                this.playerService.setTotpSecret(usernameLowercase, "");
                source.sendMessage(this.disabled);
              } catch (SQLException e) {
                source.sendMessage(this.errorOccurred);
                throw new SQLRuntimeException(e);
              }
            } else {
              source.sendMessage(this.wrong);
            }
          } else {
            source.sendMessage(this.usage);
          }
        } else {
          source.sendMessage(this.usage);
        }
      }
    } else {
      source.sendMessage(this.notPlayer);
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return Settings.IMP.MAIN.COMMAND_PERMISSION_STATE.TOTP
            .hasPermission(invocation.source(), "limboauth.commands.totp");
  }
}