# cloud-itonami-isco-7313

Open Occupation Blueprint for **ISCO-08 7313**: Jewellery and Precious Metal Workers.

This repository designs a forkable OSS business for a jewellery workshop scheduling and logistics coordination practice: a workshop scheduling/logistics/inventory-logging coordination robot manages crew/task and inventory records under a governor-gated actor, so a jewellery and precious metal workshop crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/jewelcoord/` implements the
`JewelCoordActor` as a `langgraph.graph/state-graph`
(`jewelcoord.actor`) wired to a `Jewellery Workshop Coordination
Advisor` (`jewelcoord.advisor`) and an independent `JewelCoordGovernor`
(`jewelcoord.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 23 tests / 50 assertions green (`kbb -M:test`).
HARD invariants (always hold, never overridable):
jeweller provenance, workshop provenance, no-actuation (`:effect` must
be `:propose`), a closed op-allowlist (`:log-inventory-record`,
`:schedule-crew-operation`, `:flag-safety-concern`,
`:coordinate-supply-order` — nothing else may ever be proposed), and a
permanent, unconditional block on any proposal that would directly
finalize a jewellery-fabrication-execution decision (e.g. deciding to
proceed with specific fabrication work), authorize a
precious-materials custody transfer, or override a workshop safety
officer's judgment. Always-escalate paths (human sign-off regardless
of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a workshop scheduling/logistics/inventory-logging coordination robot performs crew scheduling, precious-material/finished-piece inventory data logging and non-precious materials/tools supply-order coordination for a jewellery and precious metal workshop crew, under an actor that proposes actions and an independent **Jewellery Workshop Coordination Governor** that gates them. The governor never
dispatches hardware itself, never performs fabrication work in the workshop, and never finalizes a jewellery-fabrication-execution decision or authorizes a precious-materials custody transfer or overrides a workshop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged burn-exposure/chemical-exposure/custody-discrepancy concern, or an above-threshold supply order) require human sign-off. **This actor coordinates workshop scheduling/logistics/inventory-logging only — it never performs fabrication work or authorizes material transfers itself.**

## Core Contract

```text
crew roster + workshop registration + safety-reporting policy
        |
        v
Jewellery Workshop Coordination Advisor -> JewelCoordGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a jewellery-fabrication-execution decision, authorize a precious-materials
custody transfer, override a workshop safety officer's judgment, suppress an
operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7313`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
