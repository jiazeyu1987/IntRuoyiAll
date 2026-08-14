# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: 在生产组长报工列表提供可读的“修改记录”入口，以业务时间线展示修改人、时间、原因、电子签名状态和字段前后值。
- Non-goal: 不展示或编辑原始 payload、签名快照、字段代码、事件号、修订号、用户号或签名号；不改变报工修改权限和路线快照策略。

## Requirements And Acceptance

- FE-LOG-01: 生产报工行提供“修改记录”入口。
- FE-LOG-02: 日志按最新在前展示业务字段，不显示内部协议字段。
- FE-LOG-03: 提供加载、空记录、失败重试和移动端可读状态。
- FE-LOG-04: 修改成功后可从正式查询接口读到新日志。

## Entry And Owned Files

- Route: `/mes/pro/process-pool/production-leader`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- API: `IntRuoyiFronted/src/api/mes/pro/processpool/eventRevision.ts`
- Test: `IntRuoyiFronted/tests/e2e/production-report-correction-human-ui-static.spec.cjs`

## API And Data States

- `GET /mes/pro/process-pool/event-revision/production-report-logs?eventId=<id>`
- Success: ordered log items with `modifiedByName/modifiedAt/changeReason/signatureConfirmed/changes`.
- Empty: `[]` and explicit empty state.
- Error: preserve dialog context, show server error and retry action.

## BDD

- Given a corrected production report / When the leader opens modification records / Then show who changed what, when, why, and the signed status in readable Chinese.
- Given no corrections / When the log opens / Then show a genuine empty state.
- Given the query fails / When the log opens / Then show the failure and retry without presenting a false empty state.

## RED / GREEN

- RED: `node --test tests/e2e/production-report-correction-human-ui-static.spec.cjs` -> FAIL，新增 2 项日志合同按预期失败：缺少入口、弹窗和 API。
- GREEN: pending.

## Responsive And Accessibility

- Pending desktop and 430x932 verification; dialog must scroll internally without hiding footer actions.
- Stable data markers and readable labels are required for the real path.

## Blockers

- None identified.
