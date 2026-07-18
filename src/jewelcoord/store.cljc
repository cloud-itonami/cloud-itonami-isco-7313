(ns jewelcoord.store
  "SSoT for the ISCO-08 7313 jewellery and precious metal workshop
  scheduling/logistics/inventory-logging coordination actor (itonami
  actor pattern, ADR-2607121000 / CLAUDE.md Actors section; README's
  'Robotics premise' — a workshop scheduling/logistics coordination
  robot performs crew scheduling, precious-material/finished-piece
  inventory data logging and non-precious materials/tools
  supply-order coordination for a jewellery and precious metal
  workshop crew under this advisor/governor pair, which never
  dispatches hardware itself, never performs fabrication work itself,
  and never finalizes a jewellery-fabrication-execution decision or a
  precious-materials custody-transfer authorization, and never
  overrides a workshop safety officer's judgment — those remain the
  workshop safety officer's / an independently verified custody chain's
  exclusive authority). Modeled closely on cloud-itonami-isco-7211's
  foundrycoord.store (comparable precious-materials-adjacent
  workshop-safety domain shape).

  Domain:

    jeweller — a registered jewellery/precious-metal-working crew
               member (:jeweller-id, :name)
    workshop — a registered workshop site {:workshop-id :name
               :max-supply-cost number}. `:max-supply-cost` is an
               informational registered ceiling used only to decide
               whether a `:coordinate-supply-order` proposal escalates
               to human sign-off (the governor never blocks a
               within-threshold order outright; it only decides
               commit vs. escalate).
    record   — a committed operating record (a logged
               precious-material/finished-piece inventory entry, a
               scheduled crew/task operation, a flagged safety
               concern, or a coordinated non-precious materials/tools
               supply order) — written ONLY via commit-record!. Never
               a fabrication-execution record or a custody-transfer
               authorization record — this actor coordinates
               scheduling/logistics/inventory-logging only.
    ledger   — append-only audit trail, commit or hold.")

(defprotocol Store
  (jeweller [s jeweller-id])
  (workshop [s workshop-id])
  (records-of [s jeweller-id])
  (ledger [s])
  (register-jeweller! [s jeweller])
  (register-workshop! [s workshop])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (jeweller [_ jeweller-id] (get-in @a [:jewellers jeweller-id]))
  (workshop [_ workshop-id] (get-in @a [:workshops workshop-id]))
  (records-of [_ jeweller-id] (filter #(= jeweller-id (:jeweller-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-jeweller! [s j]
    (swap! a assoc-in [:jewellers (:jeweller-id j)] j) s)
  (register-workshop! [s w]
    (swap! a assoc-in [:workshops (:workshop-id w)] w) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:jewellers {} :workshops {} :records [] :ledger []}
                                    seed)))))
