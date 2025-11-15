# Nishi Utils Spring

[![Maven](https://img.shields.io/badge/Maven-v1.0.1-blue)](https://github.com/nishisan-dev/nishi-utils-spring)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)](https://spring.io/projects/spring-boot)

Spring Boot auto-configuration library that integrates [nishi-utils](https://github.com/nishisan-dev/nishisan-utils) statistics functionality with Spring Boot applications and Micrometer metrics.

## Overview

Nishi Utils Spring provides seamless integration between the nishi-utils statistics library and Spring Boot applications. It automatically configures the `StatsUtils` component and optionally binds it to Micrometer's `MeterRegistry` for metrics export to monitoring systems like Prometheus, Grafana, and others.

## Features

- **Auto-configuration** for `StatsUtils` in Spring Boot applications
- **Micrometer integration** for metrics export (optional)
- **Strong reference management** for metrics to prevent garbage collection
- **Conditional activation** via application properties
- **Support for multiple metric types**: counters, gauges, and averages
- **Servlet-only activation** for web applications

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.nishisan</groupId>
    <artifactId>nishi-utils-spring</artifactId>
    <version>1.0.1</version>
</dependency>
```

Configure the GitHub Packages repository:

```xml
<repositories>
    <repository>
        <id>github-utils</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/nishisan-dev/nishi-utils-spring</url>
    </repository>
</repositories>
```

## Configuration

### Enabling Statistics

Add to your `application.properties` or `application.yml`:

```properties
# Enable nishi-utils statistics
nishi.utils.stats.enabled=true
```

The auto-configuration will only activate when:
- The application is a Servlet web application
- The property `nishi.utils.stats.enabled` is set to `true`

### With Micrometer (Recommended)

If `spring-boot-starter-actuator` is present and `MeterRegistry` is available, metrics will be automatically exported:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Without Micrometer

If Micrometer is not available, `StatsUtils` will still be configured but without metrics export functionality.

## Usage

### Injecting StatsUtils

```java
import dev.nishisan.utils.stats.StatsUtils;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    
    private final StatsUtils statsUtils;
    
    public MyService(StatsUtils statsUtils) {
        this.statsUtils = statsUtils;
    }
    
    public void processRequest() {
        // Increment a hit counter
        statsUtils.notifyHitCounter("requests.processed");
        
        // Track a current value
        statsUtils.notifyCurrentValue("active.users", 42);
        
        // Track average values
        statsUtils.notifyAverageCounter("response.time", 250L);
    }
}
```

### Metric Types

#### Hit Counter
Tracks the number of occurrences and calculates rates:

```java
statsUtils.notifyHitCounter("api.calls");
```

Exports two metrics:
- `api.calls` (counter) - total count
- `api.calls.rate` (gauge) - rate per second

Retrieve values programmatically:
```java
Long count = statsUtils.getCounterValue("api.calls");
Double rate = statsUtils.getCounterRate("api.calls");
```

#### Current Value Counter
Tracks a single current value:

```java
statsUtils.notifyCurrentValue("memory.used", 1024);
// or with Integer
statsUtils.notifyCurrentValue("active.connections", 42);
```

Exports:
- `memory.used` (gauge) - current value

#### Average Counter
Tracks average values over a fixed-size window (default size: 10):

```java
statsUtils.notifyAverageCounter("request.duration", 250L);
// or with Integer
statsUtils.notifyAverageCounter("request.duration", 125);
```

Exports:
- `request.duration` (gauge) - average value

Retrieve the average programmatically:
```java
Double average = statsUtils.getAverage("request.duration");
```

## Architecture

### Components

- **`NishisanStatsAutoConfiguration`**: Spring Boot auto-configuration class that creates and configures the `StatsUtils` bean
- **`StatsUtilsMetricBind`**: Listener that binds `StatsUtils` events to Micrometer metrics with strong references

### Strong Reference Management

The library maintains strong references to all created Gauge instances to prevent garbage collection. This ensures metrics remain active throughout the application lifecycle.

## Requirements

- Java 21 or higher
- Spring Boot 3.5.6 or higher
- Maven 3.6+

## Dependencies

- `dev.nishisan:nishi-utils:1.0.6` - Core statistics library
- `spring-boot-autoconfigure` (provided)
- `spring-boot-starter-actuator` (optional, for metrics)

## License

This project is part of the Nishisan Utils ecosystem.

## Contributing

Contributions are welcome! Please ensure all changes maintain backward compatibility and include appropriate tests.

## Related Projects

- [nishisan-utils](https://github.com/nishisan-dev/nishisan-utils) - Core utilities library
