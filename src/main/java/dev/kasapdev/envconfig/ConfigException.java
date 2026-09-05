package dev.kasapdev.envconfig;

/**
 * Thrown when configuration parsing or access fails: a required key is
 * missing, a value cannot be parsed as the requested type, a config file is
 * malformed, or a {@link EnvConfig#validate(java.util.Set)} check finds one
 * or more required keys absent.
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
