package com.grinderwolf.swm.plugin.loaders.mongo;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.loaders.UpdatableLoader;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;

public class MongoLoader extends UpdatableLoader {

    // World locking executor service
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("SWM MongoDB Lock Pool Thread #%1$d").build());

    private final Map<String, ScheduledFuture<?>> lockedWorlds = new HashMap<>();

    private final MongoClient client;
    private final String database;
    private final String collection;

    public MongoLoader(DatasourcesConfig.MongoDBConfig config) throws MongoException {
        this.database = config.getDatabase();
        this.collection = config.getCollection();

        String authParams = !config.getUsername().isEmpty() && !config.getPassword().isEmpty() ? config.getUsername() + ":" + config.getPassword() + "@" : "";
        String authSource = !config.getAuthSource().isEmpty() ? "/?authSource=" + config.getAuthSource() : "";
        String uri = !config.getUri().isEmpty() ? config.getUri() : "mongodb://" + authParams + config.getHost() + ":" + config.getPort() + authSource;

        this.client = MongoClients.create(uri);

        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        mongoCollection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    @Override
    public void update() {
        MongoDatabase mongoDatabase = client.getDatabase(database);

        // Old GridFS importing
        for (String collectionName : mongoDatabase.listCollectionNames()) {
            if (collectionName.equals(collection + "_files.files") || collectionName.equals(collection + "_files.chunks")) {
                Logging.info("Updating MongoDB database...");

                mongoDatabase.getCollection(collection + "_files.files").renameCollection(new MongoNamespace(database, collection + ".files"));
                mongoDatabase.getCollection(collection + "_files.chunks").renameCollection(new MongoNamespace(database, collection + ".chunks"));

                Logging.info("MongoDB database updated!");

                break;
            }
        }

        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        // Old world lock importing
        try (MongoCursor<Document> documents = mongoCollection.find(Filters.or(Filters.eq("locked", true),
                Filters.eq("locked", false))).cursor()) {
            if (documents.hasNext()) {
                Logging.warning("Your SWM MongoDB database is outdated. The update process will start in 10 seconds.");
                Logging.warning("Note that this update will make your database incompatible with older SWM versions.");
                Logging.warning("Make sure no other servers with older SWM versions are using this database.");
                Logging.warning("Shut down the server to prevent your database from being updated.");

                Bukkit.getScheduler().scheduleSyncDelayedTask(SWMPlugin.getInstance(), () -> {
                    while (documents.hasNext()) {
                        String name = documents.next().getString("name");
                        mongoCollection.updateOne(Filters.eq("name", name), Updates.set("locked", 0L));
                    }
                }, 200L);
            }
        }
    }

    @Override
    public byte[] loadWorld(String worldName) throws UnknownWorldException, IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldName);
            }

            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bucket.downloadToStream(worldName, stream);

            return stream.toByteArray();
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    private void updateLock(String worldName, boolean forceSchedule) {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            mongoCollection.updateOne(Filters.eq("name", worldName), Updates.set("locked", System.currentTimeMillis()));
        } catch (MongoException ex) {
            Logging.error("Failed to update the lock for world " + worldName + ":");
            ex.printStackTrace();
        }

        if (forceSchedule || lockedWorlds.containsKey(worldName)) { // Only schedule another update if the world is still on the map
            lockedWorlds.put(worldName, SERVICE.schedule(() -> updateLock(worldName, false), LoaderUtils.LOCK_INTERVAL, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            return worldDoc != null;
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            try (MongoCursor<Document> documents = mongoCollection.find().cursor()) {
                while (documents.hasNext()) {
                    worldList.add(documents.next().getString("name"));
                }
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean releaseLock) throws IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            GridFSFile oldFile = bucket.find(Filters.eq("filename", worldName)).first();

            bucket.uploadFromStream(worldName, new ByteArrayInputStream(serializedWorld));

            if (oldFile != null) {
                bucket.delete(oldFile.getObjectId());
            }

            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Bson query = Filters.eq("name", worldName);
            mongoCollection.updateOne(
                    query,
                    new Document().append("$set", query),
                    new UpdateOptions().upsert(true)
            );
            if (releaseLock) {
                try {
                    unlockWorld(worldName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        ScheduledFuture<?> future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            GridFSFile file = bucket.find(Filters.eq("filename", worldName)).first();

            if (file == null) {
                throw new UnknownWorldException(worldName);
            }

            bucket.delete(file.getObjectId());

            // Delete backup file
            for (GridFSFile backupFile : bucket.find(Filters.eq("filename", worldName + "_backup"))) {
                bucket.delete(backupFile.getObjectId());
            }

            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            mongoCollection.deleteOne(Filters.eq("name", worldName));
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void acquireLock(String worldName) throws UnknownWorldException, WorldLockedException, IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldName);
            }

            // Only check lock if its there
            if (worldDoc.containsKey("locked")) {
                long lockedMillis = worldDoc.getLong("locked");

                if (System.currentTimeMillis() - lockedMillis <= LoaderUtils.MAX_LOCK_TIME) {
                    throw new WorldLockedException(worldName);
                }
            }

            updateLock(worldName, true);
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException {
        if (lockedWorlds.containsKey(worldName)) {
            return true;
        }

        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldName);
            }

            if (!worldDoc.containsKey("locked")) {
                return false;
            }

            return System.currentTimeMillis() - worldDoc.getLong("locked") <= LoaderUtils.MAX_LOCK_TIME;
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void unlockWorld(String worldName) throws IOException, UnknownWorldException {
        ScheduledFuture<?> future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            UpdateResult result = mongoCollection.updateOne(Filters.eq("name", worldName), Updates.set("locked", 0L));

            if (result.getMatchedCount() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

}
