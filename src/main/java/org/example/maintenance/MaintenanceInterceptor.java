package org.example.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MaintenanceInterceptor implements HandlerInterceptor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    SettingsStore settingsStore;

    public MaintenanceInterceptor(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (settingsStore.isMaintenanceModeEnabled()) {
            logger.info("Maintenance mode detected: intercepting request with 503 Service Unavailable response");
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setIntHeader("Retry-After", 60);
            return false;
        }
        return true;
    }
}
