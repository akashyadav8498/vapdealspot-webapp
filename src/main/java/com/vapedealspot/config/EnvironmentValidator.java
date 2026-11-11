package com.vapedealspot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Strict Environment & Profile Validator.
 * Enforces Milestone 21 rules:
 * 1. Exactly three permitted environments: dev, staging, prod.
 * 2. Safe early startup failure on invalid or obsolete profiles (e.g., uat, test).
 * 3. Prevention of cross-environment database contamination.
 * 4. Fails fast if mandatory Staging or Production credentials are missing.
 */
@Component
public class EnvironmentValidator implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);
    private static final Set<String> ALLOWED_PROFILES = Set.of("dev", "staging", "prod");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            log.info("No active Spring profile explicitly specified. Defaulting to 'dev'.");
            return;
        }

        // 1. Verify that all active profiles belong to the permitted set (dev, staging, prod)
        for (String profile : activeProfiles) {
            String lower = profile.toLowerCase().trim();
            if (!ALLOWED_PROFILES.contains(lower)) {
                String error = String.format(
                        "INVALID ENVIRONMENT PROFILE: '%s'. Only 'dev', 'staging', and 'prod' profiles are permitted. " +
                        "UAT and test profiles have been permanently removed.", profile);
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        // 2. Validate environment-specific rules
        boolean isProd = Arrays.stream(activeProfiles).anyMatch("prod"::equalsIgnoreCase);
        boolean isStaging = Arrays.stream(activeProfiles).anyMatch("staging"::equalsIgnoreCase);
        boolean isDev = Arrays.stream(activeProfiles).anyMatch("dev"::equalsIgnoreCase);

        if (isProd || isStaging) {
            String targetEnv = isProd ? "PRODUCTION" : "STAGING";
            String dbHost = environment.getProperty("DB_HOST");
            String dbUser = environment.getProperty("DB_USERNAME");

            if (dbHost == null || dbHost.isBlank() || dbHost.startsWith("${")) {
                String error = String.format(
                        "%s STARTUP FAILED: Mandatory environment variable 'DB_HOST' is not set. " +
                        "Cannot connect to unconfigured database.", targetEnv);
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (dbUser == null || dbUser.isBlank() || dbUser.startsWith("${")) {
                String error = String.format(
                        "%s STARTUP FAILED: Mandatory environment variable 'DB_USERNAME' is not set. " +
                        "Least-privilege credentials required.", targetEnv);
                log.error(error);
                throw new IllegalStateException(error);
            }

            String dbName = environment.getProperty("DB_NAME", "");
            if (isProd && dbName.equalsIgnoreCase("vapedealspot")) {
                String error = "PRODUCTION SAFETY VIOLATION: Production profile cannot point to development database 'vapedealspot'!";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (isStaging && dbName.equalsIgnoreCase("vapedealspot_prod")) {
                String error = "STAGING SAFETY VIOLATION: Staging profile cannot point to production database 'vapedealspot_prod'!";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        if (isDev) {
            String dbName = environment.getProperty("DB_NAME", "");
            if (dbName.equalsIgnoreCase("vapedealspot_prod")) {
                String error = "DEVELOPMENT SAFETY VIOLATION: Development profile cannot point to production database 'vapedealspot_prod'!";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        log.info("Active environment validated successfully: {}", Arrays.toString(activeProfiles));
    }
}
