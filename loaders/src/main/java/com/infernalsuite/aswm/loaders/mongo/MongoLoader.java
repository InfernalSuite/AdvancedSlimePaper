package com.infernalsuite.aswm.loaders.mongo;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.loaders.UpdatableLoader;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MongoLoader extends UpdatableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoLoader.class);

    private final MongoClient client;
    private final String database;
    private final String collection;

    public MongoLoader(String database, String collection, @Nullable String username, @Nullable String password,
                       @Nullable String authSource, @Nullable String host, @Nullable Integer port, @Nullable String uri) throws MongoException {
        this.database = database;
        this.collection = collection;

        String authParams = username != null && password != null ? username + ":" + password + "@" : "";
        String parsedAuthSource = authSource != null ? "/?authSource=" + authSource : "";
        String parsedUri = uri != null ? uri : "mongodb://" + authParams + host + ":" + port + parsedAuthSource;

        this.client = MongoClients.create(parsedUri);

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
                LOGGER.info("Updating MongoDB database...");

                mongoDatabase.getCollection(collection + "_files.files").renameCollection(new MongoNamespace(database, collection + ".files"));
                mongoDatabase.getCollection(collection + "_files.chunks").renameCollection(new MongoNamespace(database, collection + ".chunks"));

                LOGGER.info("MongoDB database updated!");
                break;
            }
        }

        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        // Old world lock importing
        try (MongoCursor<Document> documents = mongoCollection.find(Filters.or(Filters.eq("locked", true),
                Filters.eq("locked", false))).cursor()) {
            if (documents.hasNext()) {
                LOGGER.warn("Your SWM MongoDB database is outdated. The update process will start in 10 seconds.");
                LOGGER.warn("Note that this update will make your database incompatible with older SWM versions.");
                LOGGER.warn("Make sure no other servers with older SWM versions are using this database.");
                LOGGER.warn("Shut down the server to prevent your database from being updated.");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    LOGGER.info("Update process aborted.");
                    return;
                }

                while (documents.hasNext()) {
                    String name = documents.next().getString("name");
                    mongoCollection.updateOne(Filters.eq("name", name), Updates.set("locked", 0L));
                }
            }
        }
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
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
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
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
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
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

}
