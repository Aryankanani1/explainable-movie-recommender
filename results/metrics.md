# Offline evaluation

MovieLens ml-latest-small: 610 users, 9742 movies, 100836 ratings. Per user, a seeded 80/20 split (new Random(42 + userId)); a hidden movie is relevant when rated ≥ 4.0; K = 10. Popularity counts come from train ratings only.

| Method | Users | Skipped | P@10 | R@10 | NDCG@10 | HR@10 | Coverage | ILD | Novelty |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| random | 602 | 8 | 0.0018 | 0.0011 | 0.0023 | 0.0183 | 0.4691 | 0.7616 | 7.18 |
| popularity | 602 | 8 | 0.1201 | 0.0989 | 0.1590 | 0.5664 | 0.0060 | 0.6940 | 1.66 |
| lecture | 602 | 8 | 0.0050 | 0.0039 | 0.0055 | 0.0465 | 0.0204 | 0.3016 | 7.07 |
| cb-cosine | 602 | 8 | 0.0150 | 0.0129 | 0.0183 | 0.1179 | 0.0773 | 0.1091 | 5.83 |
| cb-centered | 602 | 8 | 0.0249 | 0.0137 | 0.0333 | 0.1645 | 0.2206 | 0.0692 | 5.17 |
| cb-idf | 602 | 8 | 0.0239 | 0.0124 | 0.0318 | 0.1611 | 0.2231 | 0.0931 | 5.39 |
| cb-idf-mmr | 602 | 8 | 0.0244 | 0.0125 | 0.0320 | 0.1661 | 0.2210 | 0.1030 | 5.37 |
| cb-tags | 602 | 8 | 0.0118 | 0.0062 | 0.0152 | 0.0980 | 0.2135 | 0.0970 | 5.90 |
| cb-tags-tuned | 602 | 8 | 0.0239 | 0.0124 | 0.0318 | 0.1611 | 0.2231 | 0.0931 | 5.39 |
| cb-tags-mmr | 602 | 8 | 0.0120 | 0.0063 | 0.0152 | 0.0997 | 0.2115 | 0.1074 | 5.87 |
| cb-tags-pop | 602 | 8 | 0.0204 | 0.0143 | 0.0243 | 0.1711 | 0.1850 | 0.1296 | 4.96 |

## β search for cb-tags (validation split, 573 users)

| β | NDCG@10 on validation |
|---:|---:|
| 0 | 0.0135 ← chosen |
| 0.1 | 0.0074 |
| 0.25 | 0.0076 |
| 0.5 | 0.0076 |
| 0.75 | 0.0084 |
| 1 | 0.0094 |

cb-tags-tuned uses β = 0, chosen on validation, then evaluated once on test.

## Paired bootstrap, NDCG@10 difference (1000 resamples of users, seed 42)

| Comparison | Mean difference | 95% CI | Excludes 0? |
|---|---:|---|---|
| cb-tags − lecture | +0.0097 | [+0.0049, +0.0147] | yes |
| cb-idf − lecture | +0.0264 | [+0.0193, +0.0340] | yes |
| cb-idf − cb-centered | -0.0015 | [-0.0067, +0.0037] | no |
| cb-tags − cb-idf | -0.0166 | [-0.0226, -0.0113] | yes |
| cb-tags-mmr − cb-tags | +0.0000 | [-0.0002, +0.0003] | no |
| cb-idf-mmr − cb-idf | +0.0002 | [-0.0002, +0.0007] | no |
| popularity − cb-idf | +0.1272 | [+0.1118, +0.1416] | yes |
