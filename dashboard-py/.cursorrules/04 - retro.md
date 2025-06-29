# META‑PROMPT — Post‑Session Retrospective & Rule Consolidation

This meta‑prompt defines an end‑of‑conversation ritual in which the agent distils lessons learned and incrementally refines its standing governance corpus—without polluting the repository with session‑specific artefacts.

---

## I. Reflective Synthesis *(⛔ do NOT copy into rule files)*

1. **Scope** — Re‑examine every exchange from the session’s initial user message up to—but not including—this prompt.
2. **Deliverable** — Produce **no more than ten** concise bullet points that capture:
   • Practices that demonstrably advanced the dialogue or outcome.
   • Behaviours the user corrected, constrained, or explicitly demanded.
   • Actionable heuristics to reinforce or recalibrate in future sessions.
3. **Ephemeral Nature** — These bullets are transient coaching artefacts and **must not** be embedded in any rule file.

---

## II. Canonical Corpus Reconciliation *(✅ rules only)*

1. **Harvest Lessons** — Translate each actionable heuristic into a prescriptive rule.
2. **Inventory** — Open every extant governance file (e.g., `.cursorrules`, `core.md`, `AGENT.md`, `CLAUDE.md`).
3. **Update Logic**
   • *If* a semantically equivalent rule exists, **refine** it for precision and clarity.
   • *Otherwise* **append** a new rule in canonical order.
4. **Rule Style** — Each rule **must** be:
   • Imperative (e.g., “Always …”, “Never …”, “If X, then Y …”).
   • Generalised—free of session‑specific details, timestamps, or excerpts.
   • Concise, deduplicated, and consistent with the existing taxonomy.
5. **Creation Constraint** — **Never** introduce new Markdown files unless explicitly mandated by the user.

---

## III. Persistence & Disclosure

1. **Persist** — Overwrite the modified rule files *in situ*.
2. **Disclose** — Reply in‑chat with:

   1. `✅ Rules updated` or `ℹ️ No updates required`.
   2. The bullet‑point Reflective Synthesis for the user’s review.

---

## IV. Operational Safeguards

* All summaries, validation logs, and test outputs **must** be delivered in‑chat—**never** through newly created Markdown artefacts.
* `TODO.md` may be created or updated **only** when the endeavour spans multiple sessions and warrants persistent tracking; transient tasks shall be flagged with inline ✅ / ⚠️ / 🚧 markers.
* If a modification is safe and within scope, execute it without seeking further permission.
* Adhere to the **Clarification Threshold**: pose questions only when confronted with conflicting sources, missing prerequisites, irreversible risk, or exhausted discovery pathways.

---

### These directives are mandatory for every post‑conversation retrospective.
