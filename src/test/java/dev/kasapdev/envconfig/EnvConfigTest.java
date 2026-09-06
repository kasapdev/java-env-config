package dev.kasapdev.envconfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EnvConfigTest {

    public static void main(String[] args) throws IOException {
        Path envFile = writeTempEnvFile();
        EnvConfig config;
        try {
            config = EnvConfig.load(envFile);
            testBasicStringValues(config);
            testCommentsAndBlankLinesAreSkipped(config);
            testDoubleQuotedValueWithEscapes(config);
            testSingleQuotedValueIsLiteral(config);
            testUnquotedValueIsTrimmed(config);
            testIntCoercionSuccess(config);
            testIntCoercionFailure(config);
            testBooleanCoercionSuccess(config);
            testBooleanCoercionFailure(config);
            testStringDefaultWhenMissing(config);
            testIntDefaultWhenMissing(config);
            testBooleanDefaultWhenMissing(config);
            testDefaultOverloadStillThrowsWhenPresentButUnparseable(config);
            testMissingRequiredKeyThrowsNamingKey(config);
            testValidatePassesWhenAllPresent(config);
            testValidateAggregatesAllMissingKeys(config);
        } finally {
            Files.deleteIfExists(envFile);
        }

        testMalformedLineThrows();
        testEmptyKeyThrows();
        testMissingFileThrowsConfigException();
        testUnknownEscapeSequenceRetainsBackslash();
        testTabEscapeSequence();
        testValueContainingEqualsSignIsPreservedInFull();
        testDuplicateKeysLastWins();

        TestKit.finish();
    }

    private static Path writeTempEnvFile() throws IOException {
        List<String> lines = List.of(
                "# Full-line comment, should be ignored",
                "APP_NAME=MyApp",
                "APP_PORT=8080",
                "DEBUG=true",
                "",
                "   ",
                "  # indented comment",
                "QUOTED_DOUBLE=\"hello world\"",
                "ESCAPED=\"line1\\nline2 and a \\\"quoted\\\" word\"",
                "QUOTED_SINGLE='raw \\n not-escaped literal'",
                "NOT_A_NUMBER=abc",
                "NOT_A_BOOL=maybe",
                "TRIMMED_KEY   =    value with internal   spaces   "
        );
        Path path = Files.createTempFile("envconfig-test", ".env");
        Files.write(path, lines, StandardCharsets.UTF_8);
        return path;
    }

    private static void testBasicStringValues(EnvConfig config) {
        TestKit.check("getString returns plain value", "MyApp".equals(config.getString("APP_NAME")));
    }

    private static void testCommentsAndBlankLinesAreSkipped(EnvConfig config) {
        TestKit.check("comment lines do not become keys", !config.has("#"));
        TestKit.check("blank lines produce no keys", config.keys().stream().noneMatch(String::isBlank));
        // 9 real key lines in the fixture file (comments and blank lines excluded).
        TestKit.check("exactly the expected number of keys were parsed", config.keys().size() == 9);
    }

    private static void testDoubleQuotedValueWithEscapes(EnvConfig config) {
        String expected = "line1\nline2 and a \"quoted\" word";
        TestKit.check("double-quoted value unescapes \\n and \\\"", expected.equals(config.getString("ESCAPED")));
        TestKit.check("double-quoted value strips surrounding quotes", "hello world".equals(config.getString("QUOTED_DOUBLE")));
    }

    private static void testSingleQuotedValueIsLiteral(EnvConfig config) {
        // Single-quoted values must NOT have escape sequences processed.
        TestKit.check("single-quoted value keeps backslash-n literal",
                "raw \\n not-escaped literal".equals(config.getString("QUOTED_SINGLE")));
    }

    private static void testUnquotedValueIsTrimmed(EnvConfig config) {
        TestKit.check("unquoted value trims leading/trailing whitespace but keeps internal spacing",
                "value with internal   spaces".equals(config.getString("TRIMMED_KEY")));
    }

    private static void testIntCoercionSuccess(EnvConfig config) {
        TestKit.check("getInt parses a valid integer", config.getInt("APP_PORT") == 8080);
    }

    private static void testIntCoercionFailure(EnvConfig config) {
        try {
            config.getInt("NOT_A_NUMBER");
            TestKit.check("getInt on non-numeric value throws ConfigException", false);
        } catch (ConfigException e) {
            TestKit.check("getInt failure message names the offending key",
                    e.getMessage().contains("NOT_A_NUMBER"));
        }
    }

    private static void testBooleanCoercionSuccess(EnvConfig config) {
        TestKit.check("getBoolean parses 'true'", config.getBoolean("DEBUG"));
    }

    private static void testBooleanCoercionFailure(EnvConfig config) {
        try {
            config.getBoolean("NOT_A_BOOL");
            TestKit.check("getBoolean on invalid value throws ConfigException", false);
        } catch (ConfigException e) {
            TestKit.check("getBoolean failure message names the offending key",
                    e.getMessage().contains("NOT_A_BOOL"));
        }
    }

    private static void testStringDefaultWhenMissing(EnvConfig config) {
        TestKit.check("getString(key, default) returns default when key absent",
                "fallback".equals(config.getString("DOES_NOT_EXIST", "fallback")));
    }

    private static void testIntDefaultWhenMissing(EnvConfig config) {
        TestKit.check("getInt(key, default) returns default when key absent",
                config.getInt("DOES_NOT_EXIST", 42) == 42);
    }

    private static void testBooleanDefaultWhenMissing(EnvConfig config) {
        TestKit.check("getBoolean(key, default) returns default when key absent",
                config.getBoolean("DOES_NOT_EXIST", true));
    }

    private static void testDefaultOverloadStillThrowsWhenPresentButUnparseable(EnvConfig config) {
        try {
            config.getInt("NOT_A_NUMBER", 99);
            TestKit.check("getInt(key, default) still throws when key present but unparseable", false);
        } catch (ConfigException e) {
            TestKit.check("getInt(key, default) exception names the key", e.getMessage().contains("NOT_A_NUMBER"));
        }
    }

    private static void testMissingRequiredKeyThrowsNamingKey(EnvConfig config) {
        try {
            config.getString("TOTALLY_ABSENT_KEY");
            TestKit.check("getString on missing required key throws ConfigException", false);
        } catch (ConfigException e) {
            TestKit.check("missing-key exception names the key", e.getMessage().contains("TOTALLY_ABSENT_KEY"));
        }
    }

    private static void testValidatePassesWhenAllPresent(EnvConfig config) {
        Set<String> required = new LinkedHashSet<>(Set.of("APP_NAME", "APP_PORT", "DEBUG"));
        boolean threw = false;
        try {
            config.validate(required);
        } catch (ConfigException e) {
            threw = true;
        }
        TestKit.check("validate() does not throw when all required keys are present", !threw);
    }

    private static void testValidateAggregatesAllMissingKeys(EnvConfig config) {
        Set<String> required = new LinkedHashSet<>(Set.of("APP_NAME", "MISSING_ONE", "MISSING_TWO"));
        try {
            config.validate(required);
            TestKit.check("validate() throws when required keys are missing", false);
        } catch (ConfigException e) {
            String message = e.getMessage();
            TestKit.check("validate() error mentions first missing key", message.contains("MISSING_ONE"));
            TestKit.check("validate() error mentions second missing key", message.contains("MISSING_TWO"));
            TestKit.check("validate() error does not list a present key as missing", !message.contains("APP_NAME"));
        }
    }

    private static void testMalformedLineThrows() throws IOException {
        Path badFile = Files.createTempFile("envconfig-bad", ".env");
        try {
            Files.write(badFile, List.of("THIS_LINE_HAS_NO_EQUALS_SIGN"), StandardCharsets.UTF_8);
            try {
                EnvConfig.load(badFile);
                TestKit.check("loading a malformed line throws ConfigException", false);
            } catch (ConfigException e) {
                TestKit.check("loading a malformed line throws ConfigException", true);
            }
        } finally {
            Files.deleteIfExists(badFile);
        }
    }

    private static void testEmptyKeyThrows() throws IOException {
        Path badFile = Files.createTempFile("envconfig-emptykey", ".env");
        try {
            Files.write(badFile, List.of("=no-key-here"), StandardCharsets.UTF_8);
            try {
                EnvConfig.load(badFile);
                TestKit.check("loading a line with an empty key throws ConfigException", false);
            } catch (ConfigException e) {
                TestKit.check("loading a line with an empty key throws ConfigException", true);
            }
        } finally {
            Files.deleteIfExists(badFile);
        }
    }

    private static void testMissingFileThrowsConfigException() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir"), "envconfig-does-not-exist-" + System.nanoTime() + ".env");
        try {
            EnvConfig.load(missing);
            TestKit.check("loading a nonexistent file throws ConfigException", false);
        } catch (ConfigException e) {
            TestKit.check("loading a nonexistent file throws ConfigException (not a raw IOException)", true);
        }
    }

    private static void testUnknownEscapeSequenceRetainsBackslash() throws IOException {
        Path file = Files.createTempFile("envconfig-escape", ".env");
        try {
            Files.write(file, List.of("KEY=\"a\\xb\""), StandardCharsets.UTF_8);
            EnvConfig cfg = EnvConfig.load(file);
            TestKit.check(
                    "an unrecognized escape sequence in a double-quoted value retains the literal backslash",
                    "a\\xb".equals(cfg.getString("KEY")));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void testTabEscapeSequence() throws IOException {
        Path file = Files.createTempFile("envconfig-tab", ".env");
        try {
            Files.write(file, List.of("KEY=\"a\\tb\""), StandardCharsets.UTF_8);
            EnvConfig cfg = EnvConfig.load(file);
            TestKit.check("the \\t escape sequence decodes to an actual tab character", "a\tb".equals(cfg.getString("KEY")));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void testValueContainingEqualsSignIsPreservedInFull() throws IOException {
        Path file = Files.createTempFile("envconfig-equals", ".env");
        try {
            Files.write(file, List.of("CONN=a=b=c"), StandardCharsets.UTF_8);
            EnvConfig cfg = EnvConfig.load(file);
            TestKit.check(
                    "only the first '=' splits key from value; the rest is preserved in the value",
                    "a=b=c".equals(cfg.getString("CONN")));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void testDuplicateKeysLastWins() throws IOException {
        Path file = Files.createTempFile("envconfig-dup", ".env");
        try {
            Files.write(file, List.of("DUP=first", "DUP=second"), StandardCharsets.UTF_8);
            EnvConfig cfg = EnvConfig.load(file);
            TestKit.check("duplicate keys resolve to the last occurrence's value", "second".equals(cfg.getString("DUP")));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
