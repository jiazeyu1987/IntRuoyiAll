# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 优化 DCC 受控打印完成反馈、最新记录高亮、无权限提示、结构化字段、副本编号和直接打印策略可见性。
- Non-goals: 不修改其它 DCC 上传、发布、分发、培训、MES/eDHR 场景；不通过 API-only 或 SQL 创建打印记录。

## Requirements And Acceptance IDs

- A1: 打印成功后结果弹窗展示打印编号、份数、打印人、打印时间和查看记录入口。
- A2: 打印记录区自动定位并高亮本次打印记录。
- A3: 无打印权限账号同文件入口不显示受控打印按钮，并看到明确权限说明。
- A4: 接收部门和使用位置支持结构化选择或常用值，提交后追溯字段一致。
- A5: 多份打印显示逐份副本编号。
- A6: 直接打印策略显性化，当前状态为 `DIRECT_PRINTED`。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: `http://127.0.0.1:8081/dcc/controlled-file/browser`
- Detail route: `/dcc/controlled-file/detail/:id`
- Components:
  - `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`
  - `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- Supporting backend projection:
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java`
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`

## API Contracts And Data States

- Print action creates records through the real controlled-print page flow.
- Records API returns print ID, print number, file number, version, copies, receiving department, use location, print user, print time, and approval status.
- Current file state is `ACTIVE`; master current active pointer matches the printed controlled file ID.
- Direct print status is `DIRECT_PRINTED`; no approval user is expected for this system policy.

## BDD Scenarios

- BDD: 打印完成后展示可审计结果 -> Given 有打印权限的非 admin 用户打印当前 ACTIVE 受控文件 When 页面提交受控打印 Then 页面显示成功结果弹窗 And 弹窗展示打印编号、份数、打印人、打印时间、副本编号和直接打印策略 And 用户可点击查看打印记录定位到本次记录。
- BDD: 最新打印记录自动定位高亮 -> Given 用户完成一次受控打印 When 用户点击查看打印记录或记录区刷新 Then 打印记录表自动滚动到本次记录 And 最新记录以高亮样式展示一段时间。
- BDD: 无打印权限时给出明确原因 -> Given 非 admin 用户无同一文件 PRINT 权限 When 用户进入同一 ACTIVE 文件受控浏览或详情页 Then 页面不显示受控打印按钮 And 显示只读权限提示说明当前用户无受控打印权限或当前文件类别不允许打印。
- BDD: 打印表单结构化减少追溯歧义 -> Given 用户打开受控打印表单 When 填写接收部门和使用位置 Then 接收部门可从组织部门选择 And 使用位置可从常用位置选择或输入新位置 And 提交后记录中保留标准化文本。
- BDD: 多份打印显示逐份副本编号 -> Given 用户打印份数大于 1 When 打印件、成功弹窗和记录区展示打印结果 Then 每份副本都有可见副本编号或编号范围，用于后续盘点追溯。

## RED Command And Expected Failure

- RED: `node doc\tasks\20260803-dcc-controlled-print-ux-optimization\dcc-controlled-print-ux-static.spec.cjs` -> FAIL, expected pre-implementation failure for missing UX contract selectors and assertions.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPrintContractTest" test` -> FAIL, expected reactor-scoped failure because sibling modules have no matching test unless `surefire.failIfNoSpecifiedTests=false` is set.

## GREEN Command And Passing Result

- GREEN: `node doc\tasks\20260803-dcc-controlled-print-ux-optimization\dcc-controlled-print-ux-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` in `IntRuoyiFronted` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPrintContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `node doc\tasks\20260803-dcc-controlled-print-ux-optimization\dcc-controlled-print-ux-real.e2e.cjs` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Permission: negative account `zhangkeying` sees same ACTIVE file row but no controlled print button and sees “无受控打印权限” in detail.
- Loading: E2E waits for controlled browser payload, print dialog, print window and records reload before assertions.
- Empty/error: E2E result has `targetNetworkFailures=[]`, `targetBadResponses=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- Accessibility: result dialog and controlled print actions are verified through role-based button locators.

## E2E Or Component Verification Path

- Real path: login -> controlled browser -> ACTIVE file -> controlled print dialog -> submit -> print window -> result dialog -> view print records -> negative account permission block.
- Final artifacts:
  - `doc/tasks/20260803-dcc-controlled-print-ux-optimization/dcc-controlled-print-ux-real-e2e-result.json`
  - `doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-window-20260802184519.png`
  - `doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-records-20260802184519.png`
  - `doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-negative-20260802184519.png`

## Blockers And Follow-Up Skills

- No active E2E blocker remains.
- Closeout/commit is not performed in this report because the shared `int_main` workspace contains unrelated dirty changes from other tasks; task-owned verification artifacts are ready for closeout.
