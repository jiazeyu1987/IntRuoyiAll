# Execution Log

## User Intent

- 2026-08-05：用户基于截图反馈审批中心“申请人”列显示 `用户 #1` / `用户 #151`，要求显示用户姓名。

## BDD / TDD

BDD: 申请人列显示姓名 -> Given 审批任务响应包含申请人用户姓名 When 用户打开审批中心任一列表 Then “申请人”列显示该姓名而不是 `用户 #<id>`。

BDD: 姓名缺失时保持可追踪显示 -> Given 审批任务只有申请人用户 ID 但没有姓名 When 用户打开审批中心列表 Then “申请人”列显示 `用户 #<id>`，姓名和 ID 都缺失时显示 `--`。

## Milestone Updates

### M1 基线与契约

- 状态：completed。
- 已创建任务目录：`doc/tasks/20260805-approval-center-applicant-name/`。
- 既有共享分支基线已由前序步骤提交：`1d145ff95 Baseline: preserve concurrent task docs before applicant name fix`。
- 确认正式姓名来源：统一审批中心服务已注入 `AdminUserApi`，原有审核人姓名通过 `adminUserApi.getUserMap(...)` 补全；本任务复用同一正式用户源补全申请人姓名，不由前端硬编码或推断。

### M2 RED

- 状态：completed。
- RED: `node tests\e2e\approval-center-applicant-column-static.spec.js` -> FAIL，期望失败：前端 API 类型缺少 `initiatorUserName?: string`。
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest,ApprovalCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，期望失败：`ApprovalTaskSummary` 缺少 `getInitiatorUserName()`。

### M3 GREEN

- 状态：completed。
- 后端：`ApprovalTaskSummary` 新增 `initiatorUserName`；`ApprovalCenterServiceImpl` 合并 `initiatorUserId/assigneeUserId` 后一次性调用 `AdminUserApi.getUserMap(...)`，补全申请人和审核人姓名。
- 前端：`ApprovalTaskSummaryVO` 新增 `initiatorUserName?: string`；`resolveApplicantLabel` 优先显示 `row.initiatorUserName`，姓名缺失时保留 `用户 #<id>`，姓名和 ID 都缺失时显示 `--`。
- GREEN: `node tests\e2e\approval-center-applicant-column-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest,ApprovalCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，21 tests。

### M4 回归与真实 E2E

- 状态：blocked。
- REGRESSION: `node tests\e2e\approval-center-standard-list-template-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-chinese-copy-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-reviewer-column-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-signature-pending-standard-list-static.spec.js` -> PASS。
- BLOCKED: `pnpm ts:check` -> FAIL，阻塞点为无关并行改动 `src/components/UnifiedListTemplate/index.vue(339,8)` emit 类型不匹配。
- E2E: `node doc\tasks\20260805-approval-center-applicant-name\approval-center-applicant-name-real.e2e.cjs` -> PASS；真实页面四个审批中心页签只读验证，`namedResultCount=30`、`pageErrors=[]`、`targetNetworkFailures=[]`、`targetWriteRequests=[]`。
- E2E 归因：页签导航中止的两个 `GET /approval-center` 读请求记录为 `abortedReadRequests`，不属于目标链路失败。
- 本任务临时热补丁目录和失败截图已清理；保留 E2E 脚本和通过结果 JSON 作为任务证据。
- 经验沉淀：已检查 `docs/experience-index.md`、`docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/powershell-memory.md`，本轮真实 E2E 导航中止归因、嵌套 Jar 未压缩热替换、Maven 卡住诊断均已有长期门禁覆盖，未新增长期经验文档。

## Blockers

- 收尾阻塞：`pnpm ts:check` 当前失败在无关并行任务修改的 `IntRuoyiFronted/src/components/UnifiedListTemplate/index.vue(339,8)`，本任务未修改该文件，按门禁暂不提交/推送。
