package com.codify.universaltracker.common.config;

// Jackson 3 (Spring Boot 4) handles Java time serialization via its own auto-configuration.
// Date format is configured via application.yaml (spwhatring.jackson.*).
// No manual ObjectMapper bean needed — Spring Boot 4 auto-configures it correctly.
