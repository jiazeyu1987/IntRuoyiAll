# 岗位需求分解矩阵 M0-M6 实现任务

## Goal

按照规划包 `doc/tasks/20260801-role-requirement-matrix-excel/` 顺序实现源 Excel 的 23 项主需求和 39 项衍生需求，最终完成 62 个 AC，并通过 BDD、严格 TDD、真实 Playwright E2E、权限、并发、迁移、快照、性能和清理验证。

## Scope

- 新实现任务目录独立于规划任务目录；规划目录只作为输入，不在本任务中继续修改。
- 严格按 `M0 -> M1 -> M2 -> M3 -> M4 -> M5 -> M6` 推进。
- 当前只允许推进 M0：冻结契约、术语、权威来源、source map、测试数据、角色、权限和 E2E 前置。
- 只有当前里程碑全部 AC 达到 `ACCEPTED`，且对应真实 Playwright E2E 或 E2E 前置检查通过，才允许进入下一里程碑。
- 本次按用户明确要求不执行 `git push`；如后续需要提交，只允许本地提交和本地验证。

## Non-Scope

- 不跨里程碑提前实现 M1-M6 生产代码。
- 不在缺少正式来源、测试账号、运行服务、数据库、Redis、电子签名或真实样本时用 mock、默认值、API-only、静态合同或截图替代真实 E2E。
- 不引入 fallback、双读、兼容 shim、默认生产系数 `1`、默认订单、默认人员、默认数量、默认合格或占位成功。
- 不使用 `formBindings`、`MAIN` 槽位或 `工序开始` 替代正式逐工序批记录绑定。

## Milestones

- [ ] M0：契约、术语、权威来源和 E2E 前置冻结。
- [ ] M1：权威活跃订单与增量模型。
- [ ] M2：生产事实、系数分配与正式批记录。
- [ ] M3：QA 规程与 PQC 闭环。
- [ ] M4：调拨、异常、完整性与放行。
- [ ] M5：日结、范围、权限、审计与快照。
- [ ] M6：迁移、并发、性能、真实 E2E 与上线验收。

## Expected Verification

- M0 source map 明确 ERP 订单、调拨、发货、补料、退料、批次、QA 规程、生产系数、正式批记录绑定、异常、返工、报废、库存、签名、租户、角色和运行服务的正式来源或 blocker。
- BDD/TDD acceptance validator 对规划包通过。
- Roadmap validator 对规划包通过。
- 当前实现任务 `task-state.json` 可按 UTF-8 解析，M0 状态和 blocker 状态明确。
- 真实 E2E 前置检查覆盖前端、后端、数据库、Redis、浏览器、登录页、角色账号、权限、电子签名和任务数据来源；缺任一前置时 M0 标记 `blocked`，不得进入 M1。
- 当前任务 Markdown/JSON 均可 UTF-8 读取，`git diff --check` 无 whitespace error。
- 本次不执行 `git push`；如后续需要提交，只允许本地提交。

## Current Status

blocked

## Current Milestone

M0

## Blockers

- M0 source map 已完成，结论为 `BLOCKED`：当前活跃订单仍是生产组长范围模型，PQC 仍读取 `mes_pro_process_pool` 活跃行且提交依赖最新生产事件，未满足统一 activeOrderId。
- ERP 调拨申请/发货/补料/退料/批次与 activeOrderId 的正式关系源未确认。
- QA 规程唯一所有权、PQC 任务/规程版本/规程快照/逐件明细正式模型未确认。
- PQC 前端仍硬编码检验项目、巡检类型和默认数量，未按发布规程动态渲染。
- 放行检验、偏差、返工、报废、库存来源仍为“未接入” blocker。
- 生产系数来源只达到部分确认：路线/排产快照存在系数字段，但 activeOrderId 缺生产系数/计划数量快照，自动排产仍存在缺失系数默认 `1` 的路径。
- 正式逐工序批记录绑定只达到部分确认：绑定表存在，但前端和 eDHR 运行态仍有缺失槽位默认 `MAIN` 路径，`batchRecordFormNames` 与 `formBindings` 互不替代尚未通过真实 E2E 证明。
- `role-requirement-matrix` 真实 E2E 预检脚本已创建并通过静态合同；用户授权本机 `芋道源码` 租户、六角色账号、权限、电子签名、压力泵路线、工单/调拨和 QC/IPQC 夹具已补齐，当前真实预检剩余 31 个 SOURCE 缺口，无 ENV/RUNTIME blocker。
- M3/M4/M5 规划静态脚本已创建并接入 package scripts；当前按业务缺口 RED，分别阻塞 QA 规程、PQC 动态表单、调拨/开工检查、日结/范围实现。
- 本地 M0 夹具不等于正式来源实现：QC/IPQC 模板不是正式 QA 规程版本模型，工单/调拨夹具不是 activeOrderId 关系源，不能据此进入 M1。

## M0 Evidence

- `source-map.md`
- `m0-preflight.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`
- `m0-test-data.md`
- `database-schema-evidence.md`

## Applicable Gate Summary

- 严格 TDD 状态链：`PLANNED -> BDD_APPROVED -> TEST_ADDED -> RED_VALID -> IMPLEMENTING -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`。
- 缺测试类、缺脚本、No tests、编译失败、依赖缺失、服务未启动、账号缺失或测试数据缺失均只能记录 blocker，不算 RED。
- 用户可见行为必须通过正式登录页、正式菜单和真实页面路径执行 Playwright E2E；API 只能用于最终只读核验或任务数据清理。
- 后续实现任务不执行 Git push，除非用户另行明确要求。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按规划包先冻结正式来源和验证门禁，再逐 AC 实现。
- `是否存在临时补丁或绕过`：否。
