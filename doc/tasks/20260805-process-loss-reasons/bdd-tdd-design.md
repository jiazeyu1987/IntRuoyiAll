# 工序损耗原因维护 BDD/TDD 设计

## Purpose and Scope

本设计用于实现 AC-D04“维护损耗原因”。本任务中的“工序”统一指工艺路线“工序设置”列表下的路线工序记录。损耗原因绑定到 `routeProcessId`，不绑定生产组长个人，也不作为前端固定下拉列表。

## Evidence Reviewed

- 用户确认口径：工艺路线“工序开始”配置了某生产组长，则该组长可维护该工艺路线工序设置列表下所有工序的损耗原因。
- 用户确认口径：一条工艺路线多个生产组长时，损耗原因数据共通；一个组长新增、修改、删除后，其他有权限组长可见。
- 项目术语契约：这里的“工序”只指工艺路线“工序设置列表”下的工序；不得混用批记录表单、表单槽位或工序开始。
- 项目规则：禁止 fallback、默认成功、前端固定列表、API-only 验收和吞异常。
- 项目规则：写入型 E2E 必须走真实前端页面，API 仅用于最终只读核验。

## Feature Scenarios

### BDD: 生产组长只能看到有权限工序

- Given 工艺路线 A 的“工序开始”配置包含生产组长甲，工艺路线 B 未配置生产组长甲。
- When 生产组长甲打开生产组长工作台的“损耗原因维护”区域。
- Then 标准列表只展示工艺路线 A 工序设置列表下的路线工序。
- Then 工艺路线 B 的工序不会出现在列表，也不能通过接口新增、修改、删除损耗原因。

### BDD: 多个生产组长共享同一工序损耗原因

- Given 工艺路线 A 的“工序开始”同时配置生产组长甲和生产组长乙。
- When 生产组长甲为工序 P 新增损耗原因“调机损耗”。
- Then 生产组长乙打开同一列表时可以看到工序 P 的“调机损耗”。
- When 生产组长乙修改或删除该原因。
- Then 生产组长甲再次打开时看到相同结果。

### BDD: 标准列表维护损耗原因

- Given 生产组长拥有工序 P 的维护权限。
- When 生产组长在标准列表操作面板中新增、修改、删除损耗原因。
- Then 操作成功后列表中“损耗原因”独立列立即反映最新配置。
- Then 删除语义为停用，不再进入后续新报工可选项。

### BDD: 报工下拉来自后端配置

- Given 工序 P 配置了启用损耗原因，工序 Q 配置了另一组损耗原因。
- When 员工在报工页面选择工序 P。
- Then 损耗原因下拉只显示工序 P 当前启用的损耗原因。
- Then 前端代码中不存在固定损耗原因列表。

### BDD: 禁用或删除原因不能用于新报工

- Given 工序 P 的损耗原因 R 已被生产组长删除或停用。
- When 员工新建报工并提交损耗数量大于 0 且选择 R。
- Then 后端拒绝提交，返回明确错误。
- Then 系统不得默认改成其它原因或默认成功。

### BDD: 跨工序原因提交被后端拒绝

- Given 工序 P 的损耗原因 R1 与工序 Q 的损耗原因 R2。
- When 员工对工序 P 报工却提交 R2。
- Then 后端拒绝提交，错误说明损耗原因不属于当前工序。

### BDD: 历史报工保留损耗原因快照

- Given 员工已使用工序 P 的损耗原因 R 完成报工。
- When 生产组长修改 R 的名称或删除 R。
- Then 历史报工详情仍展示报工时保存的损耗原因 ID、编码和名称快照。
- Then 历史批记录或追溯不被后续配置变更改写。

## Failure Scenarios

- Given 请求缺少 `routeProcessId`、`lossReasonId` 或损耗数量大于 0 时未传原因；When 提交报工；Then 后端 fail fast，不写入报工、记录本或工序池事件。
- Given 当前生产组长未通过“工序开始”获得目标路线授权；When 新增、修改、删除目标工序损耗原因；Then 后端拒绝，不能靠前端隐藏替代权限校验。
- Given 原因已停用、删除或属于其它 `routeProcessId`；When 用该原因提交新报工；Then 后端拒绝，不能默认成功或替换为其它原因。

## Boundary Scenarios

- 同一路线多个生产组长共享同一 `routeProcessId + LOSS` 原因集合，`leaderUserId` 只作为维护审计上下文，不作为 LOSS 所有权字段。
- 报工运行配置只返回当前 `routeProcessId` 且 `enabled=true` 的 LOSS 原因；其它异常原因类型不能混入损耗下拉。
- 历史报工读取快照字段，不依赖实时 join 当前原因名称。

## Data Contract

### 复用实体

- `mes_pro_process_pool_defect_reason`
  - `route_process_id`：工艺路线“工序设置”列表下的路线工序记录 ID。
  - `process_id`：路线工序关联的正式工序 ID。
  - `reason_type='LOSS'`：损耗原因类型。
  - `reason_code`
  - `reason_name`
  - `enabled`
  - `leader_user_id`：LOSS 原因不作为所有权字段，新增时写空，审计另走维护日志。

### 新增快照字段

- `mes_pro_feedback.loss_reason_id`
- `mes_pro_feedback.loss_reason_code_snapshot`
- `mes_pro_feedback.loss_reason_name_snapshot`

### 约束

- 同租户、同 `route_process_id`、同 `reason_type`、同 `reason_code`、同删除标记必须唯一。
- 删除实现为停用，禁用或删除原因不进入新报工运行配置。
- 历史报工依赖快照字段，不依赖实时原因名称。

## API Contract

### 生产组长维护接口

- `GET /mes/pro/process-pool/team-leader/loss-reasons/page`
  - 返回当前登录生产组长通过“工序开始”配置获得权限的路线工序列表。
  - 每行包含工艺路线信息、路线工序身份、工序名称/序号、损耗原因独立列。
- `POST /mes/pro/process-pool/team-leader/loss-reasons`
  - 新增当前路线工序损耗原因。
- `PUT /mes/pro/process-pool/team-leader/loss-reasons/{id}`
  - 修改原因名称、备注或启用状态。
- `DELETE /mes/pro/process-pool/team-leader/loss-reasons/{id}`
  - 停用原因，使其从新报工可选项移除。

### 报工使用接口

- 报工下拉沿用正式运行配置：`runtimeConfig.defectReasons`。
- 运行配置由后端按 `routeProcessId + reasonType=LOSS + enabled=true` 查询。
- 报工提交接口必须校验：
  - 损耗数量大于 0 时原因必填。
  - 原因必须属于当前报工 `routeProcessId` 且启用。
  - 保存原因 ID、编码快照和名称快照。

## TDD Sequence

| Step | RED Command | Expected Failure | Minimal GREEN Target |
|---|---|---|---|
| 1 | `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 缺 route-process 共享唯一约束或报工快照字段 | 增加迁移、DO 字段、schema 测试 |
| 2 | `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | LOSS 仍绑定生产组长或旧 process 权限 | 实现“工序开始”授权与 routeProcess 共享数据 |
| 3 | `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 报工下拉仍读取固定或跨工序原因 | 运行配置按 routeProcessId 返回启用 LOSS 原因 |
| 4 | `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 禁用/跨工序原因未拒绝或历史快照未保存 | 实现提交校验与快照保存 |
| 5 | `node IntRuoyiFronted/tests/e2e/process-loss-reason-maintenance-static.spec.cjs` | 缺工作台区域、标准列表、独立列或操作面板 | 接入生产组长工作台和 API wrapper |
| 6 | `pnpm.cmd ts:check` | 类型或 API 契约不一致 | 修复类型、状态和错误展示 |
| 7 | 真实 Playwright E2E | 缺两个生产组长、员工账号、路线工序和报工样本前置 | 通过真实页面验证用户 7 项验收；缺前置则 BLOCKED |

## E2E User Path

1. 使用生产组长甲登录 worktree 前端。
2. 进入生产组长工作台。
3. 打开“损耗原因维护”区域。
4. 断言只出现甲通过“工序开始”配置获得权限的工序。
5. 对目标工序新增损耗原因。
6. 使用生产组长乙登录，断言同一工序可见甲新增的原因。
7. 乙修改并删除该原因，甲刷新后看到同一变化。
8. 使用员工登录并进入报工页面，断言损耗原因下拉来自当前工序启用配置。
9. 尝试提交禁用/删除原因和跨工序原因，断言后端拒绝。
10. 完成一次合法报工后修改/删除原因，再打开历史详情，断言快照名称不变。

## API Verification

- API 只允许作为真实页面动作后的只读核验或错误边界核验。
- 不允许以 API-only 替代生产组长页面新增、修改、删除或员工报工下拉验证。

## Console and Log Checks

- 真实 E2E 需记录前端页面无目标链路 `pageerror`。
- 真实 E2E 需记录目标接口请求均来自 `8093/48093` 成对 worktree 运行态。

## Test Data

- 任务标识：`PLR-20260805-`
- 需要两个生产组长账号、一个生产员工账号、至少两条工艺路线、三条路线工序记录。
- 工艺路线 A 的“工序开始”配置组长甲和乙；工艺路线 B 只配置其他组长。
- E2E 数据必须可清理；清理前不得破坏其它共享夹具。

## Open Questions

- 真实写入型 E2E 需要用户提供或确认两个生产组长、一个员工、路线工序和报工样本数据；当前环境未注入这些前置变量。

## Test Blockers

- 缺本机数据库、缺正式工艺路线工序开始配置、缺生产组长/员工测试账号、缺报工真实入口、或 worktree 服务无法在登记端口启动时，必须记录 BLOCKED，不能使用 mock、默认原因或 API-only 替代。
