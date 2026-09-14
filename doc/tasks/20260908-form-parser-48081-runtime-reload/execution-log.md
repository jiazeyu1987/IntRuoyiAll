# Execution Log

## BDD Scenarios

BDD: 48081 运行态加载生产批记录总识别接口 -> Given 源码包含 `POST /mes/pro/batch-record-report/production-batch-record/total-recognition-json` / When 重建并重启 int_main 后端 / Then 登录态请求不再返回“请求地址不存在”，而是进入正式 Controller。

BDD: 真实 Word 解析为批记录总识别 JSON -> Given 用户指定的生产批记录 `.doc` 文件 / When 管理员登录态上传到生产批记录总识别接口 / Then 响应 `code=0`，`data` 为包含 `product/schemaVersion/processes` 的 JSON 字符串，且 `processes` 非空。

BDD: 48081 不被 GxP 审计构造器阻塞 -> Given `GxpAuditTrailServiceImpl` 同时保留运行态构造器和测试时钟构造器 / When 后端 Spring 容器启动 / Then Spring 使用显式标注的 mapper 构造器注入，不再查找不存在的默认构造器。

## Evidence

- in_progress: 已建立任务记录，准备执行运行态复现、重建、重启和登录态验证。
- Runtime check: 48081 监听 PID 40408 属于 `E:\IntRuoyi\output\runtime\int_main\backend-form-parser-total-recognition-20260908-203004.jar`，`/actuator/health` 返回 `UP`；匿名 POST 目标 URL 返回 `401 账号未登录`，说明当前运行态已进入认证链路，不再是 MVC mapping 缺失。
- BLOCKED TARGETED TEST: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL before MES tests because unrelated untracked `yudao-module-signature/src/test/java/.../gxp/GxpAuditTrailServiceContractTest.java` imports missing production classes.
- Isolation decision: 主工作区存在并行 GxP 脏改动阻断 Maven testCompile；按 `docs/local-runtime.md` 的隔离构建 Jar 加载门禁，改用干净 detached worktree 从当前 `origin/int_main` 构建后端 Jar，再复制到稳定运行目录并重启 48081。
- Runtime failure: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260908-214708.out.log` 显示 2026-09-08 21:48:18 后端启动失败，根因为 `GxpAuditTrailServiceImpl`: `No default constructor found`，导致 48081 无监听。
- RED: `mvn -pl yudao-module-signature -am "-Dtest=GxpAuditTrailServiceContractTest#springRuntimeUsesExplicitMapperConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增反射合同证明运行态构造器缺少 `@Autowired`。
- Fix: `GxpAuditTrailServiceImpl(GxpAuditEventMapper)` 增加 `@Autowired`，保留 `(GxpAuditEventMapper, Clock)` 测试构造器且不作为 Spring 运行态注入入口。
- GREEN: `mvn -pl yudao-module-signature -am "-Dtest=GxpAuditTrailServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, failures 0, errors 0。
- GREEN: 48081 runtime reload -> PASS, 当前监听 PID 63536，运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260908-220415.jar`，Jar SHA-256 `33CAC89B7B54512465A5AFC93769C17682B1FD554A40B7A0CDA51D4F01B38E20`，Jar 修改时间早于 Java 进程启动时间，运行参数归属 `E:\IntRuoyi\IntRuoyiBackend`，敏感启动参数已在任务证据中脱敏处理。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, `status=UP`。
- GREEN: anonymous `POST /admin-api/mes/pro/batch-record-report/production-batch-record/total-recognition-json` -> PASS, 返回业务码 `401 账号未登录`，证明运行态已加载 Controller mapping 且不再是 `请求地址不存在`。
- GREEN: authenticated upload of `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc` -> PASS, HTTP 200 / `code=0`，`schemaVersion=2`，产品 `按压式球囊扩充压力泵` / `IDI-01`，`processCount=15`，首工序 `粗洗工序`，末工序 `大包装工序`，与 `批记录总对应.json` 的产品和工序名序列一致，且返回内容不含 `jimuSchemaJson`、`sheetLayoutJson`、`cellRules`、`recognizedSchemaJson`。
- GREEN: admin permission projection -> PASS, `GET /admin-api/system/auth/get-permission-info` 返回菜单 `表单解析`，路径 `form-center/parser`，组件 `form-center/parser/index`，并包含 `form:parser:production-batch-record` 按钮权限。
- GREEN: `mvn -pl yudao-module-signature -am "-Dtest=GxpAuditTrailServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, failures 0, errors 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, failures 0, errors 0。
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` from `IntRuoyiFronted` -> PASS, `form parser json download static contract passed`。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, full reactor `BUILD SUCCESS`, `yudao-server-exec.jar` repackaged successfully at 2026-09-08 22:18:48 +08:00。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260908-form-parser-48081-runtime-reload\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-form-parser-48081-runtime-reload\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-48081-runtime-reload --mode preview` -> PASS, keep core task records; delete only task artifact Jar and two temporary evidence files; no blocked paths or warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-48081-runtime-reload --mode apply` -> PASS, deleted only preview-approved task temporary files; kept `task.md`、`execution-log.md`、`verification-report.md`。
- Experience: 按 `project-experience-consolidation` 规则，将本机菜单权限投影核对经验合并到 `docs/login-access.md#本机菜单权限投影核对门禁`，明确当前应以 `/admin-api/system/auth/get-permission-info` 核对动态菜单和按钮权限。
- Authorization: 用户在 2026-09-08 回复“授权”，明确允许执行 Git 收尾提交/推送。
- Git implementation evidence: `31fc6ca21 chore: checkpoint current int_main work` 已包含表单解析生产批记录 JSON 下载相关代码和测试文件。
- Git baseline evidence: `5c0310dd0 chore: 保存主干现有工作区基线` 已保存并推送授权时主干已有脏工作区；该提交不是本任务实现提交。
- Git residual baseline evidence: `28d2d1e07 chore: baseline residual gxp audit sql change` 已保存并推送基线后新出现的 GxP SQL 残留变更；该提交不是本任务实现提交。
- Git residual baseline evidence: `a59d73313 chore: baseline residual system gxp audit changes` 已保存并推送残留 SQL 基线后新出现的 system GxP 文件变更；该提交不是本任务实现提交。
- Git residual baseline evidence: `61fc34bae chore: 保存主干测试 SQL 基线` 已保存并推送并行任务产生的 system 测试 SQL 变更；该提交不是本任务实现提交。
- Git residual baseline evidence: `f3c641315 chore: baseline residual system gxp audit tests` 已保存并推送并行任务产生的 system GxP 测试类；该提交不是本任务实现提交。
- Closeout records staging: `doc/tasks/20260908-form-parser-48081-runtime-reload/` 被 `.git/info/exclude` 的 `/doc/tasks/*/` 规则忽略，最终收尾记录使用 `git add -f` 提交。
