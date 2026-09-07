# java-env-config

[![CI](https://github.com/kasapdev/java-env-config/actions/workflows/ci.yml/badge.svg)](https://github.com/kasapdev/java-env-config/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) ![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)

A small, typed configuration loader for `.env`-style files in Java. Hand-written parser (no external dependency), typed accessors (`String`/`int`/`boolean`) with clear, key-naming errors, and an aggregated `validate()` for required keys. Zero dependencies, pure Java 17, no build tool required.

## Build & Run

```bash
cd java-env-config

# Compile the library
javac -d out $(find src/main/java -name "*.java")

# Compile the tests against the compiled library
javac -cp out -d out $(find src/test/java -name "*.java")

# Run the test suite
java -cp out dev.kasapdev.envconfig.EnvConfigTest
```

All test output lines are prefixed `[PASS]` or `[FAIL]`, ending with a summary line and a non-zero exit code if anything failed.

## Usage

Given a `.env` file:

```env
# Application settings
APP_NAME=MyApp
APP_PORT=8080
DEBUG=true
GREETING="Hello,\nworld!"
RAW_TEMPLATE='no {escapes} here \n'
```

```java
import dev.kasapdev.envconfig.ConfigException;
import dev.kasapdev.envconfig.EnvConfig;

import java.nio.file.Path;
import java.util.Set;

public class Example {
    public static void main(String[] args) {
        EnvConfig config = EnvConfig.load(Path.of(".env"));

        String appName = config.getString("APP_NAME");          // "MyApp"
        int port = config.getInt("APP_PORT");                    // 8080
        boolean debug = config.getBoolean("DEBUG");               // true
        String timeout = config.getString("TIMEOUT_MS", "5000"); // default used if absent

        try {
            config.validate(Set.of("APP_NAME", "APP_PORT", "DATABASE_URL"));
        } catch (ConfigException e) {
            System.err.println("Startup aborted: " + e.getMessage());
            // e.g. "Missing required config key(s): DATABASE_URL"
        }
    }
}
```

A more realistic startup sequence: load, validate everything the app needs up front, then read
typed values with sensible defaults for the optional ones.

```java
import dev.kasapdev.envconfig.ConfigException;
import dev.kasapdev.envconfig.EnvConfig;

import java.nio.file.Path;
import java.util.Set;

public class ServerBootstrap {
    public static void main(String[] args) {
        EnvConfig config = EnvConfig.load(Path.of(".env"));

        config.validate(Set.of("APP_NAME", "APP_PORT", "DATABASE_URL"));

        String appName = config.getString("APP_NAME");
        int port = config.getInt("APP_PORT");
        boolean debug = config.getBoolean("DEBUG", false);
        int workerThreads = config.getInt("WORKER_THREADS", Runtime.getRuntime().availableProcessors());

        System.out.printf("Starting %s on port %d (debug=%s, workers=%d)%n",
                appName, port, debug, workerThreads);
    }
}
```

## Config Sections

Real `.env` files often group related settings behind a shared prefix, e.g.
`DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME` alongside `CACHE_TTL` / `CACHE_SIZE`.
`EnvConfig.section(String prefix)` returns an `EnvConfigSection` scoped to keys under
`prefix + "_"`, exposing the same typed accessors with the prefix stripped:

```env
DATABASE_HOST=db.example.com
DATABASE_PORT=5432
DATABASE_NAME=orders
CACHE_TTL=60
CACHE_SIZE=1024
```

```java
import dev.kasapdev.envconfig.EnvConfig;
import dev.kasapdev.envconfig.EnvConfigSection;

import java.nio.file.Path;

public class SectionExample {
    public static void main(String[] args) {
        EnvConfig config = EnvConfig.load(Path.of(".env"));

        EnvConfigSection database = config.section("DATABASE");
        String host = database.getString("HOST"); // "db.example.com" (from DATABASE_HOST)
        int port = database.getInt("PORT");        // 5432               (from DATABASE_PORT)

        EnvConfigSection cache = config.section("CACHE");
        int ttlSeconds = cache.getInt("TTL");       // 60                 (from CACHE_TTL)

        // A prefix that matches nothing is not an error - it's simply an empty section.
        EnvConfigSection unused = config.section("NONEXISTENT");
        int fallback = unused.getInt("ANY_KEY", -1); // -1, same missing-key behavior as EnvConfig
    }
}
```

Each accessor on `EnvConfigSection` delegates to the underlying `EnvConfig`'s own lookup and
type-conversion logic (using the fully-qualified, prefixed key), so error messages, default-value
behavior, and type coercion are identical to calling the equivalent method on `EnvConfig` directly.

## API

### File format

- `KEY=value` pairs, one per line.
- Blank (or whitespace-only) lines are skipped.
- A line whose first non-whitespace character is `#` is a full-line comment and is skipped.
- Double-quoted values (`KEY="..."`) support the escape sequences `\"`, `\\`, `\n`, `\t`.
- Single-quoted values (`KEY='...'`) are kept completely literal (no escape processing).
- Unquoted values have leading/trailing whitespace trimmed.

### `EnvConfig`

| Method | Description |
| --- | --- |
| `static EnvConfig load(Path envFilePath)` | Parses the given file. Throws `ConfigException` on I/O failure or a malformed line. |
| `boolean has(String key)` | Whether `key` is present. |
| `String getString(String key)` | Returns the raw value, or throws `ConfigException` naming the key if missing. |
| `String getString(String key, String defaultValue)` | Returns the raw value, or `defaultValue` if missing. |
| `int getInt(String key)` | Parses the value as `int`. Throws `ConfigException` naming the key if missing or not a valid integer. |
| `int getInt(String key, int defaultValue)` | Same, but returns `defaultValue` if the key is missing (still throws if present-but-invalid). |
| `boolean getBoolean(String key)` | Parses `"true"`/`"false"` (case-insensitive). Throws `ConfigException` naming the key if missing or invalid. |
| `boolean getBoolean(String key, boolean defaultValue)` | Same, but returns `defaultValue` if the key is missing (still throws if present-but-invalid). |
| `void validate(Set<String> requiredKeys)` | Throws a single `ConfigException` listing **all** missing required keys, or does nothing if all are present. |
| `Map<String, String> asMap()` | Immutable view of every raw key/value pair loaded. |
| `Set<String> keys()` | The set of keys present. |
| `EnvConfigSection section(String prefix)` | Returns a view scoped to keys under `prefix + "_"` (see [Config Sections](#config-sections)). Never throws, even for an unused prefix. |

### `EnvConfigSection`

Returned by `EnvConfig.section(String prefix)`. Exposes the same typed accessors as
`EnvConfig`, resolving each key by prepending the section's prefix and delegating to
the underlying `EnvConfig`, so behavior (errors, defaults, type coercion) is identical.

| Method | Description |
| --- | --- |
| `boolean has(String key)` | Whether `key` is present within this section. |
| `String getString(String key)` | Returns the raw value, or throws `ConfigException` naming the fully-qualified key if missing. |
| `String getString(String key, String defaultValue)` | Returns the raw value, or `defaultValue` if missing. |
| `int getInt(String key)` | Parses the value as `int`. Throws `ConfigException` if missing or not a valid integer. |
| `int getInt(String key, int defaultValue)` | Same, but returns `defaultValue` if the key is missing. |
| `boolean getBoolean(String key)` | Parses `"true"`/`"false"` (case-insensitive). Throws `ConfigException` if missing or invalid. |
| `boolean getBoolean(String key, boolean defaultValue)` | Same, but returns `defaultValue` if the key is missing. |
| `Map<String, String> asMap()` | Immutable view of this section's key/value pairs, with the prefix stripped. |
| `Set<String> keys()` | The set of keys present in this section, with the prefix stripped. |

### `ConfigException`

An unchecked exception (`RuntimeException`) whose message always names the offending key(s), thrown for: a missing required key, a value that fails to parse as the requested type, a malformed config-file line, or one or more missing keys from `validate()`.

## License

MIT — see [LICENSE](LICENSE).
