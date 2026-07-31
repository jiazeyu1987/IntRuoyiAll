# Database Schema Evidence

## Scope

- Local MySQL database `ruoyi-vue-pro` on `127.0.0.1:23306`.
- Source tenant: pending lookup for “芋道源码”.
- Target tenant: pending lookup for “测试租户”.

## Data Safety

- Destructive delete is scoped to the target tenant only.
- A task-owned JSON backup is required before deleting target tenant scheduling data.

## BDD

- BDD: Local tenant scheduler data copy -> Given source and target tenants exist locally / When target scheduler data is backed up, deleted, and reloaded from source package / Then source and target scheduler data match by key counts and import reports success.

## RED

- RED: Pending tenant/database preflight before destructive delete.

## GREEN

- GREEN: Pending.

## Verification

- Pending.

## Blockers

- None yet; tenant IDs still pending lookup.
