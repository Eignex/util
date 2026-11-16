# Exploratory comparison (rejected deletion candidate)

These 96 captures compare clean main with the first construction prototype plus
deferred clearing during hash-table deletion. This candidate is not the final
implementation. Both arms passed fixture checks; shared-host contention was high
and varied substantially.

Deletion results were inconsistent, including slower large-map cases, so all
deletion edits were reverted. The construction implementation was then refined
and measured separately in the parent directory. Do not pool these cohorts.

Apply `candidate.patch` with `git apply --unidiff-zero` at the baseline revision
in `environment.json` to reproduce the measured production prototype. Its four source hashes were verified against that metadata.
`runner.py` preserves the exact runner version used. The fixtures remain the
unchanged sources identified by hashes in `environment.json`. The four raw arm JSON
files contain every iteration and uncertainty estimate; `summary.json` keeps each
matching pair. The validation recorded here belongs to the prototype.
