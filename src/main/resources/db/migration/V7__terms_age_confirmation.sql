-- Existing agreements did not record age confirmation, so they require a new consent.
ALTER TABLE terms_agreements
    ADD COLUMN age_14_or_over_confirmed BIT(1) NOT NULL DEFAULT b'0';
