package dev.kasapdev.envconfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A prefix-scoped view of an {@link EnvConfig}, obtained via {@link EnvConfig#section(String)}.
 *
 * <p>A section exposes the same typed accessors as {@link EnvConfig}, resolving each
 * requested key by prepending the section's prefix (and separator) and delegating to
 * the underlying {@link EnvConfig}'s own lookup and type-conversion logic. This means a
 * section's error and default-value behavior (missing keys throw {@link ConfigException}
 * naming the fully-qualified key, {@code (key, defaultValue)} overloads fall back to the
 * default, etc.) is identical to using the underlying {@link EnvConfig} directly.
 *
 * <p>A section over a prefix that matches no keys is not an error: it is simply empty,
 * as reported by {@link #keys()} and {@link #asMap()}.
 *
 * <p>This class is immutable and safe to share across threads.
 */
public final class EnvConfigSection {

    private final EnvConfig parent;
    private final String prefix;

    EnvConfigSection(EnvConfig parent, String prefix) {
        this.parent = parent;
        this.prefix = prefix;
    }

    /** Returns {@code true} if {@code key} is present within this section. */
    public boolean has(String key) {
        return parent.has(prefix + key);
    }

    /** Returns the raw string value for {@code key} within this section. */
    public String getString(String key) {
        return parent.getString(prefix + key);
    }

    /** Returns the raw string value for {@code key} within this section, or {@code defaultValue} if absent. */
    public String getString(String key, String defaultValue) {
        return parent.getString(prefix + key, defaultValue);
    }

    /**
     * Returns the value for {@code key} within this section, parsed as an {@code int}.
     *
     * @throws ConfigException if the key is missing, or present but not a valid integer
     */
    public int getInt(String key) {
        return parent.getInt(prefix + key);
    }

    /**
     * Returns the value for {@code key} within this section, parsed as an {@code int},
     * or {@code defaultValue} if the key is absent.
     *
     * @throws ConfigException if the key is present but not a valid integer
     */
    public int getInt(String key, int defaultValue) {
        return parent.getInt(prefix + key, defaultValue);
    }

    /**
     * Returns the value for {@code key} within this section, parsed as a {@code boolean}.
     * Accepts {@code "true"}/{@code "false"} in any letter case.
     *
     * @throws ConfigException if the key is missing, or present but not "true"/"false"
     */
    public boolean getBoolean(String key) {
        return parent.getBoolean(prefix + key);
    }

    /**
     * Returns the value for {@code key} within this section, parsed as a {@code boolean},
     * or {@code defaultValue} if the key is absent.
     *
     * @throws ConfigException if the key is present but not "true"/"false"
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return parent.getBoolean(prefix + key, defaultValue);
    }

    /** Returns an immutable view of this section's key/value pairs, with the prefix stripped. */
    public Map<String, String> asMap() {
        Map<String, String> scoped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : parent.asMap().entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                scoped.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(scoped);
    }

    /** Returns the set of keys present in this section, with the prefix stripped. */
    public Set<String> keys() {
        Set<String> scoped = new LinkedHashSet<>();
        for (String key : parent.keys()) {
            if (key.startsWith(prefix)) {
                scoped.add(key.substring(prefix.length()));
            }
        }
        return Collections.unmodifiableSet(scoped);
    }
}
