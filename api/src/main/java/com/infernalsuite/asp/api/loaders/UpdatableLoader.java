package com.infernalsuite.asp.api.loaders;

import java.io.IOException;

public abstract class UpdatableLoader implements SlimeLoader {

    public abstract void update() throws NewerStorageException, IOException;

    public class NewerStorageException extends Exception {

        private final int implementationVersion;
        private final int storageVersion;


        public NewerStorageException(int implementationVersion, int storageVersion) {
            this.implementationVersion = implementationVersion;
            this.storageVersion = storageVersion;
        }

        public int getImplementationVersion() {
            return implementationVersion;
        }

        public int getStorageVersion() {
            return storageVersion;
        }
    }
}
