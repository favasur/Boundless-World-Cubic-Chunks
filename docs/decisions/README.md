# Architecture Decision Records (ADRs)

Use this directory to record any intentional deviation from the original 1.12.2 Cubic Chunks logic.

## When to write an ADR

- The 1.21.x engine makes a 1:1 port impossible.
- A performance or compatibility trade-off requires changing the original behavior.
- A subsystem is being rewritten for a loader-specific reason (NeoForge vs. Fabric).

## ADR format

Use `0000-example-adr.md` as a template. Name new ADRs with a four-digit number and a short kebab-case title, e.g. `0001-async-chunk-loading.md`.

## Approval

ADRs must be proposed in a dedicated issue or pull request and approved by the project maintainer before implementation. The ADR should be merged at the same time as the code change.
