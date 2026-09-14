# QA IDI 压力泵规程截图逐页核实

## Task Goal

逐页核对用户提供的 5 张 `PQC-IDI-001（B/0）按压式球囊扩充压力泵组装过程检验规程` 截图，确认 QA 规程列表中的工序、检验项目、接收标准、检验方法、检验工具和抽样方案与截图可见内容一致；若发现缺失、合并或截断错误，补齐到 `QaRegulationPage.vue` 并用静态契约锁定。

## Milestones

- [x] 创建任务记录并读取适用前端、PDF、编码、收尾和经验门禁。
- [x] 逐页核对图 1-5 与 `PQC-IDI-001` 模板项目边界。
- [x] 先补充或更新静态契约，覆盖截图逐页项目和字段。
- [x] 修复 QA 规程模板中与截图不一致的内容。
- [x] 运行目标静态契约、相邻回归和结构检查。

## Expected Verification

- `node tests/e2e/qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs doc/tasks/20260806-qa-idi-pressure-pump-screenshot-pages-verify`

## Experience Gate Summary

- `docs/frontend-development.md#前端静态契约隔离门禁`：截图/规程专用静态契约必须使用明确相邻函数边界，避免把 `PQC-ID-001` 模板误计入 `PQC-IDI-001`。
- `docs/backend-development.md#MES PQC 项目级检验快照门禁`：QA 检验项目事实必须来自发布规程，不得用前端文案或固定字段替代正式项目级规程快照。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：若生成 `frontend-feature-evidence.md`，cleanup 前需先运行 validator 并把核心 PASS 结果归档到保留文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用截图逐页静态契约锁定正式模板内容。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

- 实现、定向验证、前端证据 validator、cleanup preview/apply 和经验沉淀已完成。
- Git commit/push closeout 暂停：共享 `int_main` 工作区存在大量非本任务脏改动，提交会混入无关任务文件。
