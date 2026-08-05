# Backend API Evidence

## Scope

AC-M23 eDHR release transaction service and approval-center release adapter.

## Contract

- `submit`: requires precheck passed, current dossier config hash, route `RELEASE_APPROVE` owner, password validation, release signature, transaction event, operation audit.
- `approve`: requires pending approval, current approval task candidate, current-user approval signature evidence, transaction event, operation audit.
- `reject`: requires release owner for direct precheck-passed rejection or approval task candidate for pending approval rejection, reason, transaction event, operation audit.
- `withdraw`: remains lifecycle cancellation path and records operation audit.

## BDD

See `execution-log.md`.

## RED

Pending.

## GREEN

Pending.

## Blockers

Pending verification.

