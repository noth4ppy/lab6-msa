-- Seed data (formerly data.sql): authors id=1, 2 and book id=1 by author id=2.
INSERT INTO authors (first_name, last_name, email)
VALUES ('Leo', 'Tolstoy', 'leo.tolstoy@example.com'),
       ('Fyodor', 'Dostoevsky', 'fyodor.dostoevsky@example.com');

INSERT INTO books (title, slug, description, published_year, author_id, created_at, updated_at)
VALUES ('Crime and Punishment', 'crime-and-punishment', 'A novel about Raskolnikov.', 1866,
        (SELECT id FROM authors WHERE email = 'fyodor.dostoevsky@example.com'),
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
