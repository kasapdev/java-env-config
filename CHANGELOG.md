# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.2.0] - 2026-09-07

### Added

- `EnvConfig.section(String prefix)`, returning a new `EnvConfigSection` view
  scoped to keys under `prefix + "_"`. Given keys like `DATABASE_HOST` and
  `DATABASE_PORT`, `config.section("DATABASE")` exposes `getString("HOST")`
  and `getInt("PORT")` with the prefix stripped.
- `EnvConfigSection` exposes the same typed accessors as `EnvConfig`
  (`getString`, `getInt`, `getBoolean`, `has`, `keys`, `asMap`), each
  delegating to the underlying `EnvConfig`'s own lookup and type-conversion
  logic against the fully-qualified key, so error messages, default-value
  fallback, and type coercion are identical between the two.
- A prefix that matches no keys is not an error: `section()` returns a
  valid, empty section, and requesting a key from it behaves exactly as
  requesting a missing key from `EnvConfig` does.
- Regression tests covering multiple prefixed groups scoped independently,
  cross-section key isolation, and the empty-section case.
- README "Config Sections" section with a runnable example, plus a second
  `## Usage` example for a more realistic startup sequence.

## [1.1.0] - 2026-09-06

### Added

- Test coverage for edge cases in `EnvConfig`:
  - A line with an empty key (e.g. `=value`) throws `ConfigException`.
  - Loading a nonexistent file throws `ConfigException` (wrapping the
    underlying I/O failure) instead of a raw `IOException`.
  - An unrecognized escape sequence in a double-quoted value (e.g.
    `\x`) retains the literal backslash rather than silently dropping
    it or throwing.
  - The `\t` escape sequence in a double-quoted value decodes to an
    actual tab character.
  - Only the first `=` on a line splits key from value, so a value that
    itself contains `=` is preserved in full.
  - Duplicate keys in the same file resolve to the last occurrence's
    value.

No behavioral changes were needed — all new edge-case tests passed against
the existing implementation.
