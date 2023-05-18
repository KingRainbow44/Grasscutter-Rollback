package io.grasscutter.rollback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class RollbackFile {
    private final Map<String, Database> databases
            = new HashMap<>();

    @AllArgsConstructor @Getter
    public static class Database {
        private final boolean isGameDatabase;
        private final List<Collection> collections
                = new ArrayList<>();
    }

    @AllArgsConstructor @Getter
    public static class Collection {
        private final String name;

        private final List<Document> documents
                = new ArrayList<>();
    }
}
