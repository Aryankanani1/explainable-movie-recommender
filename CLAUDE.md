# Explainable Movie Recommender

A content-based movie recommender for FAU's Recommender Systems course (Prof. Borko Furht), Fall 2026.
- The user grades movies they know (1–10).
- The system builds a genre (and optional tag) taste profile and recommends unseen movies, with an explanation for each one.
- It can show the professor's matrix calculation for the user's actual grades.
- Data: MovieLens `ml-latest-small`, in `data/`.

**Live:** https://explainable-movie-recommender.onrender.com · **Code:** https://github.com/Aryankanani1/explainable-movie-recommender (public)

**Submission:** target **Mon Nov 2, 2026**; the official deadline is **Nov 7**. The professor requires the program, a report and a PowerPoint (details under "What's next").

## Status (as of 2026-10-23)
All ten tickets are done and merged into `main`, which is the only branch, locally and on GitHub. CI is green: 163 unit tests, 5 Testcontainers integration tests, plus a Docker job that runs `docker compose up` from a clean checkout and the smoke test.

| Ticket | What it delivered |
|---|---|
| 01 | Flyway schema; MovieLens import (9,742 movies, 100,836 ratings, tags) |
| 02 | `LectureRecommender` (the slides' method, exact golden values); grading |
| 03 | `ContentBasedRecommender`: centred grades, IDF, cosine |
| 04 | Tag block (TF-IDF, weight β), explanations, `MathTrace` (show the math) |
| 05 | Offline evaluation → `results/` (seeded 80/20 split, P/R/NDCG/HR@10, coverage, ILD, novelty, β search, paired bootstrap) |
| 06 | REST API and vanilla-JS UI (grade, recommendations, show the math, lecture example) |
| 07 | MMR diversity re-ranking (slider) and the onboarding starter set |
| 08 | Spring Security (session login, registration, BCrypt, CSRF), `@LogLatency` aspect, integration tests |
| 09 | Dockerfile, full-stack compose, `render.yaml`, CI, smoke test, README showcase, live deployment |
| 10 | Report assets → `report-assets/` (personas, recommendations, calculations, database excerpts, metrics, chart data) |

Decisions made along the way:
- **Tags are off in the app (β = 0).** The validation search picked β = 0, because tags at β = 0.5 lowered NDCG. Set by `recsys.recommend.tag-weight: 0.0`.
- **The evaluation's MMR rows use λ = 0.7, with a candidate pool of 100.**
- **With fewer than 5 grades, recommendations fall back to popular films**, and the UI shows a banner.
- **The `demo` / `demo` account** is on in `application.yaml` for local use and off on the live site.
- **Local Postgres uses host port 5433,** because a native PostgreSQL 18 owns 5432 on the dev machine.

## What's next
**For the developer to do. Claude helps with assets, not prose to submit.**
1. **Write the report.** The brief requires:
   - several examples of searches and recommendations (`report-assets/personas.md`, `recommendations.md`)
   - some elements of the database (`report-assets/database.md`)
   - some examples of calculations (`report-assets/calculations.md`, plus "Show the math" in the app)
   - the program itself (the GitHub link, the live link, code excerpts)
   - an AI-assistance disclosure, as the README has

   Use real movie names, and present the final recommendation results.
2. **Make the PowerPoint.** Use the screenshots in `docs/screenshots/`, the chart data in `report-assets/metrics-chart.csv` and `mmr-sweep.csv`, and the results table.
3. **Rewrite the README's results paragraph** (under the table) in your own words, since it overlaps with the report.
4. **Check the live site on a phone** (register, grade, recommend), then do a dry run of the presentation.
5. **Submit** the report, the PowerPoint and the program (GitHub and live links) by Nov 2 if possible, and by Nov 7 at the latest.

**Material for the report** (facts from `results/` and `report-assets/`; the interpretation is yours):
- **Accuracy:** popularity is far ahead (NDCG@10 0.159, against 0.032 for the improved method), but it covers only 0.6% of the catalogue.
- **Improved vs lecture:** the improved method beats the lecture method by +0.0264 NDCG, and the bootstrap 95% confidence interval excludes 0. It also covers about 10× more of the catalogue.
- **IDF:** no measurable change in accuracy, but higher diversity (ILD 0.069 → 0.093).
- **MMR:** at λ = 0.7 it raises ILD by about 10% at no measurable cost. The λ sweep reaches 0.152 at λ = 0.3.
- **Persona 1 (Marvel fan) under the improved method:** gets IMAX-driven picks (Captain Phillips, Skyfall, Apollo 13), because centring cancels Action and Sci-Fi and IMAX remains.
- **Tags:** the top tag is "in netflix queue", a workflow tag. That is one reason tags hurt accuracy.
- **Hand-check example:** `report-assets/calculations.md` §B, Action column: P = 65, p̂ = 65/223 = 0.2915.

**Optional, if time allows:**
- The starter-pack docs (ARCHITECTURE, ALGORITHMS, EVALUATION, DATASET, tickets) live outside the repo in `../../Desktop/Advanced_Internet_Of_Things/Recommendation_system/recsys-claude-starter-pack/`. Copying them in would need these corrections:
  - port 5433
  - `application.yaml`, not `.yml`
  - the β = 0 default
  - `CatalogSnapshot.Builder.genres`
  - the MathTrace lecture/content split
  - `runtimeMs` in `run-info.json`
  - the `compile` step in the exec commands
- **Never publish `docs/ASSIGNMENT.md`**, because it names other students' projects.

## Commands
- **Run:** `./mvnw spring-boot:run`. Compose starts only Postgres (host port 5433). The first start imports MovieLens. The app runs on 8080; if 8080 is taken, add `-Dspring-boot.run.arguments=--server.port=8090`.
- **All tests:** `./mvnw -q verify` (needs Docker for Testcontainers). For one test: `./mvnw -q -Dtest=LectureRecommenderTest test`.
- **Full stack in Docker:** `docker compose up --build` (the app is capped at 512 MB, like the free host).
- **Offline evaluation:** `./mvnw -q compile exec:java` writes `results/`. Regenerate it in place; deleting `results/` first marks `run-info.json` as "-dirty".
- **Report assets:** `./mvnw -q compile exec:java -Dexec.mainClass=edu.fau.recsys.evaluation.ReportAssetsRunner` writes `report-assets/`. The output is byte-identical across runs.
- **API smoke test:** `node scripts/smoke-test.mjs <base-url>`. Each run against the live site leaves a `smoke-<timestamp>` account behind. Clean up in Neon's SQL Editor: delete from `grade`, then from `app_user`, `WHERE username LIKE 'smoke-%'`. Never delete real users' accounts, including the developer's own.
- **UI end-to-end and screenshots:** `node scripts/ui-e2e.mjs http://localhost:8080 docs/screenshots`. It needs Chrome and the demo account, and it replaces demo's grades.
- **Reset the local database:** `docker compose down -v`.
- After editing static files while the app is running: `./mvnw -q process-resources`.

## Deployment
- **Render (free web service):** built from `render.yaml` and the `Dockerfile`, region Virginia. It deploys `main` automatically on every push.
- **Neon (free Postgres):** AWS us-east-1, database `neondb`.
- **Environment variables, set only in Render's dashboard, never in the repo or chat:**
  - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<neon-direct-host>/neondb?sslmode=require`. Use the host without `-pooler`, with no user or password in the URL.
  - `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - `RECSYS_DEMO_USER_ENABLED=false`, `SERVER_FORWARD_HEADERS_STRATEGY=native`
- **Startup:** the free instance sleeps after 15 idle minutes and takes about 1–2 minutes to wake. Spring startup alone takes about 30 s on it.
- **When a deploy fails, read Render's Logs:** the last `Caused by:` line names the problem. Past failures were all configuration mistakes: a placeholder host, a non-JDBC URL, a wrong password, and the service suspended from the dashboard.

## Package map (`edu.fau.recsys`)
| Package | Contents | Spring allowed? |
|---|---|---|
| `dataset` | MovieLens CSV readers, `TitleParser`, plain records | No |
| `engine` | catalog snapshot, vectors, recommenders, MMR, explanations, math trace | **No (pure Java)** |
| `evaluation` | split, metrics, `EvaluationRunner`, `ReportAssetsRunner` | No |
| `catalog` | JPA entities, `MovieLensImporter`, `CatalogSnapshotFactory`, `StarterSetService` | Yes |
| `grading` | `AppUser`, `Grade`, `GradeService`, `AccountService`, `CurrentUserProvider` | Yes |
| `web` | REST controllers, DTOs, `ProblemDetail` handling | Yes |
| `infra` | `RecsysProperties`, `SecurityConfig`, `@LogLatency` aspect | Yes |

## Architecture rules (non-negotiable)
1. `dataset`, `engine` and `evaluation` never import Spring or Jakarta. `ArchitectureTest` enforces this.
2. Scoring runs in memory against an immutable `CatalogSnapshot` built at startup. Never query the database per movie.
3. Results are deterministic. Ties break by score desc, then ratingCount desc, then movieId asc. Evaluation uses seed 42.
4. The engine never sees JPA entities; `catalog` adapts database rows into engine types.
5. Bulk loads use `JdbcTemplate` batch updates. Movie IDs are MovieLens IDs (assigned), never `IDENTITY`.
6. App grades are integers 1–10; MovieLens ratings are 0.5–5.0. `UserRatings` carries its own `RatingScale`.
7. Implement formulas exactly as written in the starter pack's ALGORITHMS.md. If something seems wrong, stop and ask.

## Workflow
- Before writing code, post a short plan (files, public types, tests) and wait for approval. Tests come first in `engine` and `evaluation`.
- One branch per change (`feat/…`, `chore/…`). `./mvnw -q verify` must be green before proposing a commit. **Don't commit, push or merge unless asked**, and remember that a push to `main` deploys the live site.
- Commits use the GitHub no-reply email: the repo-local `user.email` is `156785356+Aryankanani1@users.noreply.github.com`. The global git config still has a personal address, so don't use it here.
- **Never commit or paste secrets.** Config comes from environment variables, with local defaults in `application.yaml`.

## Working with the developer
- The developer is an advanced Java/Spring engineer and new to recommender systems. Skip Spring basics; explain the recommender math.
- This is graded coursework and a CV project, so the developer must be able to explain every line of `engine` and `evaluation`.
  - Explain the approach with the lecture example numbers (grades 2/10/8) before coding.
  - Prefer plain loops and variables named after the math symbols (`P`, `T`, `pHat`, `w`, `p`, `v`, `idf`).
- **The report and slides are the developer's to write.** Help by generating assets (tables, example outputs, chart data), not prose to submit.
- AI assistance is disclosed in the README and must be disclosed in the report.

## Dev machine gotchas
- **Disk space:** the disk has run full repeatedly, and Docker Desktop's disk image (about 16 GB) is the usual cause. Check with `df -h /` before building images. Docker builds are verified in CI rather than locally.
- **Docker Desktop stops on its own at times.** The integration tests then fail with a Flyway/datasource error; run `open -a Docker` and run them again.
- **Open Claude Code in this folder** (`~/Projects/Recommendation_system/movie-recommender`), not the old Desktop copy, because iCloud evicted files there.

## Code style
- Records for immutable data; constructor injection only; no Lombok in `dataset`, `engine` or `evaluation`.
- Package-private by default. Compare doubles in tests with a tolerance of `1e-4`.
- Log with SLF4J. No `System.out` outside the runners in `evaluation`.
