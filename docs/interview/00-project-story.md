# Interview Note 00: Project Story (Why, Problem, Existing Solutions, Outcome)

This is the first thing an interviewer asks: "Tell me about your project." Keep this note updated as the project grows.

**Rule:** only say what is true. If something is not built or measured yet, say so. That is more convincing than a big claim.

---

## 1. How the idea came to me (YOU MUST WRITE THIS PART)

I cannot write your real story. Write 3 to 4 lines in your own words. Use a real moment, not a pitch.

Questions that help you remember:
- Did you, a friend or family member shift a house, PG or room? What went wrong?
- Did you get spam calls after filling a moving form? How many, how long?
- Did you see a vendor lose work to a bigger one?
- What did you notice that made you think "this can be solved with software"?

```
My story (fill in):
- When / where:
- What happened:
- What bothered me:
- Why I thought it could be solved with software:
```

Example shape (replace with your truth): "When I (or someone I know) tried to book packers and movers, the phone number got shared with many brokers and calls kept coming for days. I wanted to see if a system could give a request to only a few vendors, fairly, and make the booking safe."

---

## 2. The problem (what is broken today)

| Problem | What happens in real life | Root cause |
|---|---|---|
| Broker spam and vendor monopoly | A customer's number is shared with many brokers; demand concentrates on a few big vendors | Lead-selling models earn from contact details, and there is no fair allocation rule |
| Double booking | Two customers can book the same vehicle or slot | Availability checked in application code, not locked in the database |
| Double payment on retry | A network failure and a retry can charge or book twice | No idempotency layer |

I also noticed other problems (wrong vehicle size, unwanted goods, fragmented relocation steps), but I deliberately kept them for later phases.

---

## 3. Existing solutions and their gaps

Check this table yourself before using it in an interview. I wrote it from your project analysis and I have not verified each company's current features.

| Type of player | What they do well | Gap I am targeting |
|---|---|---|
| Moving aggregators / marketplaces | Many movers, quotes | Allocation is usually first-responder or paid priority, so small vendors get less work |
| Property platforms (flat and PG discovery) | Listings and matching | Focus on the customer side; moving is a separate flow |
| Managed moving services | Pricing, tracking | They are the vendor themselves, so there is no multi-vendor fairness |
| Classifieds (used goods) | Buying and selling | No moving or logistics |

Safe way to say it: "Most platforms optimise for the customer or for the highest bidder. I wanted to look at the vendor side: fair distribution, and a booking that is provably safe."

Do not claim "no competitor has this" in an interview. Say "I did not find a platform that publishes a fairness rule, but I have not verified every competitor."

---

## 4. My solution in one paragraph

A customer submits a moving request. The system picks the vehicle size, scores eligible vendors using inactivity, remaining quota, rating and cancellations, and offers the request to exactly 3 vendors. The first to accept gets a booking, protected by a row lock and a database constraint against double booking, and by an idempotency key against duplicate requests and payments. Every decision is written to an audit log.

---

## 5. Outcome (be honest about the stage)

**Current stage:** design phase. Requirements, user stories and the database design are done; code is not written yet.

**Targets I set (from the PRD), which I will prove with tests:**
- 10 concurrent confirm requests for the same vehicle: exactly 1 succeeds.
- A repeated request with the same idempotency key never creates a duplicate.
- Allocation to 3 vendors in under 2 seconds (p95).
- Fairness measured as the share of orders going to the bottom 50% of vendors.

**What I will say now:** "I have completed the product and data design. The outcome I am working towards is a backend that I can prove is safe under concurrency, with automated tests."

**After building, replace with real results** (numbers, test output, screenshots):
```
Result (fill in later):
- Concurrency test: 10 threads, 1 success, 9 rejected
- Duplicate request test:
- Load test (k6): p95 =
- Pilot (if done): vendors =, bookings =
```

---

## 6. What I learned (add as you go)

- Writing the PRD and acceptance criteria first made the design decisions clearer.
- Safety needs both code and database rules (lock plus unique constraints).
- Scope control matters: I cut a very large idea to one verifiable MVP.
- Tooling lesson: the same `git push` can fail in the terminal and work in the IDE because of different network paths.

---

## 7. Answers to prepare

**1 minute version**
"I am building 4mulaShift, a relocation platform. The problem I picked is that existing moving platforms spam customers, concentrate work on a few vendors, and can double-book or double-charge under retries. My MVP gives each request to exactly 3 vendors using a fairness score, and makes booking safe with database row locking and idempotency. I followed a company-style process: PRD, user stories, ER design, then code with tests. I am currently at the design stage and the main proof will be a concurrency test."

**Questions to expect**
- Why this problem? (Use your real story from section 1.)
- Who are your users, and how do you know they have this problem? (Say honestly if it is from your own experience and not a survey.)
- What is different from existing platforms?
- What is the outcome so far? (Section 5, honestly.)
- What would you do if you had a team and 6 months?
- What is the biggest weakness? (Fairness formula: inactivity term can favour low-quality vendors; first-to-accept favours fast responders.)

## Self-check

1. Tell your origin story in 30 seconds without reading.
2. Name the 3 problems and their root causes.
3. What is your honest outcome right now?
4. What will you say if asked "did you validate this with real users?"