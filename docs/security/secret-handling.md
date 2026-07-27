# Secret handling

RunbookOS uses environment variables for platform secrets and encrypted database records for user-supplied integration credentials.

- Passwords are BCrypt hashes and are never reversible.
- Refresh tokens are stored as hashes; access tokens are short lived.
- Integration credentials are encrypted with AES-256-GCM using a base64-encoded 32-byte key.
- Ciphertext, nonce, key identifier, and a non-reversible fingerprint are stored separately.
- Secret values are accepted once and never returned by an API.
- Credential revocation overwrites and marks the stored value revoked.
- Logs, webhook payloads, evidence, and AI prompts pass recursive redaction.
- n8n uses its own stable `N8N_ENCRYPTION_KEY`; losing it makes stored n8n credentials unrecoverable.
- Production secrets belong in a cloud secret manager or Docker secret integration, never in Git or image layers.

Rotate integration credentials through the application. Rotate the platform encryption key by adding a new key identifier, re-encrypting records through a controlled migration, verifying fingerprints, and only then retiring the old key. Back up the n8n encryption key separately from the database backup.
