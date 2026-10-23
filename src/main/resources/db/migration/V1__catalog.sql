-- MovieLens catalog. Movie IDs are the MovieLens movieId (assigned, never generated).

CREATE TABLE movie (
    id            integer PRIMARY KEY,
    title         text          NOT NULL,
    display_title text          NOT NULL,
    year          smallint,
    tmdb_id       integer,
    rating_count  integer       NOT NULL DEFAULT 0,
    rating_mean   numeric(3, 2)
);

CREATE TABLE genre (
    id   smallint PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE movie_genre (
    movie_id integer  NOT NULL REFERENCES movie (id),
    genre_id smallint NOT NULL REFERENCES genre (id),
    PRIMARY KEY (movie_id, genre_id)
);

-- Tags are lowercased, trimmed and aggregated: count = number of applications of the tag to the movie.
CREATE TABLE movie_tag (
    movie_id integer NOT NULL REFERENCES movie (id),
    tag      text    NOT NULL,
    count    integer NOT NULL,
    PRIMARY KEY (movie_id, tag)
);

-- MovieLens users only (0.5-5.0 scale); app users' grades live in a separate table.
CREATE TABLE ml_rating (
    user_id  integer       NOT NULL,
    movie_id integer       NOT NULL REFERENCES movie (id),
    rating   numeric(2, 1) NOT NULL,
    rated_at timestamptz   NOT NULL,
    PRIMARY KEY (user_id, movie_id)
);
