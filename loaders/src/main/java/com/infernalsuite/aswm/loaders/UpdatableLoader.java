package com.infernalsuite.aswm.loaders;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;

import java.io.IOException;

public abstract class UpdatableLoader implements SlimeLoader {

    public abstract void update() throws NewerDatabaseException, IOException;

    public class NewerDatabaseException extends Exception {

        private final int currentVersion;
        private final int databaseVersion;


        public NewerDatabaseException(int currentVersion, int databaseVersion) {
            this.currentVersion = currentVersion;
            this.databaseVersion = databaseVersion;
        }

        public int getCurrentVersion() {
            return currentVersion;
        }

        public int getDatabaseVersion() {
            return databaseVersion;
        }
    }
}
