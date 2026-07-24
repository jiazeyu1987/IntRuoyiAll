# 任务：展厅前端 F4 前台体验对齐

## 目标

让当前前台展示重新对齐设计文档：补齐设备模式入口语义、公司返回路径、设置行为、讲解文本/音频行为、预览图展示和前台主页面结构。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 完成前台主页面体验对齐
- [x] 运行测试与 lint
- [x] 更新任务记录并提交

## 范围

- 可修改：
  - `src/views/showroom-frontstage/index.vue`
  - `src/views/showroom-frontstage/shared/constants.ts`
  - `src/views/showroom-frontstage/shared/route.ts`
  - `src/views/showroom-frontstage/shared/payload.ts`
  - `src/views/showroom-frontstage/shared/types.ts`
  - `src/views/showroom-frontstage/shared/narration.ts`
  - 新增你自己的前台核心组件目录，例如 `src/views/showroom-frontstage/core/**`

## 非范围

- 不修改 `src/views/showroom-frontstage/screen/**`
- 不修改 `src/views/showroom-frontstage/pad/**`
- 不修改 `src/views/showroom-frontstage/mobile/**`
- 不修改 `src/views/showroom-frontstage/shared/components/**`
- 不修改后台页面与 router

## 写入边界

- 仅允许写上述文件与目录
- 仅允许写 `scripts/showroom-frontstage-experience-alignment*.mjs`
- 仅允许写 `doc/tasks/20260519-showroom-remediation-f4-frontstage-experience-alignment/**`

## 依赖

- `B2/B5` 提供前台 display 与 narration 的真实契约
- 当前仓库存在进行中的 `showroom-frontstage-shell-wave-b`，不得侵入其目录

## 前置检查

- 上一轮相关前台任务：
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-shell-wave-d\task.md`
- 状态：
  - `Wave D` 已完成真实路径验证，但因 live 数据仍可能缺少 preview image URL 被严格驳回。
- 对本任务的影响：
  - 本任务负责把前台体验结构、设置语义、返回路径和预览图/讲解缺失态表达对齐设计文档。
  - 若 live 数据继续缺少预览图或讲解资源，只能清晰暴露缺失状态，不允许添加 fallback 内容。

## 预期验证

- `node --test scripts/showroom-frontstage-experience-alignment*.mjs`
- `pnpm exec eslint src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/shared src/views/showroom-frontstage/core`

## 完成定义

- 前台不再只是简化版 tab + 表格，而是更贴近设计的体验入口。
- 讲解语言和设置行为与设计一致。
- 预览图与讲解的错误/缺失状态处理清晰。
- 不影响当前进行中的设备壳 wave B 写入范围。

## 当前状态

已完成：前台体验语义、设置说明、返回路径和预览图/讲解状态表达已对齐设计文档，并已通过指定测试、lint 与 cleanup 预览。

## 后续说明

- 本任务为当前仓库中的历史前台整改记录。
- 后续新的 APP 展示层需求改由 `D:\ProjectPackage\Website` 仓库承接。

## Current Status

Completed.

## 验证结果

- PASS: `node --test scripts/showroom-frontstage-experience-alignment*.mjs`
- PASS: `pnpm exec eslint src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/shared src/views/showroom-frontstage/core`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-remediation-f4-frontstage-experience-alignment --mode preview`

## Cleanup Keep

- `doc/tasks/20260519-showroom-remediation-f4-frontstage-experience-alignment/task.md`
- `doc/tasks/20260519-showroom-remediation-f4-frontstage-experience-alignment/execution-log.md`
- `doc/tasks/20260519-showroom-remediation-f4-frontstage-experience-alignment/frontend-feature-evidence.md`
- `scripts/showroom-frontstage-experience-alignment.test.mjs`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f4-frontstage-experience-alignment\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\frontend-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\user-flows.md

目标：
- 重做前台主页面体验，使其更贴近设计文档。
- 只动 index/shared/core，自觉避开 screen/pad/mobile/shared/components 当前在途工作。

写入边界：
- src/views/showroom-frontstage/index.vue
- src/views/showroom-frontstage/shared/{constants,route,payload,types,narration}.ts
- 你自己新增的 src/views/showroom-frontstage/core/**

要求：
- 不要修改 screen/pad/mobile/shared/components 目录。
- 严格 TDD。
- 缺后端契约就失败并记录 blocker。
- 不要偷偷加 fallback。

完成后运行：
- node --test scripts/showroom-frontstage-experience-alignment*.mjs
- pnpm exec eslint src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/shared src/views/showroom-frontstage/core
```
