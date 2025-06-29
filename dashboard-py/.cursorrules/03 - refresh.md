<Concise synopsis of the persistent defect here>

---

# Persistent Defect Resolution Protocol

This protocol articulates an **evidence‑driven, assumption‑averse diagnostic regimen** devised to isolate the fundamental cause of a recalcitrant defect and to implement a verifiable, durable remedy.

Duplicate this file, substitute the placeholder above with a succinct synopsis of the malfunction, and supply the template to the agent.

---

## 0 · Reconnaissance & System Cartography *(Read‑Only)*

> **Mandatory first step — no planning or state mutation may occur until completed.**
> *Interrogate the terrain before reshaping it.*

1. **Repository inventory** – Traverse the file hierarchy; catalogue languages, build tool‑chains, frameworks, and test harnesses.
2. **Runtime telemetry** – Enumerate executing services, containers, CI/CD workflows, and external integrations.
3. **Configuration surface** – Aggregate environment variables, secrets, IaC manifests, and deployment scripts.
4. **Historical signals** – Analyse logs, monitoring alerts, change‑logs, incident reports, and open issues.
5. **Canonical conventions** – Distil testing idioms, naming schemes, error‑handling primitives, and pipeline topology.

*No artefact may be altered until this phase is concluded and assimilated.*

---

## 1 · Problem Reformulation & Success Metrics

* Articulate the observed pathology and its systemic impact.
* Define the **remediated** state in quantifiable terms (e.g., all tests pass; error incidence < X ppm; p95 latency < Y ms).
* Enumerate constraints (temporal, regulatory, or risk‑envelope) and collateral effects that must be prevented.

---

## 2 · Context Acquisition *(Directed)*

* Catalogue all artefacts germane to the fault—source, configuration, infrastructure, documentation, test suites, logs, and telemetry.
* Employ token‑aware sampling (`head`, `wc ‑l`, `head ‑c`) to bound voluminous outputs.
* Delimit operative scope: subsystems, services, data conduits, and external dependencies implicated.

---

## 3 · Hypothesis Elicitation & Impact Valuation

* Postulate candidate root causes (regressive commits, configuration drift, dependency incongruities, permission revocations, infrastructure outages, etc.).
* Prioritise hypotheses by *posterior probability × impact magnitude*.

---

## 4 · Targeted Investigation & Empirical Validation

For each high‑ranking hypothesis:

1. **Design a low‑intrusion probe**—e.g., log interrogation, unit test, database query, or feature‑flag inspection.

2. **Execute the probe** using non‑interactive, time‑bounded commands with unified output:

   ```bash
   TZ='Asia/Jakarta' timeout 30s <command> 2>&1 | cat
   ```

3. **Record empirical evidence** to falsify or corroborate the hypothesis.

4. **Re‑rank** the remaining candidates; iterate until a single defensible root cause remains.

---

## 5 · Root‑Cause Ratification & Remediation Design

* Synthesise the definitive causal chain, substantiated by evidence.
* Architect a **core‑level remediation** that eliminates the underlying fault rather than masking symptoms.
* Detail dependencies, rollback contingencies, and observability instrumentation.

---

## 6 · Execution & Autonomous Correction

* **Read before you write**—inspect any file prior to modification.

* Apply corrections incrementally (workspace‑relative paths; granular commits).

* Activate *fail‑fast* shell semantics:

  ```bash
  set -o errexit -o pipefail
  ```

* Re‑run automated tests, linters, and static analysers; self‑rectify until the suite is green or the Clarification Threshold is met.

---

## 7 · Verification & Resilience Evaluation

* Execute regression, integration, and load‑testing matrices.
* Inspect metrics, logs, and alerting dashboards post‑remediation.
* Conduct lightweight chaos or fault‑injection exercises when operationally safe.

---

## 8 · Synthesis & Live TODO Ledger

Employ the ✅ / ⚠️ / 🚧 lexicon.

* **Root Cause** – Etiology of the defect.
* **Remediation Applied** – Code and configuration changes enacted.
* **Verification** – Test suites executed and outcomes.
* **Residual Actions** – Append or refresh a live TODO list.

---

## 9 · Continuous Improvement & Foresight

* Recommend high‑value adjunct initiatives (architectural refactors, test‑coverage expansion, enhanced observability, security fortification).
* Provide qualitative impact assessments and propose subsequent phases; migrate items to the TODO ledger only after the principal remediation is ratified.

---
