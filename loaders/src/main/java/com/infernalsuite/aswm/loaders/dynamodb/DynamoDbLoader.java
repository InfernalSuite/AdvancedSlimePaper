package com.infernalsuite.aswm.loaders.dynamodb;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.loaders.dynamodb.beans.WorldBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.io.IOException;
import java.util.List;

public class DynamoDbLoader implements SlimeLoader {
    private final DynamoDbTable<WorldBean> worldTable;

    public DynamoDbLoader(String worldTableName, String awsRegion, String awsAccessKeyId, String awsSecretAccessKey) {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder().region(Region.of(awsRegion));
        // If awsAccessKeyId and awsSecretAccessKey are empty, client uses default credentials (environment)
        if (!awsAccessKeyId.isEmpty() || !awsSecretAccessKey.isEmpty()) {
            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
            dynamoDbClientBuilder = dynamoDbClientBuilder.credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials));
        }
        DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClientBuilder.build())
                .build();
        this.worldTable = dynamoDbEnhancedClient.table(worldTableName, TableSchema.fromBean(WorldBean.class));
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
        try {
            WorldBean worldBean = worldTable.getItem(req -> req.key(key -> key.partitionValue(worldName)));
            if (worldBean == null) {
                throw new UnknownWorldException(worldName);
            }
            return worldBean.getData();
        } catch (AwsServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try {
            return worldTable.getItem(req -> req.key(key -> key.partitionValue(worldName))) == null;
        } catch (AwsServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        try {
            return worldTable.scan()
                    .items()
                    .stream()
                    .map(WorldBean::getName)
                    .toList();
        } catch (AwsServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        try {
            worldTable.putItem(new WorldBean(serializedWorld, worldName));
        } catch (AwsServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        try {
            if (worldTable.deleteItem(req -> req.key(key -> key.partitionValue(worldName))) == null) {
                throw new UnknownWorldException(worldName);
            }
        } catch (AwsServiceException e) {
            throw new IOException(e);
        }
    }
}
