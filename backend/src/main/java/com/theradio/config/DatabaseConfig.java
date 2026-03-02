package com.theradio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    @Bean
    public DataSource dataSource() {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            log.info("DATABASE_URL is empty, relying on internal Spring configuration");
            return DataSourceBuilder.create().build();
        }

        if (databaseUrl.startsWith("postgres://")) {
            log.info("Parsing DATABASE_URL for Postgres managed service");
            try {
                URI dbUri = new URI(databaseUrl);
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
                
                // Add sslmode=require for Render if not present in path
                if (!dbUrl.contains("?")) {
                    dbUrl += "?sslmode=require";
                }

                return DataSourceBuilder.create()
                        .url(dbUrl)
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();
            } catch (URISyntaxException e) {
                log.error("Failed to parse DATABASE_URL: {}", databaseUrl, e);
                throw new RuntimeException("Invalid DATABASE_URL config", e);
            }
        }

        log.info("DATABASE_URL does not start with postgres://, using as-is: {}", databaseUrl);
        return DataSourceBuilder.create()
                .url(databaseUrl)
                .build();
    }
}
