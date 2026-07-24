# Word 导入升版审批闭环

## Task Goal

完成批记录 Word 升版导入后的审批闭环：生成 V2.0 等升版快照后，用户可在可见页面查看待审批版本，由非提交人执行通过或驳回，审批通过后切换当前可用版本，驳回后保留旧版本不被覆盖。

## Milestones

1. 补齐 BDD/TDD，覆盖待审批列表、提交人自审阻断、审批通过生效、审批驳回不生效。
2. 后端补充待审批查询与人工审核接口，复用现有版本审批事务、事件幂等和当前版本切换保护。
3. 前端在 eDHR 版本治理页增加“升版审批”入口，展示待审版本并提供通过/驳回操作。
4. 运行后端定向测试、前端静态契约测试和必要回归，记录 RED/GREEN/REGRESSION。
5. 修复 Word 升版确认弹窗版本口径：已有 V2.0 等最新版本时，确认文案不得只显示当前生效源版本 V1.0；预检同时返回当前生效版本、最新已生成版本和正确下一版本号。
6. 按用户反馈收敛审批中心视图：签名待处理、候选签名、待审核和升版审批等待处理事项全部进入“待办”，不再暴露独立“签名待处理”视图或菜单。

## Expected Verification

- 后端定向测试覆盖：待审批列表只返回 `PENDING_APPROVAL` 版本；提交人不能审批自己；非提交人通过后版本变 `APPROVED` 且定义 currentVersion 切换；非提交人驳回后版本变 `REJECTED` 且 currentVersion 不变。
- 前端静态契约覆盖：版本治理页有升版审批表格、刷新按钮、通过/驳回按钮、二次确认，并调用后端待审列表和审核接口。
- 用户 2026-07-13 明确授权后，补充本机 `芋道源码/admin` 写入型完整审批 E2E：导入 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 生成升版快照，提交升版审批，在版本治理页通过审批，并核验版本生效、旧版本不被覆盖、待审列表清空。
- 不修改数据库 schema；不引入 fallback；不使用 mock 成功。

## 经验门禁

- PowerShell：中文文件读写、测试命令和日志记录必须显式 UTF-8，禁止默认 `Get-Content`/`Set-Content` 污染中文。
- 前端页面/表格/样式：遵循 IntPP 运营台风格，审批列表使用紧凑表格、短状态标签和明确操作按钮。
- 登录/E2E：如执行真实 E2E，必须先读取 `docs/login-access.md` 并跑官方登录预检；默认只允许测试租户写入。用户已在 2026-07-13 明确授权本任务在 `芋道源码/admin` 做写入型完整审批 E2E，范围仅限本任务 Word 升版导入与审批闭环，不扩展到服务器、SQL 直写或其它业务数据修复。
- 批记录 Word 表单识别：本任务只补审批闭环，不改 Word 解析、结构化、视觉网格和导入算法。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，新增正式待审入口和人工审核接口，闭合提交、审核、生效/驳回链路。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress

## Verification Result

- PASS：旧 HEAD 静态 RED 复核确认后端缺少 `/version-approval/pending` 与 `/version-approval/review`，前端缺少待审升版列表 API 和“升版审批”入口。
- PASS：后端补齐待审查询与人工审核接口，复用版本审批事务、提交人自审阻断、审批事件记录和 currentVersion 切换保护。
- PASS：前端 eDHR 版本治理页新增“升版审批”区块，说明审核人是具备版本审批权限且不是提交人的账号，入口就在本页；支持刷新、通过、驳回和错误提示。
- PASS：`node tests\e2e\batch-record-word-import-dialog-ui-static.spec.js`，确认红框内重建产线候选不会默认选中，黄框文件名/格式提示不再显示。
- PASS：`node tests\e2e\batch-record-version-approval-closure-static.spec.js`，确认前端待审列表、审核操作和权限契约。
- PASS：`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_pendingListAndManualApproveCloseLoop,MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_manualRejectKeepsCurrentVersionAndRecordsReason" "-Dsurefire.failIfNoSpecifiedTests=false" test`，2 tests。
- PASS：`task_closeout.py --task-id 20260713-word-import-version-approval-closure --mode preview/apply --worktree-closeout off`，无删除项、无阻塞、核心证据保留。
- 2026-07-13 追加范围：用户已明确授权在本机 `芋道源码/admin` 执行写入型完整审批 E2E，待执行并补充证据。
- BLOCKED：完整审批 E2E 需要“提交人 != 审核人”；当前官方登录预检确认 `admin/admin123` 可用，但现有 `smokeappr1` 使用 `111111` 与 `admin123` 均登录失败，离线哈希比对也确认 `smokeerp1/smokeplan1/smokeappr1/smokeread1` 不匹配常用口令，且库内没有非 admin 提交的 `PENDING_APPROVAL` 待审版本。继续完成正向审批闭环前，需要用户授权创建或重置一个仅用于本次 E2E 的临时提交账号，或提供可登录的非 admin 提交账号。
- PASS：已修复用户反馈“当前版本是 V2.0，确认升版弹窗显示 V1.0”。前端确认弹窗改为优先显示“最新批记录版本 V2.0”，仅将 V1.0 表述为“当前生效源版本”；后端预检和控制器测试确认 latest 字段完整透传。
- PASS：按用户反馈“应该都放到待办里”，已将后端 provider 声明、前端 API 类型、个人工作台入口和静态/单元合同统一调整为 4 个可见视图：待办、已办、我发起的、抄送我的；签名待处理只作为待办中的任务特征，不再作为独立视图。
- PASS：`node tests\e2e\approval-center-phase4-static.spec.mjs && node tests\e2e\approval-center-standard-list-template-static.spec.js && node tests\e2e\batch-record-version-approval-closure-static.spec.js`。
- PASS：`mvn.cmd -pl yudao-module-bpm,yudao-module-dcc,yudao-module-mes,yudao-module-showroom -am "-Dtest=ApprovalModuleIntegrationGuardTest,DccApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterTest,ShowroomApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，BUILD SUCCESS。
- RED：本机 `芋道源码/admin` 真实页面导入 `批记录压力泵.doc` 后未形成 2.0 到 3.0 审批。E2E 结果为 `versionId=75`、`versionNo=V2.0`、`versionStatus=PRECHECK_PASSED`、`submitRequestCount=0`、`pendingFound=false`、`todoFound=false`；说明当前导入只生成预检通过快照，没有自动提交升版审批，也没有进入审批中心待办。

## Cleanup Keep

- `doc/tasks/20260713-word-import-version-approval-closure/backend-api-evidence.md`
- `doc/tasks/20260713-word-import-version-approval-closure/frontend-feature-evidence.md`
