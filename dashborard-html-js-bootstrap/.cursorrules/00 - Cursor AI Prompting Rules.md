# Cursor AI Prompting Framework — Advanced Usage Compendium

This compendium articulates a rigorously structured methodology for leveraging **Cursor AI** in concert with four canonical prompt schemata—**core.md**, **request.md**, **refresh.md**, and **RETRO.md**—ensuring the agent operates as a risk‑averse principal engineer who conducts exhaustive reconnaissance, executes with validated precision, and captures institutional learning after every session.

---

## I. Initialising the Core Corpus (`core.md`)

### Purpose

Establishes the agent’s immutable governance doctrine: **familiarise first**, research exhaustively, act autonomously within a safe envelope, and self‑validate.

### Set‑Up Options

| Scope                | Steps                                                                                                           |
| -------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Project‑specific** | 1. Create `.cursorrules` in the repo root.<br>2. Paste the entirety of **core.md**.<br>3. Commit.               |
| **Global**           | 1. Open Cursor → *Command Palette*.<br>2. Select **Configure User Rules**.<br>3. Paste **core.md**.<br>4. Save. |

Once loaded, these rules govern every subsequent prompt until explicitly superseded.

---

## II. Task‑Execution Templates

### A. Feature / Change Implementation (`request.md`)

Invoked to introduce new capabilities, refactor code, or alter behaviour. Enforces an evidence‑centric, assumption‑averse workflow that delivers incremental, test‑validated changes.

### B. Persistent Defect Resolution (`refresh.md`)

Activated when prior remediations fail or a defect resurfaces. Drives a root‑cause exploration loop culminating in a durable fix and verified resilience.

For either template:

1. Duplicate the file.
2. Replace the top placeholder with a concise request or defect synopsis.
3. Paste the entire modified template into chat.

The agent will autonomously:

* **Plan** → **Gather Context** → **Execute** → **Verify** → **Report**.
* Surface a live ✅ / ⚠️ / 🚧 ledger for multi‑phase endeavours.

---

## III. Post‑Session Retrospective (`RETRO.md`)

### Purpose

Codifies an end‑of‑conversation ritual whereby the agent distils behavioural insights and incrementally refines its standing rule corpus—**without** introducing session‑specific artefacts into the repository.

### Usage

1. After the primary task concludes, duplicate **RETRO.md**.
2. Send it as the final prompt of the session.
3. The agent will:

   * **Reflect** in ≤ 10 bullet points on successes, corrections, and lessons.
   * **Update** existing rule files (e.g., `.cursorrules`, `AGENT.md`) by amending or appending imperative, generalised directives.
   * **Report** back with either `✅ Rules updated` or `ℹ️ No updates required`, followed by the reflection bullets.

### Guarantees

* No new Markdown files are created unless explicitly authorised.
* Chat‑specific dialogue never contaminates rule files.
* All validation logs remain in‑chat.

---

## IV. Operational Best Practices

1. **Be Unambiguous** — Provide precise first‑line summaries in each template.
2. **Trust Autonomy** — The agent self‑resolves ambiguities unless blocked by the Clarification Threshold.
3. **Review Summaries** — Skim the agent’s final report and live TODO ledger to stay aligned.
4. **Minimise Rule Drift** — Invoke `RETRO.md` regularly; incremental rule hygiene prevents bloat and inconsistency.

---

### Legend

| Symbol | Meaning                                      |
| ------ | -------------------------------------------- |
| ✅      | Step or task fully accomplished              |
| ⚠️     | Anomaly encountered and mitigated            |
| 🚧     | Blocked, awaiting input or external resource |

---

By adhering to this framework, Cursor AI functions as a continually improving principal engineer: it surveys the terrain, acts with caution and rigour, validates outcomes, and institutionalises learning—all with minimal oversight.
