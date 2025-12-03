<!-- Copilot / AI agent instructions for the super-grep repo -->
# super-grep — AI Assistant Notes

Purpose
- Short summary: this repository contains a compact, C-based grep-like tool (`sg`) that searches a precomputed concatenated file index (`filecode` / `filecodeup`) for speed. Key files: `sg.c`, `makefile`, `makeFilecode.csh`, and `sg-history`.

Quick build & run
- Build: run `make` (the `makefile` target produces `sg` via `gcc -o sg sg.c`).
- Typical run examples (from repo root):
  - `./sg -4 "pattern"` — set surrounding context lines to 4 (see notes below on numeric flags).
  - `./sg -c "CaseSensitivePattern"` — enable case-sensitive search.
  - `./sg -A "first" "second"` — find lines containing both strings (AND).
  - `./sg -N "first" "second"` — find lines containing `first` but NOT `second` (NOT).

Where the data comes from
- `sg.c` expects gzipped precomputed files and reads them with `popen("zcat ...")` against `filecode` and `filecodeup` (uppercase copy used for case-insensitive matching). `sg.c` now reads the `SG_DATABASE` environment variable to find the directory containing the gzipped files; if `SG_DATABASE` is unset it falls back to `/repo/mine`.
- To regenerate `filecode` and `filecodeup`, inspect and run `makeFilecode.csh` (creates `filecode` and `filecodeup` and gzips them). Either run it where your code trees exist or set `DATABASE` to the folder containing the gzipped files.

Command-line behavior (important implementation details)
- Context lines: numeric digits passed as a flag set the number of surrounding lines (AFTER). Example: `-4` or `-12` (multi-digit allowed) — the code accumulates digits in the switch over `'0'..'9'` to compute AFTER. Values are clamped to 1..45 (above 45 resets to default 4).
- Case handling: the program treats searches as case-insensitive by default (it upper-cases the input when `casesens==0`). Pass `-c` or `-C` to enable case-sensitive search (the code upper-cases option letters with `toupper`, so case doesn't matter for flags).
- Boolean operators: `-A` sets AND mode, `-N` sets NOT mode. These are mutually exclusive. The printed usage in `sg.c` shows `-n` as “Number of surrounded lines”, but actual code uses digits for that — prefer digit flags per the implementation.
- History flag: `-H` executes `system("cat ~/sg-history")`. The repo contains `sg-history` (in repo root); if you want `-H` to show this file directly, copy it to `~/sg-history` or adjust the code.
- Note: `-v` appears in the usage string but is not implemented in the current `sg.c` logic.

Key code patterns to know
- Single-file C binary: logic is concentrated in `sg.c` (parsing options, streaming `zcat` output, cyclic buffer for BEFORE context, printing AFTER context). Treat `sg.c` as the canonical source of truth for behavior.
- Two-stream comparison for case-insensitivity: `filecode` contains original lines; `filecodeup` contains the same lines uppercased — `sg.c` reads both and compares either original or uppercased stream depending on `casesens`.
- Heavy reliance on `popen("zcat ...")`: deployment assumes the filecode files are gzipped and available under `DATABASE`.

Integration & deployment notes
- The `sg.c` binary reads the `SG_DATABASE` environment variable to locate the data files. To run locally set the env var before invoking `sg`, for example:

```bash
export SG_DATABASE=.
./sg -4 "pattern"
```

If you prefer not to set an env var, you can still edit `sg.c` to change the fallback path `/repo/mine`.
- `makeFilecode.csh` expects a directory structure with `*.rss` directories and `.ada` files inside — it concatenates many files into `filecode` and `filecodeup`. Running this script in the repo root will produce the `filecode*` artifacts.

Repository conventions & quirks
- Minimal build system: `makefile` only builds `sg` with `gcc -o sg sg.c` — there are no unit tests, CI configs, or packaging targets.
- Options parsing uses `toupper` on incoming option letters; numeric flags are handled as characters `'0'..'9'`. The printed usage and actual parsing contain small mismatches — follow `sg.c` implementation when in doubt.
- `sg-history` is kept in the repo, but `-H` expects `~/sg-history` at runtime.

Files to inspect when making changes
- `sg.c` — main logic and the single source of truth for runtime behaviour and edge cases.
- `makefile` — simple build rule.
- `makeFilecode.csh` — shows how `filecode` / `filecodeup` are generated; useful when changing indexing assumptions.
- `sg-history` — changelog and developer assumptions about `filecode` format.

Suggested small improvements (for contributors)
- Replace the `DATABASE` macro with a runtime flag or environment variable so the tool is usable locally without editing source.
- Make `-H` read the repo `sg-history` path or print both repo and `~/sg-history` to avoid confusion.
- Add a `--help` long option that accurately documents the digit-based context option and notes `-v` is unimplemented.

If you need more detail
- Tell me which area you want deeper: option parsing, `filecode` generation, or making the binary portable — I can add examples or a short patch to improve clarity.
