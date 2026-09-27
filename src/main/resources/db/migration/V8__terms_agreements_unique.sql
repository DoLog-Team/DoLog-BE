-- 같은 계정이 같은 약관 버전에 중복 동의 행을 쌓지 않도록 막는다. 재시도·동시 요청 대비.
-- 제약 추가 전, 이미 쌓인 중복은 가장 최근 동의 행 하나만 남기고 정리한다.
DELETE t FROM terms_agreements t
JOIN terms_agreements newer
  ON newer.account_id = t.account_id
 AND newer.terms_version = t.terms_version
 AND (newer.agreed_at > t.agreed_at OR (newer.agreed_at = t.agreed_at AND newer.id > t.id));

ALTER TABLE terms_agreements
    ADD CONSTRAINT uk_terms_agreements_account_version UNIQUE (account_id, terms_version);
