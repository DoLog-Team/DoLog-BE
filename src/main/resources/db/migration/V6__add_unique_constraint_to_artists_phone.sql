ALTER TABLE artists
    ADD CONSTRAINT uk_artists_phone UNIQUE (phone);