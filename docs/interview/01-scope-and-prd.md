# Interview Note 01: Scope and PRD

**Date:** 2026-11-07
**Related files:** `docs/PRD.md`, `README.md`

## 30-second explanation

"4mulaShift is a relocation platform. Before writing any code, I cut the idea down to one MVP: moving leads in one city. A customer submits a request, the system offers it to exactly 3 vendors using a fairness score, and the first vendor to accept gets a booking that is protected against double booking and double payment. I wrote a PRD with goals, non-goals and measurable success metrics first, because changing a design is much cheaper than changing code."

## What I did

- Created a GitHub repo inside an organization and set up a docs-first structure: `docs/`, `services/`, `postman/`.
- Added a `.gitignore` so IDE files and build output never reach the repo.
- Reduced a very large vision (moving, PG, flat, marketplace, services, AI) to one MVP.
- Wrote PRD v0.1 with problem, users, goals, non-goals, success metrics, MoSCoW features, core flow, non-functional requirements and open questions.

## Why I did it this way (decisions)

| Decision | Reason |
|---|---|
| Docs before code | A wrong requirement found after coding costs far more to fix than one found in a document. |
| MVP = moving leads only | One complete, correct flow is more valuable than five half-built ones. I was working solo. |
| Picked 3 problems out of 7 | Spam/monopoly, double booking and double payment are the ones I can prove with automated tests. |
| Measurable success metrics | "10 concurrent confirms, exactly 1 succeeds" is a pass or fail test, not an opinion. |
| Non-goals section | It protects me from scope creep and tells reviewers what I deliberately left out. |

## Alternatives I considered

- Building all lead types (moving, PG, flat) at once. Rejected: too much for one developer, and it delays proving the core engine.
- Starting with microservices. Rejected: a modular monolith is faster to build and easier to debug, and modules can be split later.
- Starting with code and writing docs afterwards. Rejected: design decisions get lost and are hard to explain later.

## Trade-offs and limitations (be honest)

- The fairness formula has a weakness: `days_since_last_order * 10` can push an inactive but low-rated vendor above a reliable one. I listed this as an open question and plan to add a minimum rating filter or a cap.
- First-to-accept still rewards fast responders, so fairness is only partial. A short accept window could fix this.
- One city and test-mode payments mean the MVP proves the engine, not the market.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| `git push` from the terminal timed out (network timeout to GitHub) | Confirmed it was a network path issue, not a login issue, and pushed through the IDE instead. |
| Extra character in the cloned folder name | Noticed it in the IDE, renamed the folder before the first commit so no path problems carried forward. |
| Pasted README text into the terminal by mistake | Recognised the zsh errors, edited the file in the editor instead. Nothing was lost. |
| File was staged at the wrong location, then moved | Used `git add .` to restage so Git recorded the final layout. |

## Questions an interviewer may ask

**Q: Why did you limit the scope so much?**
A: One correct end-to-end flow with proof (tests) is worth more than many features without it. The roadmap keeps the other lead types, but I only build what I can verify.

**Q: How do you know the MVP is successful?**
A: Through measurable criteria in the PRD: exactly one success out of 10 concurrent confirms, no duplicate records on repeated requests, and p95 latency targets.

**Q: What is a non-goal and why list it?**
A: Something I deliberately will not build in this phase. Listing it prevents scope creep and makes trade-offs visible.

**Q: What is the weakest part of your design right now?**
A: The fairness scoring. The inactivity term can favour low-quality vendors, and first-to-accept favours fast responders. I have noted both as open questions with possible fixes.

**Q: Why a modular monolith?**
A: Fast to build, simple to debug and deploy, and clear module boundaries (lead, allocation, booking, payment, vendor) let me split services later if one needs to scale independently.

## Self-check (answer these in your own words before an interview)

1. Which 3 problems does the MVP solve, and why those?
2. What is in the non-goals, and why?
3. Name two weaknesses of the fairness formula.
4. What does "exactly 1 of 10 concurrent confirms succeeds" prove?

## Proof and links

- Repo: https://github.com/4mulaMind-Org/4mulashift
- PRD: `docs/PRD.md`