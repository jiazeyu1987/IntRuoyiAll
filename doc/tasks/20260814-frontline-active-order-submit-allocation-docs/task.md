# 一线报工活跃订单自动分配文档设计

## Task Goal

根据用户确认的业务要求，分析现有文档和当前代码是否符合“一线生产选择活跃订单后提交，提交数量先自动分配到该订单，超出订单数量仍允许提交并在生产组长报工管理列表红色标识，生产组长后续仍可重新分配到其它订单”的目标流程，并输出任务级 PRD、开发文档和测试文档。

本任务已从文档设计进入 worktree 实现、验证和融合阶段。

## Evidence Reviewed

- 用户业务要求：一线生产提交时选择活跃订单；提交后数量自动分配到该活跃订单；超过该活跃订单数量仍允许提交；生产组长报工管理列表中本次提交的订单红色标识；提交后生产组长仍可把数量分配给其它订单。
- 旧设计文档：`doc/tasks/20260731-team-leader-workbench-prd-plan/prd.md`、`development-plan.md`、`test-plan.md`。
- 当前前端：一线生产页面已有活跃订单选择和提交前上下文校验；组长页面已有报工分配弹窗、原订单预填、超量提示标签和手动分配入口。
- 当前后端：一线正式提交请求上下文没有 `activeOrderId`，只用 `workOrderId` 校验所选活跃订单；提交事件没有直接保存“选中的活跃订单 ID”；组长分配保存逻辑会按订单工序剩余量截断超额分配。
- 项目规则：`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`。
- 技能规则：`product-requirements-docs`、`system-design-docs`、`bdd-tdd-acceptance-planner`。

## Milestones

- [x] M1：创建任务目录并记录目标、证据和适用门禁。
- [x] M2：分析现有文档与当前代码是否满足新业务要求。
- [x] M3：写入 PRD，明确目标业务流程、冲突修正和验收标准。
- [x] M4：写入开发文档，明确后端、前端、数据和接口改造方案。
- [x] M5：写入测试文档，明确 BDD、TDD、E2E、测试数据和阻塞条件。
- [x] M6：执行文档结构与 UTF-8 读取验证，记录结果。
- [x] P1：后端合同 RED。
- [x] P2：后端实现 GREEN。
- [x] P3：前端合同 RED。
- [x] P4：前端实现 GREEN。
- [x] P5：真实 E2E、回归与融合 int_main。

## Expected Verification

- 使用 UTF-8 方式读取本任务所有 Markdown 文档。
- 检查 PRD 是否覆盖目的范围、业务规则、状态流转、边界场景和验收标准。
- 检查开发文档是否覆盖接口、数据、后端事务、前端表现、错误状态和设计阻塞。
- 检查测试文档是否覆盖 BDD 场景、RED/GREEN 计划、真实 E2E 路径、测试数据和阻塞条件。

## Applicable Gate Summary

### 一线生产正式提交门禁

- Trigger: 一线生产正式提交、选择活跃订单、提交后组长可见性、生产组长报工列表。
- Preflight check: 必须区分“生产提交事实”和“订单级分配事实”；如果新业务要求订单级自动分配，必须建立正式字段和分配链路，不能只靠 `workOrderId` 推断。
- Blocker: 缺少选中活跃订单 ID、提交事件与初始分配不同事务、超量只被截断或静默未分配时，不能宣称符合新业务要求。
- Verification: 一线提交后必须能在生产组长本人报工管理列表看到同一事件、同一选中订单、初始分配数量和超量红色标识。
- Forbidden action: 禁止用页面预填、API-only 推断、未分配数量、静默截断或非组长账号列表替代正式业务事实。

### 前端写入成功与列表刷新分层门禁

- Trigger: 一线提交成功后自动进入组长列表可见、组长重新分配。
- Preflight check: 写入成功、列表刷新失败、响应不确定和下一次独立提交必须分层处理。
- Blocker: 提交成功但分配失败被提示为整体成功，或刷新失败导致重复提交、重复分配。
- Verification: 成功响应必须返回稳定事件和分配身份；刷新失败时提示分层错误并保留可追溯回执。
- Forbidden action: 禁止用本地假行、重复点击、缓存或吞刷新异常掩盖后端事实缺失。

### 写入型 E2E 任务自有模拟环境门禁

- Trigger: 一线提交、组长列表跨角色可见、组长重新分配。
- Preflight check: 必须使用任务自有测试数据，覆盖真实一线账号、生产组长账号、活跃订单、工序、人员范围和电子签名。
- Blocker: 缺账号、缺签名、缺活跃订单、缺组长人员范围、缺菜单权限或不能清理数据时，E2E 标记 BLOCKED。
- Verification: 真实页面提交、组长页面查看红色标识、组长调整分配、数据库只读核验和清理结果均需记录。
- Forbidden action: 禁止用 admin、mock、直连 API、静态合同或直接 SQL 代替真实用户路径。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。文档要求缺少正式活跃订单 ID、分配事务或测试前置时 fail fast。
- 是否从根因和长期维护角度解决：是。方案把一线选择、提交事实、初始订单分配、超量标识和组长调整作为同一正式链路设计。
- 是否存在临时补丁或绕过：否。禁止用列表预填、未分配数量、前端颜色或工单推断替代正式分配事实。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/task.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/execution-log.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/prd.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/development-plan.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/test-plan.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/verification-report.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/task-state.json
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/test-report.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/fas_fixture_orchestrator.py
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/database-schema-evidence.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/result.json
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/evidence.md
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/initial-overage-red.png
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/after-manual-reallocation.png

## Closeout Notes

- 文档设计、后端实现、前端实现、静态合同、后端聚焦测试、前端类型检查和独立复核均已完成。
- P5 使用固定测试租户 `122/测试租户`、独立一线/组长账号和任务自有 O1/O2 数据，通过真实 Playwright 页面完成“一线选择 O1 提交 10（计划 6）-> O1 红色待调整 4 -> 组长改配 O1=6/O2=4 -> 红色消失”。
- 主验证事件 227 和独立复核事件 228 均通过；初始版本 1 为 `FRONTLINE_SELECTED`，调整后版本 2 为 `MANUAL`，分配总量始终等于提交量 10，审计记录完整。
- 两次真实 E2E 均只产生 2 个目标写请求，目标页面、接口、HTTP 和控制台错误均为 0；E2E 清理和独立二次清理均为 `CLEAN`、任务数据残留 0。
- 为构建当前 dirty 基线而使用的临时运行覆盖已按 36 项清单恢复：13 个原有文件逐项校验原始 SHA-256，23 个覆盖新增文件逐项确认不存在；8099/48099 服务已停止且端口空闲。
- 任务变更已通过提交 `dd446b06f` 保存，当前 `int_main` 已快进到融合提交 `25d1654e5`；未执行推送。
- 融合后在 `E:\IntRuoyi` 复跑后端 50 项定向测试、9 项前端静态合同、TypeScript 类型检查和 30 模块 Maven 全量构建，全部通过；全量构建生成 `yudao-server-exec.jar`。
- P1 至 P5 全部完成，机器状态为 `completed`、测试状态为 `passed`，任务数据残留 0，任务运行服务已停止。
