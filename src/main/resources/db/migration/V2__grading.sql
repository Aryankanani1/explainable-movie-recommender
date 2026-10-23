-- App users and their grades (1-10, the lecture's scale). Never mixed with MovieLens ratings.

CREATE TABLE app_user (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      text        NOT NULL UNIQUE,
    password_hash text,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE grade (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    bigint      NOT NULL REFERENCES app_user (id),
    movie_id   integer     NOT NULL REFERENCES movie (id),
    score      smallint    NOT NULL CHECK (score BETWEEN 1 AND 10),
    updated_at timestamptz NOT NULL,
    UNIQUE (user_id, movie_id)
);
