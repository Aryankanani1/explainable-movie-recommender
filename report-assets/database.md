# Elements of the database

MovieLens ml-latest-small as imported: the year is split off each title and moved leading articles are put back ("Matrix, The" → "The Matrix"); tags are lowercased, trimmed and counted.

## Totals

| Item | Value |
|---|---:|
| Movies | 9,742 |
| Ratings (0.5–5.0 stars, half-star steps) | 100,836 |
| Users who rated | 610 |
| Ratings from – to | 1996-03-29 – 2018-09-24 |
| Genres | 19 |
| Movies with no genres listed | 34 |
| Tag applications | 3,683 |
| Distinct (movie, tag) pairs | 3,574 |
| Distinct tags | 1,475 |
| Tags on at least 2 movies (the tag vocabulary) | 542 |
| Movies with a vocabulary tag | 1,343 |

## The first 15 rows of movies.csv, parsed

| movieId | Title in movies.csv | Display title | Year | Genres |
|---:|---|---|---:|---|
| 1 | Toy Story (1995) | Toy Story | 1995 | Adventure, Animation, Children, Comedy, Fantasy |
| 2 | Jumanji (1995) | Jumanji | 1995 | Adventure, Children, Fantasy |
| 3 | Grumpier Old Men (1995) | Grumpier Old Men | 1995 | Comedy, Romance |
| 4 | Waiting to Exhale (1995) | Waiting to Exhale | 1995 | Comedy, Drama, Romance |
| 5 | Father of the Bride Part II (1995) | Father of the Bride Part II | 1995 | Comedy |
| 6 | Heat (1995) | Heat | 1995 | Action, Crime, Thriller |
| 7 | Sabrina (1995) | Sabrina | 1995 | Comedy, Romance |
| 8 | Tom and Huck (1995) | Tom and Huck | 1995 | Adventure, Children |
| 9 | Sudden Death (1995) | Sudden Death | 1995 | Action |
| 10 | GoldenEye (1995) | GoldenEye | 1995 | Action, Adventure, Thriller |
| 11 | American President, The (1995) | The American President | 1995 | Comedy, Drama, Romance |
| 12 | Dracula: Dead and Loving It (1995) | Dracula: Dead and Loving It | 1995 | Comedy, Horror |
| 13 | Balto (1995) | Balto | 1995 | Adventure, Animation, Children |
| 14 | Nixon (1995) | Nixon | 1995 | Drama |
| 15 | Cutthroat Island (1995) | Cutthroat Island | 1995 | Action, Adventure, Romance |

## Genres, with how many movies have each and their IDF weight

| Genre | Movies | idf = ln(N / n) |
|---|---:|---:|
| Action | 1,828 | 1.673 |
| Adventure | 1,263 | 2.043 |
| Animation | 611 | 2.769 |
| Children | 664 | 2.686 |
| Comedy | 3,756 | 0.953 |
| Crime | 1,199 | 2.095 |
| Documentary | 440 | 3.097 |
| Drama | 4,361 | 0.804 |
| Fantasy | 779 | 2.526 |
| Film-Noir | 87 | 4.718 |
| Horror | 978 | 2.299 |
| IMAX | 158 | 4.122 |
| Musical | 334 | 3.373 |
| Mystery | 573 | 2.833 |
| Romance | 1,596 | 1.809 |
| Sci-Fi | 980 | 2.297 |
| Thriller | 1,894 | 1.638 |
| War | 382 | 3.239 |
| Western | 167 | 4.066 |

## The 10 most-applied tags

| Tag | Applications | Movies |
|---|---:|---:|
| in netflix queue | 131 | 131 |
| atmospheric | 41 | 37 |
| funny | 24 | 22 |
| superhero | 24 | 22 |
| surreal | 24 | 22 |
| thought-provoking | 24 | 19 |
| disney | 23 | 22 |
| sci-fi | 23 | 19 |
| quirky | 22 | 22 |
| religion | 22 | 22 |

## Citation

F. Maxwell Harper and Joseph A. Konstan. 2015. The MovieLens Datasets: History and Context. ACM Transactions on Interactive Intelligent Systems (TiiS) 5, 4: 19:1–19:19. https://doi.org/10.1145/2827872
