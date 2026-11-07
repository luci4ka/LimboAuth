/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * ... (лицензия как была)
 */

package net.elytrium.limboauth.service;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.Settings;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SessionTimerService {

    private final LimboAuth plugin;

    @Nullable
    private ScheduledTask task;

    @Nullable
    private BossBar bossBar;

    private long joinTime;

    public SessionTimerService(LimboAuth plugin) {
        this.plugin = plugin;
    }

    /**
     * Запускает таймер. Возвращает true, если боссбар был создан и его нужно показать игроку.
     */
    public boolean start() {
        this.joinTime = System.currentTimeMillis();

        boolean bossBarEnabled = (!Settings.IMP.MAIN.MOD.ENABLED
                || !Settings.IMP.MAIN.MOD.LOGIN_ONLY_BY_MOD)
                && Settings.IMP.MAIN.ENABLE_BOSSBAR;

        int authTime = Settings.IMP.MAIN.AUTH_TIME;
        float multiplier = 1000.0F / authTime;
        Serializer serializer = LimboAuth.getSerializer();

        if (bossBarEnabled) {
            this.bossBar = BossBar.bossBar(
                    Component.empty(),
                    1.0F,
                    Settings.IMP.MAIN.BOSSBAR_COLOR,
                    Settings.IMP.MAIN.BOSSBAR_OVERLAY
            );
        }

        this.task = this.plugin.getServer().getScheduler()
                .buildTask(this.plugin, () -> {
                    long elapsed = System.currentTimeMillis() - this.joinTime;
                    if (elapsed > authTime) {
                        return;
                    }
                    if (bossBarEnabled && this.bossBar != null) {
                        float secondsLeft = (authTime - elapsed) / 1000.0F;
                        this.bossBar.name(serializer.deserialize(
                                MessageFormat.format(Settings.IMP.MAIN.STRINGS.BOSSBAR, (int) secondsLeft)));
                        this.bossBar.progress(Math.min(1.0F, secondsLeft * multiplier));
                    }
                })
                .delay(0, TimeUnit.SECONDS)
                .repeat(1, TimeUnit.SECONDS)
                .schedule();

        return bossBarEnabled && this.bossBar != null;
    }

    @Nullable
    public BossBar getBossBar() {
        return this.bossBar;
    }

    public void stop(Player player) {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        if (this.bossBar != null) {
            player.hideBossBar(this.bossBar);
            this.bossBar = null;
        }
    }
}