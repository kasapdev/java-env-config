package dev.kasapdev.envconfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A typed configuration loader for {@code .env}-style files.
 *
 * <p>File format:
 * <ul>
 *   <li>{@code KEY=value} pairs, one per line.</li>
 *   <li>Blank lines (or lines containing only whitespace) are skipped.</li>
 *   <li>A line whose first non-whitespace character is {@code #} is treated as a
 *       full-line comment and skipped.</li>
 *   <li>Values may be wrapped in double quotes ({@code KEY="some value"}), which
 *       allows leading/trailing whitespace to be preserved and supports the escape
 *       sequences {@code \"}, {@code \\}, {@code \n} and {@code \t}.</li>
 *   <li>Values may be wrapped in single quotes ({@code KEY='some value'}), which
 *       preserves the content completely literally (no escape processing).</li>
 *   <li>Unquoted values are trimmed of surrounding whitespace.</li>
 * </ul>
 *
 * <p>This class is immutable and safe to share across threads once loaded.
 */
public final class EnvConfig {

    private final Map<String, String> values;

    private EnvConfig(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    /**
     * Parses the {@code .env}-style file at {@code envFilePath} and returns a
     * new {@link EnvConfig} holding its key/value pairs.
     *
     * @throws ConfigException if the file cannot be read or contains a malformed line
     */
    public static EnvConfig load(Path envFilePath) {
        List<String> lines;
        try {
            lines = Files.readAllLines(envFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file: " + envFilePath, e);
        } catch (UncheckedIOException e) {
            throw new ConfigException("Failed to read config file: " + envFilePath, e.getCause());
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String rawLine = lines.get(i);
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }

            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                throw new ConfigException(
                        "Malformed config line " + lineNumber + " (expected KEY=value): \"" + rawLine + "\"");
            }

            String key = trimmed.substring(0, eq).trim();
            if (key.isEmpty()) {
                throw new ConfigException("Malformed config line " + lineNumber + " (empty key): \"" + rawLine + "\"");
            }

            String rawValue = trimmed.substring(eq + 1).trim();
            String value = parseValue(rawValue);
            parsed.put(key, value);
        }

        return new EnvConfig(parsed);
    }

    private static String parseValue(String rawValue) {
        if (rawValue.length() >= 2) {
            char first = rawValue.charAt(0);
            char last = rawValue.charAt(rawValue.length() - 1);
            if (first == '"' && last == '"') {
                return unescapeDoubleQuoted(rawValue.substring(1, rawValue.length() - 1));
            }
            if (first == '\'' && last == '\'') {
                return rawValue.substring(1, rawValue.length() - 1);
            }
        }
        return rawValue;
    }

    private static String unescapeDoubleQuoted(String content) {
        StringBuilder sb = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Returns {@code true} if {@code key} is present in this configuration. */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /** Returns the raw string value for {@code key}. */
    public String getString(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new ConfigException("Missing required config key: \"" + key + "\"");
        }
        return value;
    }

    /** Returns the raw string value for {@code key}, or {@code defaultValue} if absent. */
    public String getString(String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Returns the value for {@code key} parsed as an {@code int}.
     *
     * @throws ConfigException if the key is missing, or present but not a valid integer
     */
    public int getInt(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new ConfigException("Missing required config key: \"" + key + "\"");
        }
        return parseInt(key, value);
    }

    /**
     * Returns the value for {@code key} parsed as an {@code int}, or {@code defaultValue}
     * if the key is absent.
     *
     * @throws ConfigException if the key is present but not a valid integer
     */
    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        return parseInt(key, value);
    }

    private static int parseInt(String key, String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigException(
                    "Config key \"" + key + "\" has value \"" + value + "\" which is not a valid integer", e);
        }
    }

    /**
     * Returns the value for {@code key} parsed as a {@code boolean}. Accepts
     * {@code "true"}/{@code "false"} in any letter case.
     *
     * @throws ConfigException if the key is missing, or present but not "true"/"false"
     */
    public boolean getBoolean(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new ConfigException("Missing required config key: \"" + key + "\"");
        }
        return parseBoolean(key, value);
    }

    /**
     * Returns the value for {@code key} parsed as a {@code boolean}, or {@code defaultValue}
     * if the key is absent.
     *
     * @throws ConfigException if the key is present but not "true"/"false"
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        return parseBoolean(key, value);
    }

    private static boolean parseBoolean(String key, String value) {
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("true")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return false;
        }
        throw new ConfigException(
                "Config key \"" + key + "\" has value \"" + value + "\" which is not a valid boolean (expected true/false)");
    }

    /**
     * Verifies that every key in {@code requiredKeys} is present in this configuration.
     * If one or more are missing, throws a single {@link ConfigException} listing
     * ALL missing keys (not just the first one found).
     */
    public void validate(Set<String> requiredKeys) {
        Set<String> missing = new TreeSet<>();
        for (String key : requiredKeys) {
            if (!values.containsKey(key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new ConfigException("Missing required config key(s): " + String.join(", ", missing));
        }
    }

    /** Returns an immutable view of all raw key/value pairs loaded. */
    public Map<String, String> asMap() {
        return values;
    }

    /** Returns the set of keys present in this configuration. */
    public Set<String> keys() {
        return values.keySet();
    }
}
