-- Tre tipi di accesso:
--   admin     → admin_accounts (invariato)
--   utente    → user_accounts, role = 'user'
--   attività  → user_accounts, role = 'merchant', collegato a una scheda in company_profiles

CREATE TABLE user_accounts (
    id                UUID         PRIMARY KEY,
    email             VARCHAR(254) NOT NULL UNIQUE,         -- sempre minuscola
    password_hash     VARCHAR(100) NOT NULL,                -- BCrypt
    role              VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'merchant')),
    first_name        VARCHAR(80)  NOT NULL,
    last_name         VARCHAR(80)  NOT NULL,
    status            VARCHAR(30)  NOT NULL CHECK (status IN ('pending_verification', 'active', 'suspended')),
    email_verified_at TIMESTAMPTZ,
    terms_version     VARCHAR(40)  NOT NULL,
    terms_accepted_at TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    last_login_at     TIMESTAMPTZ
);

CREATE TABLE account_sessions (
    token_hash VARCHAR(64) PRIMARY KEY,                     -- SHA-256 del token: il token non è salvato
    account_id UUID        NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_account_sessions_account ON account_sessions (account_id);
CREATE INDEX idx_account_sessions_expiry  ON account_sessions (expires_at);

-- Link monouso per verifica email e recupero password
CREATE TABLE account_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    account_id UUID        NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    purpose    VARCHAR(20) NOT NULL CHECK (purpose IN ('verify_email', 'reset_password')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ
);
CREATE INDEX idx_account_tokens_account ON account_tokens (account_id, purpose);

-- Scheda attività collegata all'account che l'ha registrata e stato della verifica
ALTER TABLE company_profiles
    ADD COLUMN owner_account_id     UUID REFERENCES user_accounts (id) ON DELETE SET NULL,
    ADD COLUMN registration_status  VARCHAR(20) NOT NULL DEFAULT 'approved'
        CHECK (registration_status IN ('pending', 'approved', 'rejected')),
    ADD COLUMN registration_at      TIMESTAMPTZ,
    ADD COLUMN reviewed_at          TIMESTAMPTZ,
    ADD COLUMN review_note          TEXT NOT NULL DEFAULT '',
    ADD COLUMN terms_version        VARCHAR(40),
    ADD COLUMN terms_accepted_at    TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_company_owner ON company_profiles (owner_account_id) WHERE owner_account_id IS NOT NULL;
-- Una partita IVA non può avere due registrazioni attive (le rifiutate non contano)
CREATE UNIQUE INDEX uq_company_vat ON company_profiles (vat_number)
    WHERE vat_number <> '' AND registration_status <> 'rejected';
CREATE INDEX idx_company_registration ON company_profiles (registration_status);

-- Le adesioni alle attività possono essere collegate all'account dell'utente (seguono l'utente su ogni dispositivo)
ALTER TABLE merchant_clients ADD COLUMN account_id UUID UNIQUE REFERENCES user_accounts (id) ON DELETE CASCADE;

-- L'accesso degli esercenti non avviene più con link monouso generati dall'admin, ma con il proprio account.
DROP TABLE merchant_sessions;
