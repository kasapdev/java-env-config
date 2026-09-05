# java-env-config

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

### `ConfigException`

An unchecked exception (`RuntimeException`) whose message always names the offending key(s), thrown for: a missing required key, a value that fails to parse as the requested type, a malformed config-file line, or one or more missing keys from `validate()`.

## License

MIT — see [LICENSE](LICENSE).
