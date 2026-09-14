# 生产组长工作台开发验证任务

## Task Goal

在 worktree `D:\IntRuoyiWorktree\20260731_shengchanbanzuzhang` 内按已确认 PRD、开发计划、测试计划完成生产组长工作台开发、验证、真实 E2E，并在 E2E 成功后融合进 `int_main`。

交付范围包括：

- 生产组长活跃订单、班组员工、设备、设备参数、工序关系、异常关系配置。
- 员工填报页从生产组长配置读取设备、参数、不良原因和员工选项。
- 报工确认支持 FIFO 自动分配，也支持手动分配和调整，且只能分配给活跃订单。
- 订单某工序累计确认分配数量达到订单生产数量后完成该工序，并回填该工序正式批记录表单。
- 使用 BDD + 严格 TDD 完成 RED/GREEN/回归验证，最后通过真实 Playwright E2E。

## Evidence Reviewed

- 用户需求：生产组长维护员工、设备、设备参数、活跃订单、工序关系、异常原因，并确认报工分配到活跃订单。
- 用户需求：员工填报页 `output/frontline-production-operator-1920.html` 和 `output/frontline-production-operator-1920-no-device.html` 的设备、参数、不良原因来自生产组长配置。
- 用户需求：订单某工序累计确认分配数量达到订单生产数量后，工序完成并自动回填该工序绑定的正式批记录表格。
- 用户澄清：设备状态是“报修”，不是“保修”。
- 用户澄清：报工分配需要支持 FIFO 自动分配，同时允许手动分配或调整。
- 当前前端入口：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 当前接口模块：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- 当前员工填报组件：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- 项目规则：`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/e2e-rules.md`、`docs/engineering/technology-stack-routing.md`。
- 技能规则：`product-requirements-docs`、`bdd-tdd-acceptance-planner`。

## Milestones

- [x] M1：创建任务目录并记录目标、门禁、预期验证。
- [x] M2：写入 `prd.md`，定义产品范围、业务规则、状态流转和验收标准。
- [x] M3：写入 `development-plan.md`，定义按阶段推进的 BDD + TDD 开发顺序。
- [x] M4：写入 `test-plan.md`，定义 BDD 场景、RED/GREEN 命令、真实 E2E 路径和测试数据。
- [x] M5：执行文档结构与 UTF-8 读取验证，记录结果。
- [x] P1：班组配置与活跃订单后端模型。
- [x] P2：员工填报配置驱动。
- [x] P3：报工确认与活跃订单分配。
- [x] P4：订单工序完成与批记录回填。
- [x] P5：生产组长前端工作台重构。
- [x] P6：真实 E2E 与回归。

## Expected Verification

- 使用 UTF-8 方式读取任务目录下所有 Markdown / JSON 文档。
- 每个里程碑先新增或确认可执行失败测试，再运行 RED，禁止用缺测试类、缺脚本或 No tests 作为有效 RED。
- 后端运行计划内 Maven 单元 / Controller 测试，并记录 RED/GREEN。
- 前端运行计划内静态合同、类型检查和真实 Playwright E2E，并记录前后端 URL、测试账号标签、任务数据标识和清理结果。
- 最终融合进 `int_main` 前，完成任务状态、验证报告、收尾清理、提交、推送和 merge / fuse 证据。

## Applicable Gate Summary

### 文档目录精确暂存门禁

- Trigger: 当前任务只新增 `doc/tasks/20260731-team-leader-workbench-prd-plan/` 文档，且工作区已有并行前端改动。
- Preflight check: 只写入本任务目录，不触碰现有源码与其它任务目录。
- Blocker: 如需提交，必须先确认现有 ahead 与脏工作区归属，不能用宽泛 `git add -A`。
- Verification: `git status --short --branch` 显示本任务仅新增当前任务目录文件。
- Forbidden action: 禁止提交或回滚并行任务源码改动。

### E2E 脚本入口存在性门禁

- Trigger: 测试计划列出真实 Playwright E2E。
- Preflight check: 实施阶段必须核对 `IntRuoyiFronted/package.json` scripts、目标 spec 文件、真实路由、权限、测试租户和账号。
- Blocker: 缺少脚本、页面入口、权限、测试账号、浏览器或运行态时，E2E 标记 BLOCKED，不得用 API-only 或静态合同冒充真实 E2E。
- Verification: 真实 E2E 证据必须包含前后端 URL、租户/账号标签、页面断言、接口核验和任务自有数据清理。
- Forbidden action: 禁止 mock 数据、API-only、直连历史 URL 或静态合同替代真实用户路径。

### PowerShell / UTF-8 文档写入门禁

- Trigger: 写入中文 Markdown 任务文档。
- Preflight check: 使用 `apply_patch` 写入，验证时使用 `python -X utf8` 或等效 UTF-8 读取。
- Blocker: 发现乱码、编码漂移、文件读写失败时停止并报告。
- Verification: 记录 UTF-8 读取命令与结构校验结果。
- Forbidden action: 禁止默认 `Set-Content`、`Out-File`、`>`、`>>` 写中文文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。文档明确缺少正式数据链路时必须 fail fast。
- `是否从根因和长期维护角度解决`：是。方案围绕班组配置、员工填报、组长确认、订单工序完成、正式批记录回填的根链路设计。
- `是否存在临时补丁或绕过`：否。文档禁止使用 `formBindings`、默认 `MAIN`、空值、mock 或前端文案替代正式批记录来源。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260731-team-leader-workbench-prd-plan/prd.md
- doc/tasks/20260731-team-leader-workbench-prd-plan/development-plan.md
- doc/tasks/20260731-team-leader-workbench-prd-plan/test-plan.md
- doc/tasks/20260731-team-leader-workbench-prd-plan/task-state.json
- doc/tasks/20260731-team-leader-workbench-prd-plan/test-report.md
- doc/tasks/20260731-team-leader-workbench-prd-plan/p6-real-e2e-evidence.md

## Closeout Notes

- 文档写入、P1-P6 实现、真实 E2E 与回归验证已完成。
- P6 真实 Playwright 写入型 E2E 已通过：生产组长配置、员工正式报工、动态 eventId 发现、FIFO 自动分配、组长确认、订单工序完成和正式批记录回填均通过；证据见 `p6-real-e2e-evidence.md`。
- P6 后置清理已完成：`TLW-20260731-` 任务自有活跃订单、绑定、参数规则、异常原因、事件、报工、分配、工序完成和记录本条目均已清理为 `0`，设备 `980005` 恢复 `REPAIRING` 且 enabled。
- P6 复跑核验已完成：用户补齐密码后通过临时环境变量复跑 `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real`，`p6-real-e2e-evidence.md` 已重写为 `Status: PASS`，本轮 eventId=`23`；批记录字段审计 append-only 记录按治理要求保留，未强删。
- 前端静态合同、前端类型检查和后端定向回归已通过；合并后 `int_main` 定向后端回归最终覆盖 48 个测试，0 failures / 0 errors。
- 密码仅通过运行时环境变量临时注入，未写入文档、日志、证据文件或提交信息。
- 主实现提交已完成：`a67a7a305 feat: deliver team leader workbench flow`。
- `task-closeout-cleanup --worktree-closeout off` 已执行 preview/apply，删除 `backend-api-evidence.md`、`database-schema-evidence.md` 两个临时 evidence；核心结论已归档到 `execution-log.md` 和 `verification-report.md`。
- 收尾清理提交已完成：`3c5789190 chore: clean team leader workbench task evidence`。
- 融合回 `E:\IntRuoyi` 的 `int_main` 已完成：`codex/20260731_shengchanbanzuzhang` 是当前 `int_main` 祖先，`int_main` 合并后已通过静态合同、`pnpm --dir IntRuoyiFronted ts:check`、MES 定向 Maven 回归和分支端口门禁。
- 主工作区并行 Runner 改动已按脏工作区基线规则单独提交：`00df27e68 chore: baseline serial routes runner workspace changes`，未混入生产组长任务收尾提交。
- 最终远端推送已恢复：feature branch 与 `int_main` 已完成远端同步；生产组长实现、真实 E2E、`int_main` 融合、收尾记录和最终完成状态均已推送。
