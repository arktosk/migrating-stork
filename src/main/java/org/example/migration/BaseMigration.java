package org.example.migration;

public abstract class BaseMigration {

    public Integer getVersion() {
        return Integer.parseInt(getClass().getSimpleName().substring(1).split("__")[0]);
    }

    public String getDescription() {
        return getClass().getSimpleName().substring(1).split("__")[1];
    }

    public abstract void migrate() throws MigrationException;


}
