package org.example.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MigrationData {
    private int version;
    private String description;
    private long migratedAt;

    public static MigrationData of(BaseMigration instance) {
        return new MigrationData(instance.getVersion(), instance.getDescription(), 0);
    }
}