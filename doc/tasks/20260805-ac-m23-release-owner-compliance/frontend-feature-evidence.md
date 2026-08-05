# Frontend Feature Evidence

## Scope

AC-M23 release owner actions in `BatchExecutionDetailPage.vue`.

## Contract

- 放行按钮继续调用 `submitEdhrRelease` 并要求负责人电子签名密码。
- 放行退回按钮调用 `rejectEdhrRelease`，使用 `mes:pro-edhr-release:reject` 权限，和质量拒收保持独立。
- 放行追溯页保持只读，不新增写入口。

## BDD

See `execution-log.md`.

## RED

Pending.

## GREEN

Pending.

## Blockers

Pending verification.

