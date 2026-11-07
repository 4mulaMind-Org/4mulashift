# Interview Note 02: User Stories and Acceptance Criteria

**Date:** 2026-11-07
**Related files:** `docs/user-stories.md`, `docs/PRD.md`

## 30-second explanation

"After the PRD, I turned each feature into a user story with acceptance criteria in Given / When / Then form. That gave me 16 testable stories grouped into customer, vendor, platform-safety and admin epics. Each PRD feature maps to at least one story through a traceability table, so nothing is built without a requirement and every requirement has a test. For example, the double-booking story becomes a concurrency test: 10 parallel confirms, exactly 1 succeeds."

## What I did

- Wrote 16 user stories in four epics (Customer, Vendor, Booking safety, Admin and access).
- Wrote acceptance criteria for each, including failure cases, not only the happy path.
- Added a traceability table from PRD features F1 to F14 to stories.
- Marked unconfirmed rules as assumptions (offer expiry time, what happens with fewer than 3 vendors).
- Added a Definition of Ready for stories entering a sprint.

## Why I did it this way

| Decision | Reason |
|---|---|
| Given / When / Then criteria | They are unambiguous and map directly to automated tests. |
| Failure cases in the criteria | Most real bugs live in edge cases: duplicates, concurrency, expired offers. |
| Traceability table | Shows every feature is covered and makes it easy to check scope later. |
| Marking assumptions | I do not hide gaps; each assumption becomes a question to resolve before the sprint. |
| Platform stories (US-12, US-14) | Non-user-facing requirements such as locking and audit are still requirements and need acceptance criteria. |

## Alternatives I considered

- A plain feature list without criteria. Rejected: "done" would be subjective.
- Writing full use-case documents. Rejected: too heavy for a solo MVP; stories with criteria are enough.
- Only happy-path criteria. Rejected: it would hide the exact cases my project is meant to prove.

## Trade-offs and limitations

- Some rules are assumptions (10-minute offer window, admin reallocation when all offers fail). They may change after the pilot.
- Stories describe behaviour, not implementation. Technical design (tables, APIs) comes in the next step.
- 16 stories for the MVP is a lot for one person, which is why they are prioritised Must and Should.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| Not clear what to do when fewer than 3 vendors are eligible | Wrote the behaviour as an explicit assumption and kept it in the PRD open questions. |
| Safety requirements (locking, audit) are not user features | Wrote them as platform stories so they still get testable criteria. |

## Questions an interviewer may ask

**Q: What makes a good acceptance criterion?**
A: It is specific, testable and covers failure cases. "Exactly 1 of 10 concurrent confirms succeeds" can be automated; "booking should be safe" cannot.

**Q: How do you make sure no requirement is missed?**
A: A traceability table from PRD features to stories, so I can see gaps immediately.

**Q: How does a story become a test?**
A: Each criterion is a test case. For US-12 I will write a Testcontainers integration test with 10 threads against a real PostgreSQL.

**Q: What do you do with requirements you are unsure about?**
A: I record them as assumptions or open questions, decide before the sprint starts, and update the story.

**Q: What is the difference between Must and Should here?**
A: Must items are needed to prove the core engine (allocation, locking, idempotency, payment). Should items (dashboards, real-time updates, privacy hiding) improve the product but the engine works without them.

## Self-check

1. Give the Given / When / Then for the double-booking story from memory.
2. Which stories are not user-facing, and why do they still need criteria?
3. Name two assumptions in the stories and how you would validate them.
4. How does a story turn into an automated test?

## Proof and links

- `docs/user-stories.md` (16 stories, traceability table)
- Repo: https://github.com/4mulaMind-Org/4mulashift