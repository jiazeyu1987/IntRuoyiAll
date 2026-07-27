# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: “版本变更说明”弹窗只展示当前版本相对上一版本的 Git 变更，最多 10 条。
- Non-goal: 不在弹窗继续展示发布包版本号、构建时间、发布范围、组件、摘要、变更项或源码提交机器字段。

## Requirements And Acceptance

- AC1: 弹窗正文只包含 Git 变更列表。
- AC2: Git 变更最多展示 10 条。
- AC3: Git 变更为空时显示明确空状态，不回退到发布包元信息。

## UI Entry Points And Owned Files

- Entry: 左侧菜单底部 `ReleaseInfoDock` 版本号按钮。
- Owned frontend files:
  - `IntRuoyiFronted/src/components/ReleaseInfoDock/ReleaseInfoDock.vue`
  - `IntRuoyiFronted/tests/e2e/release-info-dock-version-only-static.spec.js`
  - `IntRuoyiFronted/scripts/release-info-dock-contract.test.mjs`

## API Contracts And Data States

- Reads `/release-info.json`.
- Uses `changeSet.gitChanges` as the only dialog data source.
- Does not fall back to `changeSet.items`, `changeSet.changes`, `sourceRepos`, or publish metadata for dialog content.

## BDD Scenarios

- `BDD: release change dialog shows only git diff summary -> Given release-info contains old metadata and gitChanges, When the user opens the dialog, Then only the first 10 gitChanges are rendered.`
- `BDD: release change dialog empty git diff state -> Given release-info has no gitChanges, When the user opens the dialog, Then the empty Git change message is shown.`

## RED

- `RED: node tests/e2e/release-info-dock-version-only-static.spec.js -> FAIL, expected reason: gitChangeItems.slice(0, 10) and Git-only dialog were absent.`
- `RED: node --test scripts/release-info-dock-contract.test.mjs -> FAIL, expected reason: the component still exposed old details.`

## GREEN

- `GREEN: node tests/e2e/release-info-dock-version-only-static.spec.js -> PASS.`
- `GREEN: node --test scripts/release-info-dock-contract.test.mjs -> PASS.`
- `GREEN: pnpm ts:check -> PASS.`

## Verification

- Focused static contracts confirm old metadata/source sections are hidden and Git changes are capped at 10.
- Type checking confirms the narrowed release-info contract compiles.

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive: existing dock width/media behavior preserved; dialog body simplified to one section.
- Accessibility: existing button semantics and dialog title preserved.
- Loading/error: failed `/release-info.json` still surfaces `版本信息未生成`.
- Empty: no Git changes displays `Git 变更未生成`.
- Permission: no permission or route changes.

## Blockers And Follow-Up Skills

- No frontend blocker.
- Backend full publish-tooling regression has one unrelated SQL metadata blocker recorded in `execution-log.md`.
