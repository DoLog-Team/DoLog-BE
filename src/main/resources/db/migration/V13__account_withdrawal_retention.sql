-- V10~V12 are reserved by notification and exhibition-zone branches.
-- Retain withdrawn accounts and content for three calendar months.
ALTER TABLE accounts ADD COLUMN withdrawn_at DATETIME(6) NULL;
ALTER TABLE artists ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE artist_profiles ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE artworks ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE artwork_artist_maps ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE bts ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE bts_artwork_map ADD COLUMN deleted_at DATETIME(6) NULL;
CREATE INDEX idx_accounts_withdrawn_at ON accounts (withdrawn_at);
CREATE INDEX idx_artworks_deleted_at ON artworks (deleted_at);
CREATE INDEX idx_bts_deleted_at ON bts (deleted_at);
