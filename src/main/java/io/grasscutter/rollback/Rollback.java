package io.grasscutter.rollback;

import emu.grasscutter.plugin.Plugin;
import lombok.Getter;
import org.slf4j.Logger;

public final class Rollback extends Plugin {
    @Getter private static Rollback instance;

    /**
     * @return The plugin's logger.
     */
    public static Logger getPluginLogger() {
        return Rollback.getInstance().getLogger();
    }

    @Override
    public void onLoad() {
        instance = this;

        this.getLogger().info("Rollback has been loaded.");
    }

    @Override
    public void onEnable() {
        this.getHandle().registerCommand(new RollbackCommand());
        this.getHandle().registerCommand(new BackupCommand());

        this.getLogger().info("Rollback has been enabled.");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Rollback has been disabled.");
    }
}
