package io.grasscutter.rollback;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import emu.grasscutter.plugin.Plugin;
import lombok.Getter;
import org.bson.Document;
import org.slf4j.Logger;

import java.io.IOException;

public final class Rollback extends Plugin {
    @Getter private static final Gson gson
            = new GsonBuilder()
            .registerTypeAdapter(Document.class, new DocumentAdapter())
            .create();
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
        this.getHandle().registerCommand(new FileCommand());

        this.getLogger().info("Rollback has been enabled.");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Rollback has been disabled.");
    }

    /** JSON document adapter. */
    static class DocumentAdapter extends TypeAdapter<Document> {
        @Override
        public void write(JsonWriter jsonWriter, Document document) throws IOException {
            jsonWriter.value(document.toJson());
        }

        @Override
        public Document read(JsonReader jsonReader) throws IOException {
            return Document.parse(jsonReader.nextString());
        }
    }
}
