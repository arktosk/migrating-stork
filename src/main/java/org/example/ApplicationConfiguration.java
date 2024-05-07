package org.example;

import org.example.maintenance.MaintenanceInterceptor;
import org.example.maintenance.SettingsStore;
import org.example.migration.MigrationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApplicationConfiguration implements WebMvcConfigurer {

    @Autowired
    private SettingsStore settingsStore;

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new MaintenanceInterceptor(settingsStore));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReadyEventCallback() {
        settingsStore.enableMaintenanceMode();
        MigrationRunner.runInContext(applicationContext);
        settingsStore.disableMaintenanceMode();
    }
}