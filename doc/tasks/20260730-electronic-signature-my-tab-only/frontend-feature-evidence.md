# Frontend Feature Evidence

## Feature Goal

普通用户进入电子签名时只能看到“我的签名”，不能默认进入或渲染签名记录、长期留存、周期复核、CSV 质量包、统一策略、用户授权等治理页签。

## Non-Goals

- 不修改电子签名接口契约。
- 不给普通用户补管理员权限。
- 不吞掉或隐藏后端错误来冒充可访问。

## Owned Files

- `IntRuoyiFronted/src/views/signature-governance/index.vue`
- `IntRuoyiFronted/src/store/modules/permission.ts`
- `IntRuoyiFronted/tests/e2e/electronic-signature-my-tab-only-static.spec.js`
- `IntRuoyiFronted/scripts/signature-governance-page-contract.test.mjs`

## BDD

- `BDD: 普通用户电子签名页签隔离 -> Given 普通用户进入电子签名页面 When 页面初始化页签 Then 页面只展示“我的签名”页签且不会展示/默认进入无权限的管理页签`

## Acceptance

- 普通用户进入电子签名根入口时只落到 `/signature-governance/my-signature`。
- 普通用户不会渲染全量签名记录、长期留存、周期复核、CSV 质量包、统一策略或用户授权页签。
- 管理员角色仍可通过正式授权子路由访问治理页签。

## RED / GREEN

- `RED: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> FAIL, 管理页签未建正向授权集合且签名记录页签可直接 mount`
- `GREEN: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Verification

- `node tests/e2e/electronic-signature-my-tab-only-static.spec.js` -> PASS
- `node --test scripts/signature-governance-page-contract.test.mjs` -> PASS
- `pnpm ts:check` -> PASS

## Permission Checks

- 普通用户治理页签请求会被前端重定向到 `/signature-governance/my-signature`。
- 签名记录与治理页签组件只在 `canViewGovernanceTabs` 为真时 mount。
- 权限路由合并不再为电子签名普通用户补回未授权隐藏静态子路由。

## Remaining Blockers

- 真实页面 E2E 未执行；当前交付以静态合同、类型检查和 SQL 门禁验证。仓库存在并发 ahead/dirty 状态，不能安全执行最终提交推送。
