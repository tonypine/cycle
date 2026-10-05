# 0004: Backups only end-to-end encrypted

**Status:** accepted, 2026-10-04

## Context

[`0001`](0001-stack.md) keeps her data on the phone, with Android's own backup as the one way off it,
and the app shipped with `allowBackup="true"` and no backup rules. Android then backs up her log and
settings to Google Drive whether or not that backup is end-to-end encrypted. On Android 9 and later
the backup is encrypted with the phone's PIN, pattern or password, so Google cannot read it, but only
when a screen lock is set; without one it is stored in a form Google can read and hand over
([Android](https://developer.android.com/identity/data/autobackup)).
[`apps-and-privacy.md`](../research/apps-and-privacy.md) explains why that matters for cycle data:
the category's privacy record, and period data as legal evidence since 2022.

## Decision

Cycle's data goes into a Google backup only when the backup is end-to-end encrypted. On a phone
without a screen lock, the app is not backed up at all.

| Area | Decision |
| -- | -- |
| What is backed up | The Room database (`databases/`, including its journal files) and the DataStore files (`files/datastore/`). Nothing else: an `include` rule backs up only what it names. |
| Android 12 and later | `res/xml/data_extraction_rules.xml`: `<cloud-backup disableIfNoEncryptionCapabilities="true">`. This format ignores a per-file `requireFlags`, so the requirement covers the whole section. |
| Android 10 and 11 | `res/xml/backup_rules.xml` (`fullBackupContent`): each `include` carries `requireFlags="clientSideEncryption"`. |
| Phone to phone | A device-to-device transfer (cable or local network, no server) moves the same files on Android 12 and later. On 10 and 11 the legacy format cannot say "encrypted or phone to phone", so there only an encrypted backup carries them. |
| Tests | `BackupRulesTest` checks both requirements, and that every file the app writes her log and settings to is covered by every rule set. |

## Options considered

- **Back up everything, encrypted or not (what shipped).** The most reliable backup, but on a phone
  without a screen lock her cycle history sits on Google's servers in a form that can be read,
  subpoenaed or leaked. That is the exposure the app exists to avoid.
- **Require end-to-end encryption (chosen).** No readable copy ever leaves the phone. The cost falls
  only on a phone without a screen lock, which also leaves the data open to anyone holding the phone.
- **No backup at all (`allowBackup="false"`).** As private as it gets, but a lost or broken phone
  then loses her whole history, and [`0001`](0001-stack.md) relies on backup to bring her data back
  after a reinstall.
- **Our own encrypted backup** (a passphrase and a custom `BackupAgent`, or an encrypted export
  file). Works without a screen lock, but we would own key handling and recovery, and a forgotten
  passphrase loses everything. Android's encryption already does this with a secret she uses every
  day.

## Consequences

- With a screen lock and Google backup on, Cycle is backed up as before, and restoring it on a new
  phone asks for the old phone's screen lock.
- Without a screen lock, nothing is backed up and Android does not say so. Losing the phone then
  loses her history unless she has exported it. Export is planned from the start
  ([`product-implications.md`](../research/product-implications.md)).
- The rules list the files to back up, so a new storage location outside `databases/` and
  `files/datastore/` is not backed up until it is added to both rule files. `BackupRulesTest` writes
  a day and a setting and fails if either file is missed; a new store needs a write there too.
- Each app gets 25 MB of backup; her log is far below that.
- If she prefers a backup on a phone without a screen lock, the trade-off is hers to make: this
  record changes with a new one.
