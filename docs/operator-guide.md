# Operator Guide

## First Deployment

1. Define the operator's workshop coverage and crew intake process.
2. Define consent and purpose categories for jeweller/workshop records.
3. Run synthetic operating cases (inventory log entry, crew-operation
   scheduling, supply coordination, safety-concern flagging).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions (all flagged safety concerns, above-threshold supply
   orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (burn exposure, chemical/acid
  exposure, custody discrepancy)
- provenance for all operating records (jeweller and workshop both
  independently registered)
- human review for high-risk cases
- audit export for all gated actions
- a hard, unconditional block on any attempt to route a
  jewellery-fabrication-execution decision, a precious-materials
  custody-transfer authorization, or a workshop-safety-officer
  override decision, through this actor — those decisions stay the
  workshop safety officer's / an independently verified custody
  chain's exclusive authority end to end

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans, and that no deployment configuration can route a
jewellery-fabrication-execution decision, a precious-materials
custody-transfer authorization, or a workshop-safety-officer judgment
override through this actor.
