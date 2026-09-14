# Execution Log

## User Intent

用户要求把截图中的“备注”显示在 QA 规程页面“总览”下方。

## Preflight

- 页面入口：`/mes/pro/process-pool/qa-regulation`。
- 页面组件：`IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`。
- 现状：总览页签仅渲染“适用范围”卡片，卡片下方没有备注区块。
- 产品边界：`PQC-IDI-001` 与 `PQC-ID-001` 由产品 ID 分别维护规则和检验项目；本次备注是总览级通用规则，不改两套产品项目数组。
- 运行边界：只读真实页面验证，不执行保存、发布、绑定或其它写入动作。

## BDD

- BDD: QA 规程总览显示备注 -> Given 用户已通过真实 QA 页面选择一个已绑定产品且当前页签为“总览” When 页面完成总览内容渲染 Then “适用范围”卡片下方显示标题“备注”和四条完整规则，且内容可读、不被截断。

## TDD Plan

- RED: `node tests/e2e/qa-regulation-overview-note-static.spec.cjs` -> FAIL，expected reason：`QaRegulationPage.vue` 当前没有总览备注锚点和四条备注文本。
- GREEN: 同一命令 -> PASS，备注区块位于总览适用范围卡片之后，并包含四条完整文本。
- REGRESSION: 运行相邻 QA 静态合同、`pnpm ts:check`、`git diff --check`，再运行真实 Playwright 只读路径。

## Milestone Status

- M1：完成页面入口、产品模板和截图内容核对。
- M2：完成 RED；合同在 `scopeCardEnd` 后找不到 `data-qa-regulation-overview-note`。
- M3：待执行。
- M4：待执行。
- M5：待执行。

## Baseline Evidence

- Baseline commit: `41a68cebb` (`chore: baseline concurrent task artifacts before qa note`).
- Baseline files: 前序 `20260806-hide-review-copy-columns` 文档改动，以及并行任务 `20260807-production-leader-process-loss-reasons-random`、`20260807-qa-project-configured-first` 的任务记录。
- 本任务目录未进入基线提交；提交时 `git diff --cached --check` 仅提示并行任务文档既有末尾空行，未修改这些并行文件。
- Baseline 后并行任务删除了 `doc/tasks/20260807-qa-project-configured-first/` 工作副本，当前以工作区删除状态保留，不恢复、不触碰。

## Current Status

in_progress

专用静态合同已 RED，开始实现备注展示。
