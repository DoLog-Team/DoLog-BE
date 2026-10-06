-- Persist file cleanup with the source deletion; retry storage failures after commit.
CREATE TABLE file_deletion_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    file_url TEXT NOT NULL
);
