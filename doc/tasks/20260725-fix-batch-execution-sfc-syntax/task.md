# 20260725 Fix Batch Execution SFC Syntax

## Task Goal

Fix the Vue compiler syntax error in `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` without changing unrelated behavior.

## Milestones

- [x] Record BDD scenario and reproduce the reported compiler parse failure.
- [x] Apply the minimal root-cause syntax fix.
- [x] Run targeted verification proving the SFC parses.
- [x] Record final verification and closeout status.

## Expected Verification

- Targeted Vue SFC compile check fails before the fix with the reported missing semicolon parse error.
- Targeted Vue SFC compile check passes after the fix.

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复源文件语法边界而非关闭 Vite overlay 或绕过编译。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 已存在；本次错误文本为 Vue SFC 局部语法错误，未命中需额外打开的专项经验文档。

## Verification Evidence

- `RED`：targeted `@vue/compiler-sfc` `compileScript` check reproduced `[vue/compiler-sfc] Missing semicolon` at `BatchExecutionListPage.vue` line 920.
- `GREEN`：same targeted `@vue/compiler-sfc` `compileScript` check returned `SFC script compile OK`.
