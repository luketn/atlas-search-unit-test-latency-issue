package com.mycodefu;

import org.slf4j.Logger;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A base class you can extend in tests to run against an Atlas Local Development Environment.
 */
public abstract class AtlasMongoDBTest {
    private static final Logger log = getLogger(AtlasMongoDBTest.class);

    static MongoDBAtlasLocalContainer mongoDAtlas = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local")
            .withLogConsumer(outputFrame -> log.debug("mongod: {}", outputFrame.getUtf8String()));

    // Singleton pattern Ref: https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
    static {
        try {
            Instant start = Instant.now();

            // Applying patch for MacOS Sequoia bug
            // ref: https://medium.com/@luketn/java-on-docker-sigill-exception-on-mac-os-sequoia-15-2-9311e4775442
            String operatingSystemName = System.getProperty("os.name");
            if (operatingSystemName.toLowerCase().contains("mac")) {
                mongoDAtlas.withEnv("JAVA_TOOL_OPTIONS", "-XX:UseSVE=0");
            }

            log.debug("starting mongod...");
            mongoDAtlas.start();
            log.debug("mongod started, connection string: {}", mongoDAtlas.getConnectionString());

            //Check logs
            if (log.isTraceEnabled()) {
                try {
                    Files.writeString(Paths.get("./mongod-init-log.txt"), mongoDAtlas.getLogs());
                } catch (IOException e) {
                    log.trace("An error occurred while writing mongod and mongot logs", e);
                }
            }

            log.debug("mongod and mongot started in {} seconds", Duration.between(start, Instant.now()).getSeconds());
        } catch (Exception e) {
            log.warn("An error occurred while starting mongod and mongot", e);
        }
    }

    protected String connectionString() {
        return mongoDAtlas.getConnectionString();
    }
}
