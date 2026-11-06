-- Barizone · schema iniziale PostgreSQL
-- Equivalente tipizzato dello schema D1 (SQLite) della versione Cloudflare Worker.
-- Le tabelle demo_*, user_profiles e merchant_access non sono migrate: non erano usate dal codice attivo.

-- ───────────────────────── Notizie ─────────────────────────
CREATE TABLE news (
    id                 VARCHAR(80)  PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    subtitle           TEXT         NOT NULL DEFAULT '',
    content            TEXT         NOT NULL,
    category           VARCHAR(40)  NOT NULL,
    quartiere          VARCHAR(80)  NOT NULL DEFAULT '',
    neighborhoods      TEXT[]       NOT NULL DEFAULT '{}',
    cover_image        TEXT         NOT NULL DEFAULT '',
    cover_caption      TEXT,
    image_source_url   TEXT,
    image_license_url  TEXT,
    video_url          TEXT         NOT NULL DEFAULT '',
    source_url         TEXT         NOT NULL DEFAULT '',
    author             VARCHAR(120) NOT NULL DEFAULT 'Redazione Barizone',
    status             VARCHAR(20)  NOT NULL DEFAULT 'draft'
                       CHECK (status IN ('draft', 'published', 'archived')),
    origin             VARCHAR(20)  NOT NULL DEFAULT 'admin'
                       CHECK (origin IN ('admin', 'editorial')),
    is_featured        BOOLEAN      NOT NULL DEFAULT FALSE,
    views              INTEGER      NOT NULL DEFAULT 0,
    fact_date          VARCHAR(40),
    edition_date       DATE,
    expires_at         TIMESTAMPTZ,
    published_at       TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_news_status_date ON news (status, published_at DESC);
CREATE INDEX idx_news_quartiere   ON news (quartiere);

-- ───────────────────────── Aziende ─────────────────────────
CREATE TABLE company_profiles (
    id                     VARCHAR(64) PRIMARY KEY,
    business_name          VARCHAR(160) NOT NULL,
    owner_name             VARCHAR(500) NOT NULL DEFAULT '',
    legal_name             VARCHAR(500) NOT NULL DEFAULT '',
    vat_number             VARCHAR(11)  NOT NULL DEFAULT '',
    tax_code               VARCHAR(500) NOT NULL DEFAULT '',
    legal_address          VARCHAR(500) NOT NULL DEFAULT '',
    postal_code            VARCHAR(5)   NOT NULL DEFAULT '',
    city                   VARCHAR(500) NOT NULL DEFAULT '',
    province               VARCHAR(500) NOT NULL DEFAULT '',
    email                  VARCHAR(500) NOT NULL DEFAULT '',
    pec                    VARCHAR(500) NOT NULL DEFAULT '',
    phone                  VARCHAR(500) NOT NULL DEFAULT '',
    category               VARCHAR(500) NOT NULL DEFAULT '',
    quartiere              VARCHAR(500) NOT NULL DEFAULT '',
    address                VARCHAR(500) NOT NULL DEFAULT '',
    description            TEXT         NOT NULL DEFAULT '',
    slogan                 TEXT         NOT NULL DEFAULT '',
    promotion              TEXT         NOT NULL DEFAULT '',
    plan                   VARCHAR(500) NOT NULL DEFAULT '',
    cover_image            TEXT         NOT NULL DEFAULT '',
    maps_url               TEXT         NOT NULL DEFAULT '',
    entity_type            VARCHAR(500) NOT NULL DEFAULT '',
    photos                 TEXT[]       NOT NULL DEFAULT '{}',
    is_demo                BOOLEAN      NOT NULL DEFAULT FALSE,
    verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    contract_active        BOOLEAN      NOT NULL DEFAULT FALSE,
    notifications_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'draft'
                           CHECK (status IN ('draft', 'published', 'suspended', 'archived')),
    updated_at             TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_company_status ON company_profiles (status);

CREATE TABLE business_spotlight (
    spotlight_date DATE        PRIMARY KEY,
    merchant_id    VARCHAR(64) NOT NULL
);

CREATE TABLE media_files (
    id         UUID        PRIMARY KEY,
    mime       VARCHAR(40) NOT NULL CHECK (mime IN ('image/jpeg', 'image/png', 'image/webp')),
    size_bytes INTEGER     NOT NULL,
    content    BYTEA,
    storage_key TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

-- Configurazione chiave/valore (contatti WhatsApp, chiavi VAPID)
CREATE TABLE app_config (
    id    VARCHAR(120) PRIMARY KEY,
    value TEXT         NOT NULL
);

-- ───────────────────────── Amministrazione ─────────────────────────
CREATE TABLE admin_accounts (
    username      VARCHAR(80)  PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    salt          VARCHAR(128) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    iterations    INTEGER      NOT NULL
);

CREATE TABLE admin_sessions (
    token_hash VARCHAR(64)  PRIMARY KEY,
    username   VARCHAR(80) NOT NULL REFERENCES admin_accounts (username) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_admin_sessions_expiry ON admin_sessions (expires_at);

CREATE TABLE login_attempts (
    id           VARCHAR(64)  PRIMARY KEY,
    attempts     INTEGER     NOT NULL DEFAULT 0,
    window_start TIMESTAMPTZ NOT NULL
);

-- Account del titolare: nel repository è presente solo l'hash salato (come nella versione D1)
INSERT INTO admin_accounts (username, name, salt, password_hash, iterations)
VALUES ('leonardo.favia', 'Leonardo Favia',
        'a71473db422fdf6d497dc31e1cfbef9e73202215495396d5',
        'c9c1a3c271c286a9bb4337cf134e9b5631c93adc502522beb6d600494d9b9992', 100000);

-- ───────────────────────── Iscritti alle news ─────────────────────────
CREATE TABLE news_subscribers (
    id          UUID         PRIMARY KEY,
    first_name  VARCHAR(80)  NOT NULL,
    last_name   VARCHAR(80)  NOT NULL,
    email       VARCHAR(254) NOT NULL UNIQUE,
    phone       VARCHAR(25)  NOT NULL DEFAULT '',
    quartiere   VARCHAR(80)  NOT NULL,
    consent_at  TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('active', 'unsubscribed')),
    token       VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE news_subscriber_consents (
    subscriber_id UUID        PRIMARY KEY REFERENCES news_subscribers (id) ON DELETE CASCADE,
    version       VARCHAR(40) NOT NULL,
    consent_at    TIMESTAMPTZ NOT NULL
);

-- ───────────────────────── Notifiche push ─────────────────────────
CREATE TABLE push_subscriptions (
    id              VARCHAR(64) PRIMARY KEY,           -- base64url(SHA-256(endpoint))
    user_id         VARCHAR(120) NOT NULL,              -- 'public-device' oppure 'subscriber:<id>'
    endpoint        TEXT         NOT NULL,
    p256dh          VARCHAR(120) NOT NULL,
    auth            VARCHAR(60)  NOT NULL,
    quartiere       VARCHAR(80)  NOT NULL DEFAULT 'Tutta Bari',
    consented_at    TIMESTAMPTZ  NOT NULL,
    consent_version VARCHAR(40)  NOT NULL
);
CREATE INDEX idx_push_subscriptions_quartiere ON push_subscriptions (quartiere);
CREATE INDEX idx_push_subscriptions_user ON push_subscriptions (user_id);

CREATE TABLE push_campaigns (
    id             UUID         PRIMARY KEY,
    kind           VARCHAR(20)  NOT NULL DEFAULT 'sponsored' CHECK (kind IN ('barizone', 'sponsored')),
    title          VARCHAR(80)  NOT NULL,
    body           VARCHAR(200) NOT NULL,
    url            TEXT         NOT NULL,
    business       VARCHAR(200) NOT NULL DEFAULT '',
    contract_ref   VARCHAR(200) NOT NULL DEFAULT '',
    amount_cents   BIGINT       NOT NULL DEFAULT 0 CHECK (amount_cents >= 0),
    payment_status VARCHAR(20)  NOT NULL DEFAULT 'pending' CHECK (payment_status IN ('pending', 'paid')),
    quartiere      VARCHAR(80)  NOT NULL DEFAULT 'Tutta Bari',
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'sending', 'completed')),
    created_at     TIMESTAMPTZ  NOT NULL,
    started_at     TIMESTAMPTZ,
    created_by     VARCHAR(80)  NOT NULL
);

CREATE TABLE push_deliveries (
    id              VARCHAR(160) PRIMARY KEY,           -- '<campaign>:<subscription>'
    campaign_id     UUID         NOT NULL REFERENCES push_campaigns (id) ON DELETE CASCADE,
    subscription_id VARCHAR(64) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('sending', 'accepted', 'failed')),
    sent_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_push_delivery_campaign ON push_deliveries (campaign_id);

-- ───────────────────────── Gruppi esercenti ─────────────────────────
CREATE TABLE merchant_sessions (
    token_hash VARCHAR(64)  PRIMARY KEY,
    shop_id    VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE merchant_clients (
    id         UUID        PRIMARY KEY,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE merchant_memberships (
    id         UUID        PRIMARY KEY,
    client_id  UUID        NOT NULL REFERENCES merchant_clients (id) ON DELETE CASCADE,
    shop_id    VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    alias      VARCHAR(60) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (client_id, shop_id)
);
CREATE INDEX idx_merchant_memberships_shop ON merchant_memberships (shop_id, active);

CREATE TABLE merchant_messages (
    id         UUID         PRIMARY KEY,
    shop_id    VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    title      VARCHAR(100) NOT NULL,
    body       VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE merchant_deliveries (
    message_id    UUID        NOT NULL REFERENCES merchant_messages (id) ON DELETE CASCADE,
    membership_id UUID        NOT NULL REFERENCES merchant_memberships (id) ON DELETE CASCADE,
    delivered_at  TIMESTAMPTZ NOT NULL,
    push_status   VARCHAR(20) NOT NULL DEFAULT 'pending'
                  CHECK (push_status IN ('pending', 'sending', 'accepted', 'failed', 'revoked', 'not_enabled')),
    PRIMARY KEY (message_id, membership_id)
);

CREATE TABLE merchant_device_subscriptions (
    id           VARCHAR(64)  PRIMARY KEY,               -- hex(SHA-256(endpoint))
    client_id    UUID        NOT NULL UNIQUE REFERENCES merchant_clients (id) ON DELETE CASCADE,
    endpoint     TEXT        NOT NULL,
    p256dh       VARCHAR(120) NOT NULL,
    auth         VARCHAR(60) NOT NULL,
    consented_at TIMESTAMPTZ NOT NULL
);
