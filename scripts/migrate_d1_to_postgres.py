#!/usr/bin/env python3
"""
Migrazione dati Barizone: Cloudflare D1 (SQLite) → PostgreSQL.

Uso:
  1. Esporta il database D1 di produzione:
       npx wrangler d1 export barizone --remote --output=d1-dump.sql
  2. Genera l'SQL per PostgreSQL:
       python3 -I scripts/migrate_d1_to_postgres.py d1-dump.sql > import.sql
  3. Avvia una volta l'applicazione Java (Flyway crea lo schema), poi importa:
       psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f import.sql

Cosa viene migrato: notizie, aziende (il JSON diventa colonne), foto, vetrina del giorno,
contatti, chiavi VAPID (indispensabili: senza, i dispositivi iscritti non ricevono più notifiche),
account admin, iscritti e consensi, iscrizioni push, campagne e consegne, gruppi esercenti.
Non vengono migrate: sessioni (si rientra con login), tentativi di login, tabelle demo_* e\naziende segnate come demo.

L'import è idempotente (ON CONFLICT DO NOTHING) e avviene in un'unica transazione.
Solo libreria standard di Python: nessuna dipendenza da installare.
"""
import json
import sqlite3
import sys

out = []


def q(value):
    """Letterale SQL PostgreSQL."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (bytes, bytearray, memoryview)):
        return "'\\x" + bytes(value).hex() + "'::bytea"
    return "'" + str(value).replace("'", "''") + "'"


def arr(values):
    items = ",".join('"' + str(v).replace("\\", "\\\\").replace('"', '\\"') + '"' for v in values or [])
    return q("{" + items + "}") + "::text[]"


def ts(value):
    return "NULL" if value in (None, "") else q(value) + "::timestamptz"


def insert(table, row):
    cols = ", ".join(row.keys())
    vals = ", ".join(row.values())
    out.append(f"INSERT INTO {table} ({cols}) VALUES ({vals}) ON CONFLICT DO NOTHING;")


def rows(db, table):
    exists = db.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", (table,)).fetchone()
    if not exists:
        return []
    db.row_factory = sqlite3.Row
    return db.execute(f"SELECT * FROM {table}").fetchall()


def subscription_parts(raw):
    s = json.loads(raw)
    keys = s.get("keys", {})
    return s.get("endpoint", ""), keys.get("p256dh", ""), keys.get("auth", "")


def main(path):
    db = sqlite3.connect(":memory:")
    with open(path, encoding="utf-8") as f:
        db.executescript(f.read())

    out.append("BEGIN;")

    for r in rows(db, "news"):
        insert("news", {
            "id": q(r["id"]), "title": q(r["title"]), "subtitle": q(r["subtitle"] or ""), "content": q(r["content"]),
            "category": q(r["category"]), "quartiere": q(r["quartiere"] or ""), "cover_image": q(r["cover_image"] or ""),
            "video_url": q(r["video_url"] or "") if "video_url" in r.keys() else "''",
            "source_url": q(r["source_url"] or ""), "author": q(r["author"] or "Redazione Barizone"),
            "status": q(r["status"]), "origin": "'admin'", "is_featured": q(bool(r["is_featured"])),
            "published_at": ts(r["published_at"]), "updated_at": ts(r["updated_at"]),
        })

    skipped_demo = 0
    for r in rows(db, "company_profiles"):
        d = json.loads(r["data"])
        if d.get("is_demo") is True:
            skipped_demo += 1  # i dati demo non si importano
            continue
        text = lambda k: q((d.get(k) or "") if isinstance(d.get(k), str) else "")
        insert("company_profiles", {
            "id": q(r["id"]), "business_name": text("business_name"), "owner_name": text("owner_name"),
            "legal_name": text("legal_name"), "vat_number": text("vat_number"), "tax_code": text("tax_code"),
            "legal_address": text("legal_address"), "postal_code": text("postal_code"), "city": text("city"),
            "province": text("province"), "email": text("email"), "pec": text("pec"), "phone": text("phone"),
            "category": text("category"), "quartiere": text("quartiere"), "address": text("address"),
            "description": text("description"), "slogan": text("slogan"), "promotion": text("promotion"),
            "plan": text("plan"), "cover_image": text("cover_image"), "maps_url": text("maps_url"),
            "entity_type": text("entity_type"), "photos": arr([p for p in d.get("photos", []) if isinstance(p, str)]),
            "verified": q(d.get("verified") is True),
            "contract_active": q(d.get("contract_active") is True), "notifications_enabled": "FALSE",
            "status": q(r["status"]), "updated_at": ts(r["updated_at"]),
        })

    for r in rows(db, "business_spotlight"):
        insert("business_spotlight", {"spotlight_date": q(r["date"]) + "::date", "merchant_id": q(r["merchant_id"])})

    for r in rows(db, "company_media"):
        content = bytes(r["content"])
        insert("media_files", {"id": q(r["id"]) + "::uuid", "mime": q(r["mime"]), "size_bytes": str(len(content)),
                               "content": q(content), "created_at": "now()"})

    kinds = {}
    for r in rows(db, "push_config"):
        key = r["id"]
        if key.startswith("campaign-kind:"):
            kinds[key.split(":", 1)[1]] = r["value"]
        elif key in ("vapid", "barizone-contact"):
            insert("app_config", {"id": q(key), "value": q(r["value"])})

    for r in rows(db, "site_admin_accounts"):
        insert("admin_accounts", {"username": q(r["username"]), "name": q(r["name"]), "salt": q(r["salt"]),
                                  "password_hash": q(r["password_hash"]), "iterations": str(r["iterations"])})

    for r in rows(db, "news_subscribers"):
        insert("news_subscribers", {"id": q(r["id"]) + "::uuid", "first_name": q(r["first_name"]),
                                    "last_name": q(r["last_name"]), "email": q(r["email"]), "phone": q(r["phone"] or ""),
                                    "quartiere": q(r["quartiere"]), "consent_at": ts(r["consent_at"]),
                                    "status": q(r["status"]), "token": q(r["token"])})
    for r in rows(db, "news_subscriber_consents"):
        insert("news_subscriber_consents", {"subscriber_id": q(r["subscriber_id"]) + "::uuid", "version": q(r["version"]),
                                            "consent_at": ts(r["consent_at"])})

    for r in rows(db, "push_subscriptions"):
        endpoint, p256dh, auth = subscription_parts(r["subscription"])
        insert("push_subscriptions", {"id": q(r["id"]), "user_id": q(r["user_id"]), "endpoint": q(endpoint),
                                      "p256dh": q(p256dh), "auth": q(auth), "quartiere": q(r["quartiere"]),
                                      "consented_at": ts(r["consented_at"]), "consent_version": q(r["consent_version"])})

    for r in rows(db, "push_campaigns"):
        insert("push_campaigns", {
            "id": q(r["id"]) + "::uuid", "kind": q(kinds.get(r["id"], "sponsored")), "title": q(r["title"]),
            "body": q(r["body"]), "url": q(r["url"]), "business": q(r["business"] or ""),
            "contract_ref": q(r["contract_ref"] or ""), "amount_cents": str(r["amount_cents"] or 0),
            "payment_status": q(r["payment_status"]), "quartiere": q(r["quartiere"]), "status": q(r["status"]),
            "created_at": ts(r["created_at"]), "started_at": ts(r["started_at"]), "created_by": q(r["created_by"]),
        })
    for r in rows(db, "push_deliveries"):
        insert("push_deliveries", {"id": q(r["id"]), "campaign_id": q(r["campaign_id"]) + "::uuid",
                                   "subscription_id": q(r["subscription_id"]), "status": q(r["status"]),
                                   "sent_at": ts(r["sent_at"])})

    for r in rows(db, "merchant_clients"):
        insert("merchant_clients", {"id": q(r["id"]) + "::uuid", "token_hash": q(r["token_hash"]),
                                    "created_at": ts(r["created_at"])})
    for r in rows(db, "merchant_memberships"):
        insert("merchant_memberships", {"id": q(r["id"]) + "::uuid", "client_id": q(r["client_id"]) + "::uuid",
                                        "shop_id": q(r["shop_id"]), "alias": q(r["alias"]), "active": q(bool(r["active"])),
                                        "updated_at": ts(r["updated_at"])})
    for r in rows(db, "merchant_messages"):
        insert("merchant_messages", {"id": q(r["id"]) + "::uuid", "shop_id": q(r["shop_id"]), "title": q(r["title"]),
                                     "body": q(r["body"]), "created_at": ts(r["created_at"])})
    for r in rows(db, "merchant_deliveries"):
        insert("merchant_deliveries", {"message_id": q(r["message_id"]) + "::uuid",
                                       "membership_id": q(r["membership_id"]) + "::uuid",
                                       "delivered_at": ts(r["delivered_at"]), "push_status": q(r["push_status"])})
    for r in rows(db, "merchant_device_subscriptions"):
        endpoint, p256dh, auth = subscription_parts(r["subscription"])
        insert("merchant_device_subscriptions", {"id": q(r["id"]), "client_id": q(r["client_id"]) + "::uuid",
                                                 "endpoint": q(endpoint), "p256dh": q(p256dh), "auth": q(auth),
                                                 "consented_at": ts(r["consented_at"])})

    out.append("COMMIT;")
    print("\n".join(out))
    print(f"-- {len(out) - 2} righe preparate per l'import ({skipped_demo} aziende demo escluse)", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Uso: python3 -I migrate_d1_to_postgres.py d1-dump.sql > import.sql")
    main(sys.argv[1])
