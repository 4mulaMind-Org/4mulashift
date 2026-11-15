# Troubleshooting Log

Every real problem hit during the project: symptom, cause, fix, lesson. This is also source material for interview answers ("tell me about a bug you debugged").

Status: **Fixed**, **Open**, or **Ignored (harmless)**.

---

## TS-001: `git push` from the terminal timed out
- **Date:** 2026-11-07 | **Status:** Fixed (workaround)
- **Symptom:** `fatal: unable to access 'https://github.com/...': Recv failure: Operation timed out`
- **Cause:** Network path from the terminal to GitHub timed out. It was not a login problem. The commit itself was safe locally.
- **Fix:** Pushed from IntelliJ (Cmd + Shift + K), which worked.
- **Other options noted:** try a mobile hotspot, `curl -I https://github.com`, `git -c http.version=HTTP/1.1 push`, or switch the remote to SSH.
- **Lesson:** A timeout is a network problem, not an authentication problem. Try another path before debugging credentials.

## TS-002: Extra character in the project folder name
- **Date:** 2026-11-07 | **Status:** Fixed
- **Symptom:** Project name and path showed `4mulashift‹` in IntelliJ.
- **Cause:** A stray character was typed into the Directory field of the clone dialog.
- **Fix:** Closed the project, renamed the folder in Finder to `4mulashift`, reopened it before the first commit.
- **Lesson:** Read the directory field before pressing Clone. Fix naming problems before anything is committed.

## TS-003: README text pasted into the terminal
- **Date:** 2026-11-07 | **Status:** Fixed
- **Symptom:** `zsh: command not found: Fair,` and similar errors.
- **Cause:** File content was pasted into the terminal instead of the editor, so the shell tried to run it as commands.
- **Fix:** Edited `README.md` in the editor. Nothing was damaged.
- **Lesson:** Terminals run commands; files are edited in the editor.

## TS-004: `PRD.md` created in the wrong folder, Git status looked confusing
- **Date:** 2026-11-07 | **Status:** Fixed
- **Symptom:** `git status` showed `PRD.md` both as a new staged file and as deleted.
- **Cause:** IntelliJ's "Add to Git" staged the file at the repo root. I then moved it into `docs/`, so Git saw a deletion of the old path.
- **Fix:** `git add .` restaged the final layout; the commit showed `docs/PRD.md`.
- **Lesson:** Staged and unstaged states are separate. Run `git status` before committing.

## TS-005: Design gap, double-booking index did not cover pending bookings
- **Date:** during API design | **Status:** Fixed in design (apply to `er-diagram.md` and migrations)
- **Symptom:** Found while writing the API spec. Accepting an offer creates a `PENDING_PAYMENT` booking first, but the partial unique index covered only `CONFIRMED`.
- **Cause:** Two customers could both reach payment for the same vehicle and day because a pending booking did not hold the slot.
- **Fix:** The partial unique index on `(vehicle_id, slot_date)` must apply to status `PENDING_PAYMENT` and `CONFIRMED`. Unpaid bookings expire after a payment window (15 minutes, assumption) and become `CANCELLED`, which releases the slot.
- **Lesson:** Review each new document against the earlier ones. A correct design on paper can still have a hole between two documents.

## TS-006: `com.4mulashift` is not a valid Java package
- **Date:** 2026-11-11 | **Status:** Fixed
- **Symptom:** Spring Initializr form defaulted to `com.4mulashift`.
- **Cause:** Java identifiers cannot start with a digit.
- **Fix:** Used `com.mulashift` for group and package. The product brand stays "4mulaShift".
- **Lesson:** Code names and brand names can differ. Decide package names before generating the project, since renaming later touches every file.

## TS-007: `docker compose` said "no configuration file provided: not found"
- **Date:** 2026-11-15 | **Status:** Fixed
- **Symptom:** Compose could not find the file even though it was in the repo root.
- **Cause:** `ls -la` showed the file name was `docker-compose.yml ` with a trailing space.
- **Fix:** `mv "docker-compose.yml " docker-compose.yml`.
- **Lesson:** When a tool cannot find a file, check the exact name with `ls -la`. Invisible characters matter.

## TS-008: Port 8080 was already in use
- **Date:** 2026-11-15 | **Status:** Fixed
- **Symptom:** `APPLICATION FAILED TO START: Web server failed to start. Port 8080 was already in use.` The Flyway migration had already been applied before the web server failed.
- **Cause:** The app from the previous run was still alive (PID 32573, visible in the earlier startup log). A health check I thought came from the new run had been answered by the old one.
- **Fix:** `lsof -i :8080`, then `kill 32573`. The `Google` entries in `lsof` were closed Chrome connections and were left alone.
- **Lesson:** Stop the old process before restarting. A passing health check does not prove the new build is the one running. Check the PID in the startup log.

## TS-009: IntelliJ Run button grey, Maven project not loaded
- **Date:** 2026-11-15 | **Status:** Fixed
- **Symptom:** Run button disabled, `src/main/java` not marked as a source root, no Maven panel.
- **Cause:** The IntelliJ project root is `4mulashift`, but `pom.xml` is in `services/backend`, so IntelliJ did not detect it as a Maven project.
- **Fix:** Right-click `services/backend/pom.xml` and choose Add as Maven Project (alternative: open `services/backend` as its own project). Also run from `BackendApplication.java`, not from `pom.xml`.
- **Lesson:** The IDE's project root and the build root can differ in a monorepo. The terminal worked the whole time, so the build was fine; only the IDE setup was wrong.

## TS-010: `.env` not found when running from IntelliJ
- **Date:** 2026-11-15 | **Status:** Fix identified (apply the change below)
- **Symptom:** `FATAL: password authentication failed for user "${POSTGRES_USER}"` (the user name was the literal text, not a value).
- **Cause:** `application.properties` imported `optional:file:../../.env[.properties]`, which works when the app starts in `services/backend`. IntelliJ started it in the repo root (startup log: `started ... in .../4mulashift`), so the relative path pointed outside the repo. `optional:` made Spring ignore the missing file silently, so the placeholder stayed unresolved.
- **Fix:** Import both locations:
  `spring.config.import=optional:file:../../.env[.properties],optional:file:./.env[.properties]`
- **Lesson:** Relative paths depend on the working directory. `optional:` turns a loud error into a silent one, so verify that optional config was actually loaded. The earlier terminal run worked only because of the directory it was started from.

## TS-011: Runtime JDK is 27, code targets 21
- **Date:** 2026-11-15 | **Status:** Open
- **Symptom:** Startup log says `using Java 27` and shows Lombok `sun.misc.Unsafe` warnings, while the compiler targets release 21.
- **Cause:** The default JDK on the machine is 27, which is not an LTS release.
- **Planned fix:** IntelliJ, File, Project Structure, SDK: choose JDK 21. Make the terminal use 21 as well and confirm with `java -version`.
- **Lesson:** Run and build on the same LTS JDK that you target.

## TS-012: Warnings that were checked and ignored
- **Date:** 2026-11-15 | **Status:** Ignored (harmless)
- Maven `HTTP Status: 401` for `flyway-community-db-support` and `MavenFeed`: optional extra repositories that need login; build is unaffected.
- Netty `MacOSDnsServerAddressStreamProvider` not found: optional macOS DNS helper; falls back to system defaults.
- Lombok `sun.misc.Unsafe` warning: a Lombok and newer JDK message; goes away or is reduced on JDK 21.
- Reclazz hot-reload plugin `WARN` lines: IDE plugin messages, not application errors.
- **Lesson:** Not every warning needs action, but check each one once and write down why it is safe.

---

## How to add an entry

1. Copy the format above (symptom, cause, fix, lesson).
2. Give it the next TS number.
3. Reference it from `dev-log.md` and, if it makes a good story, from the interview notes.