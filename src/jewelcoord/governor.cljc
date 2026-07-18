(ns jewelcoord.governor
  "JewelCoordGovernor — the independent safety/custody/scope layer
  gating every workshop scheduling/logistics/inventory-logging
  proposal an advisor may make for a jewellery and precious metal
  workshop crew. The governor never dispatches hardware itself, never
  performs fabrication work itself, and never finalizes a
  jewellery-fabrication-execution decision or a precious-materials
  custody-transfer authorization, and never overrides a workshop
  safety officer's judgment — those are permanently out of this
  actor's scope and remain the workshop safety officer's / an
  independently verified custody chain's exclusive authority
  (README's 'Robotics premise': this actor coordinates WORKSHOP
  SCHEDULING/LOGISTICS/INVENTORY-LOGGING ONLY — it never performs
  fabrication work or authorizes material transfers itself). Modeled
  closely on cloud-itonami-isco-7211's foundrycoord.governor
  (comparable precious-materials-adjacent workshop-safety domain
  shape).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. jeweller provenance   — the crew member must be independently
                                verified/registered before any action.
    2. workshop provenance   — the workshop site must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs fabrication work itself;
                                it only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-inventory-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                jewellery-fabrication-execution
                                decision, or to authorize a
                                precious-materials custody-transfer, or
                                to override a workshop safety officer's
                                judgment, is a hard, permanent block
                                (checked both against the proposed :op
                                and, defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"proceed with the
                                fabrication work\" / \"authorize the
                                custody transfer of precious materials\"
                                / \"override the workshop safety
                                officer's judgment\", never as bare
                                nouns like \"fabrication\", \"gold\" or
                                \"custody\", so the check can never
                                self-trip on the advisor's own routine
                                rationale text, e.g. \"logged inventory
                                record for jeweller …\" or \"scheduled
                                crew operation for finishing task …\" or
                                \"…routed for workshop safety officer
                                review\" — all three legitimately
                                contain those bare nouns but none is a
                                finalization action, and all are
                                exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a burn-exposure / chemical-exposure /
                                custody-discrepancy concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [jewelcoord.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-inventory-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-fabrication-decision :authorize-custody-transfer
    :proceed-with-fabrication-work :finalize-custody-transfer-decision
    :override-workshop-safety-officer-judgment
    :override-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("fabrication", "gold", "custody", "workshop", "officer") — so this
;; can never match inside the mock advisor's own default rationale
;; text (which legitimately contains those bare nouns, e.g. "finishing
;; task" / "workshop safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the fabrication work" "proceed with fabrication work"
   "authorize the custody transfer of precious materials"
   "authorize the custody transfer" "authorize a custody transfer"
   "finalize the fabrication decision" "finalize the custody transfer decision"
   "override the workshop safety officer's judgment"
   "override the safety officer's judgment"
   "override workshop safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal jeweller-record workshop-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? jeweller-record)
      (conj {:rule :no-jeweller
             :detail "未登録 jeweller への提案は不可（jeweller record は独立して検証・登録済みでなければならない）"})

      (nil? workshop-record)
      (conj {:rule :no-workshop
             :detail "未登録 workshop への提案は不可（workshop record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は加工作業・材料移転を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "宝飾・貴金属加工実行判断の確定、貴金属材料の custody transfer 認可、workshop safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `jewelcoord.store/Store`. Pure — never mutates
  the store, never dispatches a workshop operation."
  [request _context proposal store]
  (let [jeweller-record (store/jeweller store (:jeweller-id request))
        workshop-record (some->> (:workshop-id proposal) (store/workshop store))
        hard (hard-violations proposal jeweller-record workshop-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
