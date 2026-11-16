# Exploratory hashmap candidates

These two cohorts preceded the final two-object-map candidate. Neither is pooled
with final measurements. Both compare clean main at
`e8c0d83110533464e707dc51802e4e4e9fbd065a` against their own frozen JAR.

- [Six-map clear and required lookup](lookup-clear/README.md): 48 pairs, 96 captures.
  The numeric-map clear guards were rejected after the small nonempty-clear control
  slowed in both repetitions.
- [Long-key mixer](long-keys/README.md): 48 pairs, 96 captures. This fixes an extreme
  high-bit stride collision pattern but makes some favorable existing patterns
  substantially slower. No mixer or public hash edits remain in production.

Each directory retains full samples, logs, source and artifact hashes, exact
runner, and patches needed to reconstruct the prototype from the final sources.
The patches use zero context; apply them with `git apply --unidiff-zero`.
All captures used the same local paired JVM protocol described in the main report.
CPU contention was uncontrolled and varied across captures.
