package io.grasscutter.rollback;

import com.mongodb.client.MongoClients;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.config.Configuration;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.player.Player;

import java.util.List;

@Command(label = "rollback", aliases = {"rb"},
        usage = "/rollback [name]", permission = "grasscutter.rollback",
        targetRequirement = Command.TargetRequirement.NONE,
        threading = true)
public final class RollbackCommand implements CommandHandler {
    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Create a MongoDB client for the database.
        var dbOptions = Configuration.DATABASE;
        var gameClient = MongoClients.create(dbOptions.game.connectionUri);
        var serverClient = MongoClients.create(dbOptions.server.connectionUri);

        // Get the collections from the database.
        var gameDatabase = DatabaseManager.getGameDatabase();
        var serverDatabase = DatabaseManager.getAccountDatastore().getDatabase();
        // Get the backup collections from the database.
        var name = (args.size() > 0 ? args.get(0) : "bk") + "_";
        var gameBackup = gameClient.getDatabase(
                name + gameDatabase.getName());
        var serverBackup = serverClient.getDatabase(
                name + serverDatabase.getName());

        // Clear the existing databases.
        gameDatabase.drop();
        serverDatabase.drop();

        // Copy all collections from the backup database to the new database.
        for (var collectionName : gameBackup.listCollectionNames()) {
            var collection = gameBackup.getCollection(collectionName);
            var newCollection = gameDatabase.getCollection(collectionName);

            for (var document : collection.find()) {
                newCollection.insertOne(document);
            }
        }

        if (!gameDatabase.getName().equals(serverDatabase.getName())) {
            for (var collectionName : serverBackup.listCollectionNames()) {
                var collection = serverBackup.getCollection(collectionName);
                var newCollection = serverDatabase.getCollection(collectionName);

                for (var document : collection.find()) {
                    newCollection.insertOne(document);
                }
            }
        }

        CommandHandler.sendMessage(sender, "Rollback executed!");
        Rollback.getPluginLogger().info("Rollback was completed by {}. Server is restarting in 1s.",
                sender == null ? "Console" : sender.getNickname());

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) { }

            System.exit(0);
        }).start();

        // Close the MongoDB clients.
        gameClient.close();
        serverClient.close();
    }
}
