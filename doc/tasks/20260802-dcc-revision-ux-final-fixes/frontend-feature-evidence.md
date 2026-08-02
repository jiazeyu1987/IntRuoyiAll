# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 修复 DCC 升版/修订发布链路 3 个前端体验缺口：版本历史变更说明、发布完成摘要、BPM markers pageerror。
- Non-goals: 不修改后端审批、状态机、master 指针、签名证据、数据库数据或真实 E2E 业务数据。

## Requirements And Acceptance IDs

- A1: 版本历史弹窗标题必须为“版本历史”，版本历史表必须显示“升版原因/变更说明”。
- A2: 发布完成后的详情页必须显示新版 ACTIVE、旧版 SUPERSEDED、master 当前生效版本、受控浏览落位摘要。
- A3: BPM 流程图高亮缺失节点不得产生 `markers` pageerror，必须显示可见警告。

## UI Entry Points And Owned Files

- DCC 受控文件详情页：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- BPM 流程图 viewer：`IntRuoyiFronted/src/components/bpmnProcessDesigner/package/designer/ProcessViewer.vue`
- 聚焦静态契约：`IntRuoyiFronted/tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs`

## API Contracts And Data States

- 使用既有 `ControlledFileVO.versionHistory[].remark` 展示升版原因/变更说明。
- 使用既有 `status/currentActiveVersionNo/publishedFileId/stampedFileId/supersededByFileId/versionHistory` 推导发布完成摘要。
- 不新增 API 字段，不改变请求参数，不修改后端状态。

## BDD Scenarios

- BDD: 版本历史展示升版原因 -> Given 已生效升版文件有 V1/V2 版本历史和提交备注 When 用户打开版本历史 Then 表格展示升版原因/变更说明。
- BDD: 发布完成结果摘要 -> Given V2 发布完成 When 用户进入详情 Then 结果摘要证明 V1 失效、V2 生效、master 切换、受控浏览落位。
- BDD: BPM markers pageerror 防护 -> Given BPM 高亮节点缺失 When 打开流程图 Then 页面显示高亮不完整警告且无 markers pageerror。

## RED Command

- RED: `node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` -> FAIL, expected reason: preview version dialog title still used `版本信息`, and the focused assertions for version change reason, publish completion summary, and BPM marker guard were absent before implementation.

## GREEN Command

- GREEN: `node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/dcc-upload-governance-ux-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/dcc-browser-version-summary-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `$env:DCC_E2E_PASSWORD = -join (1..6 | ForEach-Object { [char]49 }); node doc/tasks/20260802-dcc-revision-ux-final-fixes/dcc-revision-ux-final-real-e2e.cjs` -> PASS.

## Responsive Accessibility Loading Empty Error Permission Checks

- 版本历史和发布摘要沿用现有 Element Plus 表格、tag、card 样式。
- BPM 高亮缺失使用 `el-alert` 暴露错误归属，不吞异常、不阻断页面其它 DCC 审批处理。

## E2E Or Component Verification Path

- 本任务以聚焦静态契约、相邻静态回归和只读真实 Playwright E2E 复验前端行为；不使用 API-only 代替真实 E2E，也不创建写入型业务数据。
- 完整升版发布真实 E2E 已由 `doc/tasks/20260802-dcc-revision-publish-real-e2e/verification-report.md` 覆盖；本任务仅收口该真实链路复查后提出的 3 个前端 UX/稳定性缺口。
- 只读真实 E2E 结果：`doc/tasks/20260802-dcc-revision-ux-final-fixes/real-e2e-result.json`，`status=PASS`，覆盖发布完成摘要、受控浏览版本历史弹窗和 BPM markers pageerror。

## Blockers And Follow-Up Skills

- 当前共享工作区存在非本任务脏改动和 ahead-of-origin 状态；提交/推送需按项目 closeout 门禁另行处理。
