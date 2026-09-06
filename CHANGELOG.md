# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
