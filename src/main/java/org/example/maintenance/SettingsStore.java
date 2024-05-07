package org.example.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SettingsStore {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean maintenanceModeEnabled = true;

    public void enableMaintenanceMode() {
        maintenanceModeEnabled = true;
        logger.info("Maintenance mode enabled");
    }

    public void disableMaintenanceMode() {
        maintenanceModeEnabled = false;
        logger.info("Maintenance mode disabled");
    }

    public boolean isMaintenanceModeEnabled() {
        return maintenanceModeEnabled;
    }
}
