CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS statements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accountId VARCHAR(64) NOT NULL,
    period VARCHAR(7) NOT NULL,
    objectKey VARCHAR(256) NOT NULL,
    checksumSha256 VARCHAR(128) NOT NULL,
    sizeBytes BIGINT NOT NULL,
    createdAt TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploadedBy VARCHAR(64) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_statements_account_period ON statements (accountId, period);

CREATE TABLE IF NOT EXISTS download_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statementId UUID NOT NULL REFERENCES statements(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    expiresAt TIMESTAMPTZ NOT NULL,
    createdBy VARCHAR(64) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_download_token_token ON download_tokens (token);

CREATE TABLE IF NOT EXISTS download_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statementId UUID NOT NULL REFERENCES statements(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL,
    downloadedAt TIMESTAMPTZ NOT NULL DEFAULT now(),
    userId VARCHAR(64),
    ip VARCHAR(45),
    userAgent VARCHAR(256)
);
CREATE INDEX IF NOT EXISTS idx_download_audits_statement ON download_audits (statementId);
