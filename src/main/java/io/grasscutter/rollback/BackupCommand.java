package io.grasscutter.rollback;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClients;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.config.Configuration;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.player.Player;

import java.util.List;

@Command(label = "backup", aliases = {"bk"},
        usage = "/backup [name]", permission = "grasscutter.backup",
        targetRequirement = Command.TargetRequirement.NONE,
        threading = true)
public final class BackupCommand implements CommandHandler {
    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Create a MongoDB client for the database.
        var dbOptions = Configuration.DATABASE;
        var gameClient = MongoClients.create(dbOptions.game.connectionUri);
        var serverClient = MongoClients.create(dbOptions.server.connectionUri);

        // Get the collections from the database.
        var gameDatabase = DatabaseManager.getGameDatabase();
        var serverDatabase = DatabaseManager.getAccountDatastore().getDatabase();

        // Save the databases to the backup.
        var name = (args.size() > 0 ? args.get(0) : "bk") + "_";
        gameDatabase.aggregate(List.of(
                new BasicDBObject("$out", name + gameDatabase.getName())));
        serverDatabase.aggregate(List.of(
                new BasicDBObject("$out", name + serverDatabase.getName())));

        CommandHandler.sendMessage(sender, "Backup executed!");

        // Close the MongoDB clients.
        gameClient.close();
        serverClient.close();
    }
}
