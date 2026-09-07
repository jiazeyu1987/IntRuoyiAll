# DCC 发布通知与关联影响评估测试计划

## Purpose and Scope

为 PRD 的 AC-01 至 AC-18 定义严格 TDD、失败路径、真实数据和 Playwright 验收。生产代码实施前先运行相应 RED；每个 GREEN 必须覆盖可观察业务结果。

## Evidence Reviewed

- `prd.md`、`user-flows.md`、`acceptance-criteria.md` 和 `development-plan.md`。
- 当前 finalization、related-file、message-job、DCC detail/workbench 和 major-revision 测试。
- 项目 BDD/TDD、E2E、数据库、Long ID 和 no-fallback 规则。

## BDD Scenarios

### BDD-01 发布与账本原子提交

- Given: B/1 已批准，A/1 是当前正式版本。
- When: 文控发布 B/1。
- Then: B/1 ACTIVE、A/1 SUPERSEDED、Master 指向 B/1，并存在唯一后续批次和冻结快照；任何账本写入失败时全部回滚。
- Covers: AC-01、AC-02、AC-14。

### BDD-02 可见范围与通知收件人分离

- Given: 一名用户同时命中文件责任人、分发对象和 VIEW 规则。
- When: 系统生成后续记录。
- Then: 可见范围保留全部规则来源和发布时解析用户清单，但该用户只有一条通知并显示两个责任来源；消息不授予权限。
- Covers: AC-03、AC-04、AC-05。

### BDD-03 正反向关系按 Master 去重

- Given: X 正向关联 Y，Y 也引用 X，Z 当前正式版本反向引用 X。
- When: X 新大版本发布。
- Then: Y、Z 各一个任务，Y 任务保存两个方向，所有任务冻结发布时版本。
- Covers: AC-06、AC-07。

### BDD-04 负责人缺失与文控转派

- Given: Y 当前责任人停用。
- When: 创建影响任务。
- Then: 任务 UNASSIGNED 并进入文控列表；文控填写原因转派后新负责人可处理，停用用户不收消息。
- Covers: AC-08、AC-09。

### BDD-05 无需升版

- Given: 当前用户是影响任务负责人。
- When: 选择无需升版并填写原因。
- Then: 任务完成且相关文件版本、状态、正式指针完全不变；并发第二次提交失败。
- Covers: AC-10、AC-11。

### BDD-06 需要升版

- Given: 负责人选择需要升版。
- When: 提交决定并点击开始升版。
- Then: 不自动建版，只进入现有 major-revision 路径；已有开放版本时只能关联；关联版本发布后任务 RESOLVED。
- Covers: AC-12、AC-13。

### BDD-07 通知失败隔离和重试

- Given: 三个收件人中一个发送失败。
- When: 发布后发送消息并由文控重试。
- Then: 两个 SENT、一个 FAILED、文件仍 ACTIVE；重试只处理失败用户，SENT 不重放。
- Covers: AC-14、AC-15。

### BDD-08 小版本无发布后续

- Given: A/1 已检出。
- When: 检入生成 A/2。
- Then: 不产生发布批次、通知或影响任务。
- Covers: AC-16。

### BDD-09 真实页面闭环

- Given: 测试租户有两份新文件、关联关系、文控和责任人账号。
- When: Playwright 完成发布、查看通知、评估决定和升版入口。
- Then: 页面与只读 API/DB 一致，业务写动作全部来自前端，控制台无错误。
- Covers: AC-17、AC-18。

## TDD Sequence

1. Schema/mapper RED -> migration + DO/Mapper GREEN。
2. Publication atomicity/idempotency/relation snapshot RED -> P1 service GREEN。
3. Impact task permission/CAS/decision/revision-link RED -> P2 service GREEN。
4. Notification dedup/failure/retry/access RED -> P3 service GREEN。
5. Frontend API/route/component static RED -> DCC detail/workbench/admin UI GREEN。
6. Adjacent lifecycle regression -> runtime migration/restart -> Playwright -> independent tester。

## RED Commands

- `mvn -o -pl yudao-module-dcc -Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest test`
  - Expected failure: 批次、快照 schema 和发布接入不存在。
- `mvn -o -pl yudao-module-dcc -Dtest=DccRelatedFileImpactAssessmentServiceTest test`
  - Expected failure: 正反向去重、负责人、决定和升版跟踪不存在。
- `mvn -o -pl yudao-module-dcc -Dtest=DccPublicationNotificationServiceTest test`
  - Expected failure: 新业务类型、幂等收件人和失败重试不存在。
- `node tests/e2e/dcc-release-impact-workbench-static.spec.js`
  - Expected failure: 工作台、详情和文控管理入口不存在。

## Expected Failures

- 发布后只有 ACTIVE/SUPERSEDED，没有后续批次。
- 当前关系服务只列正向版本级关系，不能解析反向引用或按 Master 去重。
- 当前消息服务没有发布/影响任务业务类型，也没有发布批次幂等键。
- 当前工作台只有审批待办，没有影响评估页签。

## GREEN Commands

- `mvn -o -pl yudao-module-dcc -Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccRelatedFileImpactAssessmentServiceTest,DccPublicationNotificationServiceTest,DccImpactAssessmentQueryServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest test`
- `python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file <complete dependency closure>`
- `node tests/e2e/dcc-release-impact-workbench-static.spec.js`
- `node tests/e2e/dcc-detail-publication-followup-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- 相邻 DCC 新文件生命周期组合测试必须继续通过。

## Refactor Checks

- 发布事务只调用一个后续账本端口，不在 finalization 中展开收件人和任务细节。
- 影响评估服务复用现有 project OWNER 和 major-revision 合同，不复制权限逻辑。
- 通知复用幂等系统 API，不新增直接写 `system_notify_message` 的旁路。
- 所有分页一行对应一个批次或任务；一对多收件人/方向先聚合后查询，禁止前端去重。
- Snowflake ID 在 DTO、路由和比较中保持字符串。

## Test Cases

| ID | Scenario | Covers | Evidence |
| --- | --- | --- | --- |
| T01 | 发布账本与状态原子性 | AC-01, AC-02 | service fault injection + transaction test |
| T02 | 重复发布幂等 | AC-14 | unique-key/concurrency test |
| T03 | 可见范围快照不改写 | AC-03 | permission mutation test |
| T04 | 收件人来源合并去重 | AC-04 | resolver test |
| T05 | 通知不授权 | AC-05 | access service/controller test |
| T06 | 正向、反向、双向去重 | AC-06, AC-07 | relation integration test |
| T07 | 停用负责人和转派 | AC-08, AC-09 | user-state/permission test |
| T08 | 无需升版零副作用 | AC-10, AC-11 | CAS + before/after file snapshot |
| T09 | 需要升版与现有开放版 | AC-12, AC-13 | workflow integration test |
| T10 | 通知部分失败与重试 | AC-14, AC-15 | delivery fault test |
| T11 | 小版本不生成后续 | AC-16 | check-in regression |
| T12 | 页面/数据库一致 | AC-17 | frontend contracts + read-only verify |
| T13 | 真实页面完整链 | AC-18 | Playwright |

## Failure Paths

- 批次、范围快照或关系快照任一插入失败：发布事务回滚，旧版本继续 ACTIVE。
- 消息模板缺失、单用户发送失败：对应行 FAILED，其他行继续，发布不回滚。
- 相关版本、负责人或用户状态不可解析：任务 UNASSIGNED/明确 blocker，不使用默认值。
- 越权处理、空原因、旧版本号、并发 CAS：请求失败，决定和文件状态不变。
- 已存在开放大版本：禁止重复创建，只允许显式关联。
- 通知链接权限变化：按当前 VIEW 拒绝，不显示正文。
- 页面缺入口、账号或测试数据：E2E BLOCKED，不用 API 或 SQL 写入替代。

## E2E

### User Path

1. Playwright 登录测试租户文控账号，通过真实页面发布带任务标识的 B/1。
2. 在详情页确认 B/1 ACTIVE、旧版 SUPERSEDED 和发布后续摘要。
3. 登录实际收件账号，从站内通知进入详情并确认当前权限行为。
4. 登录关联文件负责人，在“我的影响评估”处理一项无需升版任务。
5. 处理另一项需要升版任务，点击开始升版或关联已有开放版本。
6. 文控页面检查消息 SENT/FAILED、任务决定与升版链接。
7. 业务动作完成后，API/DB 只读核验批次、收件人、任务、审计和版本状态。

### Browser Rules

- 必须使用 Playwright 真实页面和真实测试账号。
- 禁止 `fetch`、`apiGet`、直接 HTTP 或数据库写入承担业务动作。
- 截图不得包含密码、token 或完整敏感请求头。
- 浏览器 console 必须无目标链路 error。

## Test Data

- Source file X: 新建并可完成 A/1 -> A/2 -> B/1 的任务自有文件。
- Related file Y: 与 X 正向关联且有有效责任人。
- Referencing file Z: 当前正式版本反向引用 X，负责人不同于 Y。
- Users: 文控、X 责任人、Y 责任人、Z 责任人、分发对象、无权限用户。
- Failure fixture: 一个明确可恢复的通知发送失败场景，仅在测试实现支持确定性注入时使用；不能用 mock 冒充最终 E2E。

## Reset Procedure

- 测试数据使用统一任务标识。
- 清理只能通过正式页面提供的撤回/删除入口处理允许清理的工作数据；已发布版本和审计不得物理删除。
- 无法清理的发布历史保留为测试证据，报告精确记录文件编号和范围，不扩展到历史数据治理。

## Data Ownership

- 所有新文件、关系、通知和任务属于本任务测试租户和任务标识。
- 不复用或修改既有业务文件作为写入验收对象。

## API Verification

- 仅在页面业务动作结束后执行只读列表/详情核验。
- 校验批次唯一、收件人去重、任务按 Master 去重、状态、快照、审计和 Long ID。

## Console and Log Checks

- 页面 console 目标链路 0 error。
- 后端日志无吞异常、默认用户、重复键未处理或通知伪成功。
- 日志只记录脱敏错误摘要，不记录消息正文敏感信息或凭据。

## Evidence Log Template

- `BDD: <scenario> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS, <count/result>`
- `E2E: <page path> -> PASS/BLOCKED, <artifact and visible result>`
- `READ-ONLY VERIFY: <scope> -> PASS, <counts/statuses>`

## Test Blockers

- 未获当轮 E2E、数据库迁移、48081 重启或测试账号写入授权时，不运行真实写入验收。
- 缺少两个以上任务自有新文件、正反向关系或有效责任人账号时，T13 保持 BLOCKED。
- 通知失败无法在真实环境确定性触发时，单元/集成故障注入可验证失败合同，但不得宣称真实失败重试 E2E 已覆盖。
