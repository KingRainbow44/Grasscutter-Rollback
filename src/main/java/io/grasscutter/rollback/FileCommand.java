package io.grasscutter.rollback;

import com.mongodb.client.MongoClients;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.config.Configuration;
import emu.grasscutter.database.DatabaseManager;
import emu.grasscutter.game.player.Player;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Command(label = "file", aliases = {"fi", "import"},
        usage = "/file [name]", permission = "grasscutter.fileimport",
        targetRequirement = Command.TargetRequirement.NONE,
        threading = true)
public final class FileCommand implements CommandHandler {
    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Check if a file was specified.
        if (args.size() < 1) {
            CommandHandler.sendMessage(sender, "You must specify a file name.");
            return;
        }

        // Create a MongoDB client for the database.
        var dbOptions = Configuration.DATABASE;
        var gameClient = MongoClients.create(dbOptions.game.connectionUri);
        var serverClient = MongoClients.create(dbOptions.server.connectionUri);

        // Try to find the specified file.
        var file = new File(Rollback.getInstance()
                .getDataFolder(), args.get(0));
        // Perform the action depending on if the file exists.
        if (file.exists()) try (var reader = new FileReader(file)) {
            // Add collections from the file to the database.
            var dump = Rollback.getGson().fromJson(reader, RollbackFile.class);
            for (var databaseEntry : dump.getDatabases().entrySet()) {
                var databaseName = databaseEntry.getKey();
                var database = databaseEntry.getValue();

                // Resolve the target database.
                var targetDatabase = database.isGameDatabase() ?
                        gameClient.getDatabase(databaseName) :
                        serverClient.getDatabase(databaseName);

                // Drop the database.
                targetDatabase.drop();

                // Add collections from the file to the database.
                for (var collection : database.getCollections()) {
                    var targetCollection = targetDatabase.getCollection(collection.getName());

                    // Add documents from the file to the collection.
                    for (var document : collection.getDocuments()) {
                        targetCollection.insertOne(document);
                    }
                }
            }

            CommandHandler.sendMessage(sender, "Imported the database from the file.");
        } catch (IOException ignored) {

        } else {
            // Get the collections from the database.
            var gameDatabase = DatabaseManager.getGameDatabase();
            var serverDatabase = DatabaseManager.getAccountDatastore().getDatabase();

            // Dump collections from the database to the file.
            var dump = new RollbackFile();

            // Dump the game database.
            var gameDump = new RollbackFile.Database(true);
            for (var collectionName : gameDatabase.listCollectionNames()) {
                var collection = gameDatabase.getCollection(collectionName);
                var dumpCollection = new RollbackFile.Collection(collectionName);

                // Add all documents from the collection to the dump.
                for (var document : collection.find()) {
                    dumpCollection.getDocuments().add(document);
                }

                gameDump.getCollections().add(dumpCollection);
            }
            dump.getDatabases().put(gameDatabase.getName(), gameDump);

            // Dump the server database if it is different from the game database.
            if (!gameDatabase.getName().equals(serverDatabase.getName())) {
                var serverDump = new RollbackFile.Database(false);
                for (var collectionName : serverDatabase.listCollectionNames()) {
                    var collection = serverDatabase.getCollection(collectionName);
                    var dumpCollection = new RollbackFile.Collection(collectionName);

                    // Add all documents from the collection to the dump.
                    for (var document : collection.find()) {
                        dumpCollection.getDocuments().add(document);
                    }

                    serverDump.getCollections().add(dumpCollection);
                }
                dump.getDatabases().put(serverDatabase.getName(), serverDump);
            }

            // Write the dump to the file.
            try (var writer = new java.io.FileWriter(file)) {
                Rollback.getGson().toJson(dump, writer);
            } catch (IOException ignored) {

            }

            CommandHandler.sendMessage(sender, "Dumped the database to the file.");
        }

        // Close the MongoDB clients.
        gameClient.close();
        serverClient.close();
    }
}
