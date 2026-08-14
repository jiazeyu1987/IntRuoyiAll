# Frontend Feature Evidence

## Scope

- 班组长工作台提交看板增加“复核判定”日志列。
- 复核弹窗文案改为“正确/不正确”，贴合用户业务表达。
- 组长可从检查列表打开“修正不正确内容”弹窗，提交正式原始记录修订。
- PQC 详情增加 `PQC提交日志`，展示提交事件编号、PQC 检验员、服务端提交时间和原始 payload。

## Acceptance

- 组长可以对每个提交判定正确/不正确，并在列表回看最新判定、说明、复核人和复核时间。
- 组长修正不正确内容时必须提交修改原因、修正签名和字段差异，走正式原始记录修订接口。
- PQC 检验员提交内容在详情中以提交日志形式展示事件编号、提交人、时间和原始 payload。

## Non-Goals

- 不在复核接口中直接修改原始 payload。
- 不新增 mock 数据、默认日志或静默错误。
- 不改变现有 PQC 提交流程的数据源。

## UI Entry Points and Owned Files

- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `IntRuoyiFronted/src/api/mes/pro/processpool/index.ts`
- `IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js`

## API Contracts and Data States

- 列表/详情读取 `submissionReviewStatus`、`submissionReviewRemark`、`submissionReviewLeaderUserId`、`submissionReviewedAt`。
- 修正提交调用 `updateProcessPoolOriginalRecord`，要求修改原因、签名、签名快照、修改人、修改后 payload 和字段差异。
- 复核提交仍调用 `reviewTeamLeaderSubmission`，仅提交判定状态和说明。

## BDD

- `BDD: 组长判定员工提交是否正确 -> Given 员工或PQC检验员提交了一条工序池事件 / When 组长在检查列表复核该提交 / Then 组长可以标记正确或不正确并保存复核说明`
- `BDD: 组长修改不正确内容留痕 -> Given 组长判定提交内容不正确 / When 组长提交修正后的字段内容和修改原因 / Then 系统保存修正内容并记录修改前、修改后、修改人、修改时间和原因日志`
- `BDD: PQC提交日志可追溯 -> Given PQC检验员提交过程检验内容 / When 组长或审核视图查看该提交 / Then 系统展示PQC提交日志，包含提交人、提交时间、原始payload和提交事件编号`

## RED

- `RED: node tests\e2e\mes-process-pool-team-leader-static.spec.js -> FAIL, 缺少复核日志字段和修正入口契约`

## GREEN

- `GREEN: node tests\e2e\mes-process-pool-team-leader-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Verification

- 静态契约确认列表展示 `data-team-leader-review-log`、详情展示 `data-pqc-submission-log`、修正调用 `updateProcessPoolOriginalRecord`。
- 类型检查确认新增字段、修正表单和 API 调用类型一致。

## UX and Error States

- 原始 payload 缺失或不是合法 JSON 时，不打开修正弹窗并提示错误。
- 修正表单缺少原因、签名、签名快照或字段差异时，前端直接报错，不提交默认值。
- 后端 API 错误通过现有 `ElMessage.error` 展示，不隐藏。

## Blockers

- 未跑真实 Playwright E2E：本次未启动本地前后端服务，按任务已有静态契约和定向后端测试完成验证。
- 提交/推送未执行：共享工作区存在大量并行改动。
