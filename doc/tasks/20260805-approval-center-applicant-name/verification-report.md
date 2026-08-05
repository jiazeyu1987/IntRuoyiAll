# 审批中心申请人显示姓名验证报告

## 结论

- 功能结论：PASS。审批中心“申请人”列已优先显示后端正式解析的用户姓名，姓名缺失时仍保留 `用户 #<id>` 可追踪显示。
- 收尾结论：BLOCKED。全量 `pnpm ts:check` 失败于无关并行改动 `UnifiedListTemplate/index.vue(339,8)`，本任务未提交/推送。

## 验证命令

- RED: `node tests\e2e\approval-center-applicant-column-static.spec.js` -> FAIL，缺少 `initiatorUserName?: string`。
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest,ApprovalCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `getInitiatorUserName()`。
- GREEN: `node tests\e2e\approval-center-applicant-column-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest,ApprovalCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，21 tests。
- REGRESSION: `node tests\e2e\approval-center-standard-list-template-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-chinese-copy-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-reviewer-column-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\approval-center-signature-pending-standard-list-static.spec.js` -> PASS。
- BLOCKED: `pnpm ts:check` -> FAIL，`src/components/UnifiedListTemplate/index.vue(339,8)` emit 类型不匹配，属于当前共享工作区无关并行改动。
- E2E: `node doc\tasks\20260805-approval-center-applicant-name\approval-center-applicant-name-real.e2e.cjs` -> PASS。

## 真实 E2E 证据

- 入口：`http://127.0.0.1:8081`，本机默认身份标签 `芋道源码/admin`。
- 运行态：前端 8081 HTTP 200，后端 48081 health `UP`。
- 页面路径：`/approval-center/todo`、`/approval-center/done`、`/approval-center/my-initiated`、`/approval-center/cc`。
- 结果摘要：`namedResultCount=30`，待办、已办、我发起的页签均出现正式申请人姓名，例如 `瑛泰管理员(admin)`、`彭云凤(pengyunfeng)`、`王思雨(wangsiyu)`。
- 目标链路：`pageErrors=[]`、`consoleErrors=[]`、`targetNetworkFailures=[]`、`targetWriteRequests=[]`。
- 归因说明：两个 `GET /admin-api/approval-center/... net::ERR_ABORTED` 为页签导航中止的只读请求，已在结果 JSON 中单独记录为 `abortedReadRequests`。
- 结果文件：`doc/tasks/20260805-approval-center-applicant-name/e2e-artifacts/approval-center-applicant-name-result.json`。

## 未完成项

- `pnpm ts:check` 需要等并行任务修复或归属确认 `UnifiedListTemplate/index.vue` 的 emit 类型问题后复跑。
- 因全量类型检查未通过，按项目提交门禁暂不执行实现提交、收尾提交和推送。
