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

        // Create the collections.
        var name = (args.size() > 0 ? args.get(0) : "bk") + "_";
        var newGame = gameClient.getDatabase(name + gameDatabase.getName());
        var newServer = serverClient.getDatabase(name + serverDatabase.getName());

        // Drop the databases.
        newGame.drop();
        newServer.drop();

        // Copy all collections from the database to the new database.
        for (var collectionName : gameDatabase.listCollectionNames()) {
            var collection = gameDatabase.getCollection(collectionName);
            var newCollection = newGame.getCollection(collectionName);

            for (var document : collection.find()) {
                newCollection.insertOne(document);
            }
        }

        if (!gameDatabase.getName().equals(serverDatabase.getName())) {
            for (var collectionName : serverDatabase.listCollectionNames()) {
                var collection = serverDatabase.getCollection(collectionName);
                var newCollection = newServer.getCollection(collectionName);

                for (var document : collection.find()) {
                    newCollection.insertOne(document);
                }
            }
        }

        CommandHandler.sendMessage(sender, "Backup executed!");

        // Close the MongoDB clients.
        gameClient.close();
        serverClient.close();
    }
}
