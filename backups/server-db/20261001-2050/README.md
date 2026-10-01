# xiaozh-server DB backup — 2026-10-01 20:50 (UTC+7)

Snapshot of `xiaozhi_esp32_server` (MySQL 8.4) on `xiaozh-server`, taken right before the source
was switched to `thang/main` (967a5035). Pre-switch server source: upstream `788f530` plus
cong91 patches (fork HEAD `ffce7a8f`).

- `xiaozhi_esp32_server.sql.gz.enc` — `mysqldump --single-transaction --routines --triggers`,
  gzip, then AES-256-CBC (PBKDF2, 200000 iterations). Encrypted because the dump holds API keys
  and password hashes and this repo is public.
- Plain gzip MD5 (before encryption): `59ec41d4abdff013cad32025b000b469`
- The passphrase is not in the repo (kept by the repo owner).

Restore:

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in xiaozhi_esp32_server.sql.gz.enc -out db.sql.gz -pass file:<passphrase-file>
gunzip -c db.sql.gz | mysql -uroot -p xiaozhi_esp32_server
redis6-cli FLUSHDB   # config/params caches have no TTL
```

The server also keeps `/home/ec2-user/backups/20261001-2050/` with the plain dump, the
`/opt/xiaozhi-esp32-server` source tarball (no venv/models/.git) and the console tarball.
