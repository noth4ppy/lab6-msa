-- Schema of the existing entities (model/Author.java, model/Book.java).
-- Column types match the JPA mapping so that spring.jpa.hibernate.ddl-auto=validate passes.
CREATE TABLE authors (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    email      VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_authors_email UNIQUE (email)
);

CREATE TABLE books (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(255),
    slug           VARCHAR(255),
    description    TEXT,
    published_year INT,
    author_id      BIGINT       NOT NULL,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_books_slug UNIQUE (slug),
    CONSTRAINT fk_books_author FOREIGN KEY (author_id) REFERENCES authors (id)
);
