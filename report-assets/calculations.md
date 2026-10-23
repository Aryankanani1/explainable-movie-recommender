# Examples of calculations

Every number below was computed by the engine that ranks the films (the same classes as the app). Matrices show only the genres of the graded films and of the candidates shown.

## A. The lecture's worked example (slides), reproduced

Grades: Batman v Superman 2, Guardians of the Galaxy 10, Captain America: Civil War 8. Candidates: the four other films of the example.

### Step 1. Genre matrix x (1 = the film has the genre) and grades r

| Film | r | Comedy | Adventure | Super Hero | Sci-Fi | Drama |
|---|---:|---:|---:|---:|---:|---:|
| Batman v Superman | 2 | 0 | 1 | 1 | 0 | 0 |
| Guardians of the Galaxy | 10 | 1 | 1 | 1 | 1 | 0 |
| Captain America: Civil War | 8 | 1 | 0 | 1 | 0 | 0 |

### Step 2. Weighted genre matrix r · x, column sums P and profile p̂ = P / T

| Film | Comedy | Adventure | Super Hero | Sci-Fi | Drama |
|---|---:|---:|---:|---:|---:|
| Batman v Superman | 0 | 2 | 2 | 0 | 0 |
| Guardians of the Galaxy | 10 | 10 | 10 | 10 | 0 |
| Captain America: Civil War | 8 | 0 | 8 | 0 | 0 |
| **P** (column sums) | 18 | 12 | 20 | 10 | 0 |
| **p̂ = P / T** | 0.3000 | 0.2000 | 0.3333 | 0.1667 | 0.0000 |

T = Σ P = 60.

### Step 3. Scores: score(c) = Σ_g p̂_g · x_cg (each cell is p̂_g · x_cg)

| Film | Comedy | Adventure | Super Hero | Sci-Fi | Drama | Score | ×10 |
|---|---:|---:|---:|---:|---:|---:|---:|
| The Hitchhiker's Guide to the Galaxy | 0.3000 | 0.2000 | 0.0000 | 0.1667 | 0.0000 | 0.6667 | 6.67 |
| Spider-Man | 0.3000 | 0.0000 | 0.3333 | 0.0000 | 0.0000 | 0.6333 | 6.33 |
| Batman Begins | 0.0000 | 0.0000 | 0.3333 | 0.0000 | 0.0000 | 0.3333 | 3.33 |
| Lady Bird | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.00 |

## B. Persona 1 (Marvel fan who dislikes DC), lecture method

### Step 1. Genre matrix x (1 = the film has the genre) and grades r

| Film | r | Action | Adventure | Crime | Fantasy | IMAX | Sci-Fi | Thriller |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Iron Man | 10 | 1 | 1 | 0 | 0 | 0 | 1 | 0 |
| The Avengers | 9 | 1 | 1 | 0 | 0 | 1 | 1 | 0 |
| Man of Steel | 4 | 1 | 1 | 0 | 1 | 1 | 1 | 0 |
| Captain America: The Winter Soldier | 9 | 1 | 1 | 0 | 0 | 1 | 1 | 0 |
| Guardians of the Galaxy | 10 | 1 | 1 | 0 | 0 | 0 | 1 | 0 |
| Justice League | 3 | 1 | 1 | 0 | 0 | 0 | 1 | 0 |
| Thor: Ragnarok | 8 | 1 | 1 | 0 | 0 | 0 | 1 | 0 |
| Captain America: Civil War | 8 | 1 | 0 | 0 | 0 | 0 | 1 | 1 |
| Suicide Squad | 2 | 1 | 0 | 1 | 0 | 0 | 1 | 0 |
| Batman v Superman: Dawn of Justice | 2 | 1 | 1 | 0 | 1 | 0 | 1 | 0 |

### Step 2. Weighted genre matrix r · x, column sums P and profile p̂ = P / T

| Film | Action | Adventure | Crime | Fantasy | IMAX | Sci-Fi | Thriller |
|---|---:|---:|---:|---:|---:|---:|---:|
| Iron Man | 10 | 10 | 0 | 0 | 0 | 10 | 0 |
| The Avengers | 9 | 9 | 0 | 0 | 9 | 9 | 0 |
| Man of Steel | 4 | 4 | 0 | 4 | 4 | 4 | 0 |
| Captain America: The Winter Soldier | 9 | 9 | 0 | 0 | 9 | 9 | 0 |
| Guardians of the Galaxy | 10 | 10 | 0 | 0 | 0 | 10 | 0 |
| Justice League | 3 | 3 | 0 | 0 | 0 | 3 | 0 |
| Thor: Ragnarok | 8 | 8 | 0 | 0 | 0 | 8 | 0 |
| Captain America: Civil War | 8 | 0 | 0 | 0 | 0 | 8 | 8 |
| Suicide Squad | 2 | 0 | 2 | 0 | 0 | 2 | 0 |
| Batman v Superman: Dawn of Justice | 2 | 2 | 0 | 2 | 0 | 2 | 0 |
| **P** (column sums) | 65 | 55 | 2 | 6 | 22 | 65 | 8 |
| **p̂ = P / T** | 0.2915 | 0.2466 | 0.0090 | 0.0269 | 0.0987 | 0.2915 | 0.0359 |

T = Σ P = 223.

### Step 3. Scores: score(c) = Σ_g p̂_g · x_cg (each cell is p̂_g · x_cg)

| Film | Action | Adventure | Crime | Fantasy | IMAX | Sci-Fi | Thriller | Score | ×10 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| The Matrix Reloaded | 0.2915 | 0.2466 | 0.0000 | 0.0000 | 0.0987 | 0.2915 | 0.0359 | 0.9641 | 9.64 |
| The Matrix Revolutions | 0.2915 | 0.2466 | 0.0000 | 0.0000 | 0.0987 | 0.2915 | 0.0359 | 0.9641 | 9.64 |
| Iron Man 2 | 0.2915 | 0.2466 | 0.0000 | 0.0000 | 0.0987 | 0.2915 | 0.0359 | 0.9641 | 9.64 |
| Spider-Man 3 | 0.2915 | 0.2466 | 0.0000 | 0.0000 | 0.0987 | 0.2915 | 0.0359 | 0.9641 | 9.64 |
| G.I. Joe: Retaliation | 0.2915 | 0.2466 | 0.0000 | 0.0000 | 0.0987 | 0.2915 | 0.0359 | 0.9641 | 9.64 |

## C. Persona 1 (Marvel fan who dislikes DC), improved method

### Step 1. Centred weights w = r − r̄, with r̄ = 6.50

| Film | r | w |
|---|---:|---:|
| Iron Man | 10 | 3.50 |
| The Avengers | 9 | 2.50 |
| Man of Steel | 4 | -2.50 |
| Captain America: The Winter Soldier | 9 | 2.50 |
| Guardians of the Galaxy | 10 | 3.50 |
| Justice League | 3 | -3.50 |
| Thor: Ragnarok | 8 | 1.50 |
| Captain America: Civil War | 8 | 1.50 |
| Suicide Squad | 2 | -4.50 |
| Batman v Superman: Dawn of Justice | 2 | -4.50 |

### Step 2. Genre rarity: idf_g = ln(N / n_g)

|  | Action | Adventure | Crime | Drama | Fantasy | IMAX | Sci-Fi | Thriller |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| idf | 1.673 | 2.043 | 2.095 | 0.804 | 2.526 | 4.122 | 2.297 | 1.638 |

### Step 3. Normalized genre vectors ĝ (IDF-weighted, length 1)

| Film | Action | Adventure | Crime | Drama | Fantasy | IMAX | Sci-Fi | Thriller |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Iron Man | 0.478 | 0.584 | 0.000 | 0.000 | 0.000 | 0.000 | 0.656 | 0.000 |
| The Avengers | 0.309 | 0.378 | 0.000 | 0.000 | 0.000 | 0.762 | 0.425 | 0.000 |
| Man of Steel | 0.280 | 0.342 | 0.000 | 0.000 | 0.423 | 0.691 | 0.385 | 0.000 |
| Captain America: The Winter Soldier | 0.309 | 0.378 | 0.000 | 0.000 | 0.000 | 0.762 | 0.425 | 0.000 |
| Guardians of the Galaxy | 0.478 | 0.584 | 0.000 | 0.000 | 0.000 | 0.000 | 0.656 | 0.000 |
| Justice League | 0.478 | 0.584 | 0.000 | 0.000 | 0.000 | 0.000 | 0.656 | 0.000 |
| Thor: Ragnarok | 0.478 | 0.584 | 0.000 | 0.000 | 0.000 | 0.000 | 0.656 | 0.000 |
| Captain America: Civil War | 0.510 | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 0.700 | 0.499 |
| Suicide Squad | 0.474 | 0.000 | 0.593 | 0.000 | 0.000 | 0.000 | 0.651 | 0.000 |
| Batman v Superman: Dawn of Justice | 0.388 | 0.473 | 0.000 | 0.000 | 0.585 | 0.000 | 0.532 | 0.000 |

### Step 4. Weighted vectors w · ĝ and the profile p = Σ w · ĝ

| Film | Action | Adventure | Crime | Drama | Fantasy | IMAX | Sci-Fi | Thriller |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Iron Man | 1.673 | 2.043 | 0.000 | 0.000 | 0.000 | 0.000 | 2.297 | 0.000 |
| The Avengers | 0.774 | 0.945 | 0.000 | 0.000 | 0.000 | 1.906 | 1.062 | 0.000 |
| Man of Steel | -0.701 | -0.856 | 0.000 | 0.000 | -1.058 | -1.727 | -0.962 | 0.000 |
| Captain America: The Winter Soldier | 0.774 | 0.945 | 0.000 | 0.000 | 0.000 | 1.906 | 1.062 | 0.000 |
| Guardians of the Galaxy | 1.673 | 2.043 | 0.000 | 0.000 | 0.000 | 0.000 | 2.297 | 0.000 |
| Justice League | -1.673 | -2.043 | 0.000 | 0.000 | 0.000 | 0.000 | -2.297 | 0.000 |
| Thor: Ragnarok | 0.717 | 0.876 | 0.000 | 0.000 | 0.000 | 0.000 | 0.984 | 0.000 |
| Captain America: Civil War | 0.765 | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 1.050 | 0.749 |
| Suicide Squad | -2.133 | 0.000 | -2.670 | 0.000 | 0.000 | 0.000 | -2.927 | 0.000 |
| Batman v Superman: Dawn of Justice | -1.744 | -2.130 | 0.000 | 0.000 | -2.634 | 0.000 | -2.394 | 0.000 |
| **p** | 0.125 | 1.822 | -2.670 | 0.000 | -3.692 | 2.085 | 0.171 | 0.749 |

### Step 5. Scores: score(c) = cos(p, v_c) (each cell is p_g · v_cg / (‖p‖ · ‖v_c‖))

| Film | Action | Adventure | Crime | Drama | Fantasy | IMAX | Sci-Fi | Thriller | Score |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Captain Phillips | 0.0000 | 0.1396 | 0.0000 | 0.0000 | 0.0000 | 0.3222 | 0.0000 | 0.0460 | 0.5079 |
| Skyfall | 0.0075 | 0.1338 | 0.0000 | 0.0000 | 0.0000 | 0.3089 | 0.0000 | 0.0441 | 0.4944 |
| Mission: Impossible - Ghost Protocol | 0.0075 | 0.1338 | 0.0000 | 0.0000 | 0.0000 | 0.3089 | 0.0000 | 0.0441 | 0.4944 |
| Poseidon | 0.0075 | 0.1338 | 0.0000 | 0.0000 | 0.0000 | 0.3089 | 0.0000 | 0.0441 | 0.4944 |
| Apollo 13 | 0.0000 | 0.1479 | 0.0000 | 0.0000 | 0.0000 | 0.3415 | 0.0000 | 0.0000 | 0.4894 |
