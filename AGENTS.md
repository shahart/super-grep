# Repository Guidelines

## Project Structure & Module Organization
`src/main.rs` is the active Rust entrypoint for the `sg` CLI. `Cargo.toml` defines the crate and dependencies, and `target/` contains generated build output that should not be edited manually. Legacy C sources remain at the repo root (`sg.c`, `makefile`) and are still covered by CI. Sample search data and demo assets live in `filecode`, `filecodeup`, and the `15/` and `16/` example directories.

## Build, Test, and Development Commands
Use Cargo for the Rust implementation:

- `cargo build --verbose` builds the current CLI and matches the Rust CI workflow.
- `cargo test --verbose` runs the unit test in `src/main.rs`.
- `cargo run -- -n 4 -s container` runs the tool locally against `filecode`.
- `make` builds the legacy C binary from `sg.c`.

Prefer verifying both `cargo build` and `cargo test` before opening a PR. Run `make` as well if you touch the C path or shared data files.

## Coding Style & Naming Conventions
Follow standard Rust style: 4-space indentation, `snake_case` for functions and variables, and `CamelCase` only for types. Keep CLI parsing in `clap` derives and return `io::Result<()>` from fallible top-level flows, as in `main`. Avoid introducing large helper modules unless logic clearly outgrows `main.rs`. For the C code, keep the existing simple procedural style and preserve the current `makefile` target names.

## Testing Guidelines
The repository currently uses Rust’s built-in test framework with inline tests under `#[cfg(test)]`. Name tests for observable behavior, for example `test_search_container_in_filecode`. Tests may rely on the checked-in `filecode` fixture, so keep that file stable unless the change explicitly updates expected behavior. Add or update tests for new flags, output formatting, or search semantics.

## Commit & Pull Request Guidelines
Recent commits are short, imperative, and specific, for example `Typo` and `Rust -n param...`. Keep subjects concise and focused on one change. Pull requests should include a brief description, note any behavior changes to CLI flags or output, and list the commands you ran (`cargo test --verbose`, `make`, etc.). Link related issues when applicable.
