<Concise synopsis of the desired feature or modification>

---

# Feature‑or‑Change Implementation Protocol

This protocol prescribes an **evidence‑centric, assumption‑averse methodology** commensurate with the analytical rigour expected of a senior software architect. Duplicate this file, replace the placeholder above with a clear statement of the required change, and submit it to the agent.

---

## 0 · Familiarisation & System Cartography *(read‑only)*

**Goal:** Build a high‑fidelity mental model of the existing codebase and its operational context before touching any artefact.

1. **Repository census** — catalogue languages, build pipelines, and directory taxonomy.
2. **Dependency topology** — map intra‑repo couplings and external service contracts.
3. **Runtime & infrastructure schematic** — list processes, containers, environment variables, and IaC descriptors.
4. **Idioms & conventions** — distil naming regimes, linting rules, and test heuristics.
5. **Verification corpus & gaps** — survey unit, integration, and e2e suites; highlight coverage deficits.
6. **Risk loci** — isolate critical execution paths (authentication, migrations, public interfaces).
7. **Knowledge corpus** — ingest ADRs, design memos, changelogs, and ancillary documentation.

▶️ **Deliverable:** a concise mapping brief that informs all subsequent design decisions.

---

## 1 · Objectives & Success Metrics

* Reframe the requested capability in precise technical language.
* Establish quantitative and qualitative acceptance criteria (correctness, latency, UX affordances, security posture).
* Enumerate boundary conditions (technology stack, timelines, regulatory mandates, backward‑compatibility).

---

## 2 · Strategic Alternatives & Core‑First Design

1. Enumerate viable architectural paths and compare their trade‑offs.
2. Select the trajectory that maximises reusability, minimises systemic risk, and aligns with established conventions.
3. Decompose the work into progressive **milestones**: core logic → auxiliary extensions → validation artefacts → refinement.

---

## 3 · Execution Schema *(per milestone)*

For each milestone specify:

* **Artefacts** to inspect or modify (explicit paths).
* **Procedures** and CLI commands, each wrapped in `timeout 30s <cmd> 2>&1 | cat`.
* **Test constructs** to add or update.
* **Assessment hooks** (linting, type checks, CI orchestration).

---

## 4 · Iterative Implementation Cycle

1. **Plan** — declare the micro‑objective for the iteration.
2. **Contextualise** — re‑examine relevant code and configuration.
3. **Execute** — introduce atomic changes; commit with semantic granularity.
4. **Validate** —

   * Run scoped test suites and static analyses.
   * Remediate emergent defects autonomously.
   * Benchmark outputs against regression baselines.
5. **Report** — tag progress with ✅ / ⚠️ / 🚧 and update the live TODO ledger.

---

## 5 · Comprehensive Verification & Handover

* Run the full test matrix and static diagnostic suite.
* Generate supplementary artefacts (documentation, diagrams) where they enhance understanding.
* Produce a **terminal synopsis** covering:

  * Changes implemented
  * Validation outcomes
  * Rationale for key design decisions
  * Residual risks or deferred actions
* Append the refreshed live TODO ledger for subsequent phases.

---

## 6 · Continuous‑Improvement Addendum *(optional)*

Document any non‑blocking yet strategically valuable enhancements uncovered during the engagement—performance optimisations, security hardening, refactoring, or debt retirement—with heuristic effort estimates.
