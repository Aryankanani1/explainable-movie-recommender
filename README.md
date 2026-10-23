# Explainable Movie Recommender

A content-based movie recommender built with Java and Spring Boot on the MovieLens dataset. You grade movies you know; it builds a taste profile from their genres and tags, recommends films you haven't seen, and explains each pick. It can also show the full matrix calculation behind a recommendation.

> Course project for Recommender Systems, Florida Atlantic University (Prof. Borko Furht), Fall 2026. Work in progress; this README grows with the project.

![Recommendations with explanations](docs/screenshots/2-recommendations.png)

## Run locally
Requirements: JDK 21 and Docker (for PostgreSQL).
```bash
./mvnw spring-boot:run      # starts Postgres, migrates and imports MovieLens on first start
./mvnw verify               # all tests (integration tests use Testcontainers)
```
Then open http://localhost:8080. Postgres from `compose.yaml` listens on host port 5433. For trying it out, `application.yaml` seeds a `demo` / `demo` account (`recsys.demo-user.enabled`; turn it off for anything public).

Offline evaluation and report assets:
```bash
./mvnw -q compile exec:java                                                               # writes results/
./mvnw -q compile exec:java -Dexec.mainClass=edu.fau.recsys.evaluation.ReportAssetsRunner # writes report-assets/
```

## How it works
- **Lecture method:** the slide example (grade matrix → genre profile → weighted scores), reproduced exactly; see "Show the math" in the app.
- **Improved method:** mean-centred grades, IDF-weighted genres and cosine similarity, with an optional tag block and MMR re-ranking for diversity.
- **Evaluation:** seeded 80/20 per-user split, Precision/Recall/NDCG/HR@10, coverage, diversity (ILD) and novelty against random and popularity baselines. Results: [`results/metrics.md`](results/metrics.md).
- **Report assets:** worked calculations, personas and tables in [`report-assets/`](report-assets/); screenshots in [`docs/screenshots/`](docs/screenshots/).

## Data
This project uses the MovieLens `ml-latest-small` dataset (100,836 ratings and 3,683 tag applications across 9,742 movies, by 610 users), redistributed in `data/ml-latest-small/` under its original license (see the README in that folder). The data is used for research and education only, not for commercial use. This project is not endorsed by the University of Minnesota or GroupLens.

> F. Maxwell Harper and Joseph A. Konstan. 2015. The MovieLens Datasets: History and Context. *ACM Transactions on Interactive Intelligent Systems (TiiS)* 5, 4: 19:1–19:19. https://doi.org/10.1145/2827872

## AI assistance
Development used AI assistance (Anthropic's Claude) for architecture review, code review, test design and teaching. The author wrote and can explain the recommendation engine and evaluation code.

## License
Code: [MIT](LICENSE). Data: MovieLens license (see above).
