# 岗位需求分解矩阵全链路差距收敛开发包

## Goal

以 `C:\Users\BJB110\Desktop\文档\职责\岗位需求分解矩阵.xlsx` 的两个 sheet 为唯一需求基线，把 23 项主流程需求和 39 项衍生需求转换为可实施、可测试、可追踪的增量开发包。实施完成后，系统应通过真实业务入口覆盖 ERP 候选、活跃订单、调拨追溯、生产报工、班组长复核与分配、QA 规程、PQC 检验与复核、正式批记录、完整性检查和放行闭环。

本任务只优化开发文档，不修改生产代码、数据库、运行态或源 Excel。

## Scope

- 为 Excel 主表 23 行建立 `M01-M23`，为衍生需求 39 行建立 `D01-D39`。
- 每个需求绑定实施里程碑、代码/数据实现区域、BDD 场景和唯一验收编号 `AC-Mxx` / `AC-Dxx`。
- 复用 `doc/tasks/20260731-team-leader-workbench-prd-plan/` 已交付并验证的生产组长能力，只规划统一契约和缺失行为，不重复绿地建设。
- 明确统一活跃订单、生产系数、ERP 调拨、QA 规程、PQC、正式批记录、异常和放行的数据来源及状态边界。
- 形成 `prd.md`、`development-plan.md`、`test-plan.md` 和 `task-state.json`，供后续实现任务按 BDD + 严格 TDD 执行。
- 为 62 个 `AC-*` 分别定义唯一 `TC-*`、正向断言、失败/边界断言和最低测试层级，禁止用范围表达或代码覆盖率替代逐项验收。
- 形成 `docs/acceptance/bdd-scenarios.md`、`tdd-plan.md`、`e2e-plan.md` 和 `test-data.md`，固定 BDD、严格 TDD、真实 E2E 与测试数据合同。
- 定义真实 Playwright 用户路径、正式测试数据、并发、迁移、审计、历史快照和任务自有数据清理门禁。

## Non-Scope

- 不在本任务中实现 Java、Vue、TypeScript、SQL、菜单、权限或 E2E 脚本。
- 不修改或重新导出 `岗位需求分解矩阵.xlsx`。
- 不替代金蝶 ERP 中的订单确认、调拨申请、仓库发货和实物解包动作。
- 不把 Excel 中未定义的复杂排产、自动质量判定、自动异常闭环或自动放行扩展进本节点。
- 不假定 ERP 接口、调拨数据源、测试租户、账号、密码、运行服务或样本数据已经可用。

## Existing Reusable Baseline

`doc/tasks/20260731-team-leader-workbench-prd-plan/` 已完成并通过真实 E2E 的能力：

- 班组员工、设备、设备参数、工序关系、异常原因和活跃订单配置。
- 员工正式报工、生产组长查看原始提交、通过或退回。
- 活跃订单 FIFO 建议、手工调整和报工分配。
- 订单工序累计完成、正式逐工序批记录绑定读取和批记录回填。
- 生产组长工作台 UI、后端定向回归和真实 Playwright 闭环。

后续实现必须保留这些已验证行为，并修复以下差距：

- 生产组长使用 `mes_pro_process_pool_active_order`，PQC 使用 `mes_pro_process_pool` 活跃行，尚未形成跨角色唯一活跃订单事实。
- 生产报工仍要求订单、任务、工作站等订单上下文，不符合“先记录工序生产事实、后由组长分配订单”。
- PQC 提交依赖最新生产事件并复制设备/工作站上下文，无法在尚无生产事件时执行首检。
- PQC 页面固定写死检验项目、`PATROL`、数量 `30`、损耗 `1`，数据模型缺少规程版本、检验类型、日期、班次、轮次、逐件明细和复核状态。
- 报工分配与工序完成仍按固定订单数量，未使用 `固定 ERP 订单数量 × 工序生产系数`。
- 班组设备可独立创建，未强制只绑定设备台账中的正式设备。
- 活跃订单尚无多调拨、分批发货、补料、退料、数量和批次追溯关系。
- 批记录回填使用代表事件，未对订单工序的全部员工、设备和多次报工做确定性汇总。
- 负责范围只覆盖员工、工序、工作站，缺产线、设备和订单。
- 班组日结、PQC 日结及完整放行来源接入尚未完成；eDHR 放行中的检验、偏差、返工、报废和库存检查仍是“来源未接入”阻塞项。

## Terminology Contract

- `工序开始`：只表示特殊开始节点的附件上传人、附件负责人或同类开始动作，不提供表单内容。
- `批记录表单`：只表示 `MesProRouteFlowProcessBatchRecordDO` 中当前路线工序正式绑定的生产批记录，是该工序的正式生产记录载体。
- `表单槽位` / `formBindings`：只表示特殊表单或动态表单中心模板绑定，不得替代、补齐或推断批记录表单。
- 三条链路必须分别建模、展示和验证；任一链路不得用另外两条的数据证明完成。

## Milestones

- [ ] M0：冻结 Excel 需求编号、术语、状态机、权威数据源和已交付基线。
- [ ] M1：建立统一活跃订单聚合及增量数据库契约，完成迁移预检。
- [ ] M2：完成工序事实优先报工、复核修订、系数分配、进度和确定性批记录汇总设计。
- [ ] M3：完成版本化 QA 规程和首检/巡检/末检/PQC 提交复核完整设计。
- [ ] M4：完成调拨追溯、开工检查、异常、过程检验、完整性和放行设计。
- [ ] M5：完成日结、只读看板、范围、权限、审计和历史快照设计。
- [ ] M6：完成迁移、并发、真实 Playwright E2E、回归、清理和上线验收设计。

这些里程碑是后续实现状态；当前文档任务完成后，`task-state.json.status` 保持 `planned`，不得伪装为已实现。

## Expected Verification

- `validate_node_dev_plan.py` 对五个规划文件校验通过。
- `validate_acceptance_plan.py` 对四个 `docs/acceptance/` 验收文档校验通过。
- `development-plan.md` 的追踪矩阵中 `M01-M23`、`D01-D39` 各出现一次且无缺号、重号。
- 每个需求都映射到唯一 `AC-Mxx` / `AC-Dxx`、一个里程碑、一个实施区域和一个 BDD 场景。
- `test-plan.md` 的验收测试矩阵包含 62 个唯一 AC、62 个唯一 TC，且逐行具备非空测试层级、正向断言和失败/边界断言。
- 用户可见写行为包含后端业务测试和真实 Playwright E2E；schema、迁移、并发、权限、租户、快照和性能具备对应低层测试。
- `task-state.json` 可按 UTF-8 解析，包含 `status`、`currentMilestone`、`milestones`、`acceptance`。
- `task-state.json` 机器可读记录严格 TDD 已启用、需求/AC/TC 均为 62、正向和失败/边界覆盖为强制门禁。
- 当前任务 Markdown / JSON 均可用 UTF-8 读取，`git diff --check` 无 whitespace error。
- 当前任务规划提交只包含本任务规划产物；并行脏改动按独立基线提交保存，生产代码、数据库、运行态和源 Excel 均不由本任务修改。
- 收尾前执行 `project-experience-consolidation` 和 `task-closeout-cleanup` preview/apply。

## Blockers

当前任务收尾阻塞：

- `2026-08-01` GitHub HTTPS 不可用：`git ls-remote origin HEAD` 先后返回 `Recv failure: Connection was reset` 和 `Failed to connect to github.com port 443`；`curl --http1.1 https://github.com` 与命令级 Git HTTP/2 同样失败。
- 影响：本地规划、验证、cleanup 和提交均已完成，但 `int_main` 仍领先 `origin/int_main`，因此任务不得标记 completed。
- 解除条件：网络恢复后先执行 `git ls-remote origin HEAD`，再执行 `git push origin int_main`，最后确认 `git status --short --branch` 不再显示 ahead。

以下是后续实施的 fail-fast 前置，不是本次文档优化的阻塞：

- 必须确认金蝶生产订单、调拨申请、调拨单、发货、补料、退料和物料批次的正式本地表/API；未确认前不得编写猜测型关联。
- 必须完成现有两套活跃订单来源的数据冲突盘点；无法确定唯一开放订单/路线时不得切换。
- 必须确认 QA 规程由 MES 还是现有 QMS 模块持有，并冻结跨模块接口所有权；不得同时维护两套有效规程。
- 必须提供测试租户、角色账号、权限、登录凭据、浏览器、数据库、Redis、ERP 同步样本、正式工艺路线、正式批记录绑定和电子签名数据后，才能执行写入型真实 E2E。
- 历史 PQC/活跃订单数据若缺少路线版本、规程版本或轮次，必须保留为明确的 legacy 记录；不得用默认值猜填后参与新放行判断。

## Applicable Gate Summary

- 中文 Markdown/JSON 使用 `apply_patch` 写入，并用 UTF-8 重新读取。
- 正式批记录只读取逐工序绑定；`formBindings` 和 `工序开始` 不得作为替代来源。
- 真实 E2E 必须从前端登录和菜单入口执行，API 仅用于最终只读核验和任务数据清理确认。
- 一对多调拨、报工、PQC 和批记录读模型必须先聚合再分页，避免 JOIN 造成重复行或错误总数。
- 实施任务必须先创建真实可执行测试，再记录行为型 RED；缺测试类、No tests 或缺运行前置不算有效 RED。
- 共享 `int_main` 只允许精确暂存当前任务文件，不得使用 `git add -A` 混入并行任务。

## Final Verification Result

- PASS：源 Excel 的 23 项主需求和 39 项衍生需求均已形成唯一需求 ID、验收 ID、里程碑、实施区域、BDD 和可观察验收。
- PASS：PRD、开发计划、测试计划和任务状态之间的任务名称、里程碑、BDD 与验收映射一致。
- PASS：62 个 AC 均已配置唯一 TC、最低测试层级、正向断言和失败/边界断言；所有包含 UI 层的测试均同时要求真实 E2E。
- PASS：严格 TDD 状态固定为 `BDD_APPROVED -> TEST_ADDED -> RED_VALID -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`，缺测试或 No tests 不算 RED。
- PASS：四份 `docs/acceptance/` 验收文档和机器可读测试覆盖元数据已通过结构校验。
- PASS：roadmap 结构校验、Excel 逐行对照、UTF-8/JSON、whitespace 和 cleanup preview/apply 均通过。
- PASS：cleanup 只删除任务自有临时校验脚本；新增四份验收文档已进入 `Cleanup Keep`。
- 本任务只完成规划包；`task-state.json.status` 继续保持 `planned`，M0-M6 生产实现尚未开始。
- BLOCKED：GitHub HTTPS 连接失败，本地提交尚未推送到 `origin/int_main`。

## Current Status

blocked

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。所有缺少正式来源、规程、绑定、系数、签名或权限的路径均要求 fail fast。
- `是否从根因和长期维护角度解决`：是。计划统一跨角色权威事实和状态机，复用已交付能力，只补齐 Excel 差距。
- `是否存在临时补丁或绕过`：否。未使用默认订单、默认工序、默认人员、默认数量、默认合格、代表事件或 `formBindings` 掩盖正式来源缺失。

## Cleanup Keep

- doc/tasks/20260801-role-requirement-matrix-excel/prd.md
- doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md
- doc/tasks/20260801-role-requirement-matrix-excel/test-plan.md
- doc/tasks/20260801-role-requirement-matrix-excel/task-state.json
- doc/tasks/20260801-role-requirement-matrix-excel/docs/acceptance/bdd-scenarios.md
- doc/tasks/20260801-role-requirement-matrix-excel/docs/acceptance/tdd-plan.md
- doc/tasks/20260801-role-requirement-matrix-excel/docs/acceptance/e2e-plan.md
- doc/tasks/20260801-role-requirement-matrix-excel/docs/acceptance/test-data.md
