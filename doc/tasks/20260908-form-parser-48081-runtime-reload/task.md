# 表单解析生产批记录 48081 运行态修复

## Task Goal

修复本机 48081 后端仍提示 `请求地址不存在: admin-api/mes/pro/batch-record-report/production-batch-record/total-recognition-json` 的问题，确认运行态已加载新增 MES parse-only Controller，并用用户指定的生产批记录 `.doc` 文件验证接口返回 `product/schemaVersion/processes` 批记录总识别 JSON。

## Milestones

- [x] M1: 复现当前 48081 运行态缺失接口、离线或旧 Jar 问题
- [x] M2: 核对源码、测试和运行 Jar 关键 class / mapping
- [x] M2a: 修复 GxP 审计服务构造器注入导致的 48081 启动阻塞
- [x] M3: 重建并重启 int_main 48081 后端
- [x] M4: 使用登录态接口和指定 `.doc` 文件验证总识别 JSON
- [x] M5: 记录验证证据并完成收尾

## Expected Verification

- 48081 端口归属核对，确认旧进程属于 `E:\IntRuoyi` 的 int_main 后端后再停止
- `mvn -pl yudao-module-signature -am '-Dtest=GxpAuditTrailServiceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -pl yudao-server -am -DskipTests package`
- `/actuator/health` 返回 `UP`
- 登录态上传 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc` 到 `/admin-api/mes/pro/batch-record-report/production-batch-record/total-recognition-json`，业务响应 code=0，data 可解析且包含 `schemaVersion=2` 与非空 `processes`
- `git diff --check`

## Current Status

completed

48081 后端已恢复到新运行 Jar，目标接口已进入认证链路并通过管理员登录态真实 `.doc` 上传验证；返回内容为 `product/schemaVersion/processes` 批记录总识别 JSON，不含 Jimu schema 字段。定向后端回归、服务打包、前端静态合同、health 检查和 admin 菜单/按钮权限核对均已通过。用户已在 2026-09-08 明确授权 Git 收尾，当前分支提交并推送到 `origin/int_main` 后完成。

## 设计约束检查

- 不改数据库、不补 fallback、不返回 mock JSON。
- 只在确认 48081 旧进程属于本项目 int_main 后端后停止并重启。
- 目标接口必须由真实登录态上传用户指定 `.doc` 验证，不能只凭源码存在或匿名 401 判定成功。
- Git 提交/推送仅在用户 2026-09-08 明确回复“授权”后执行。

## Git Closeout Evidence

- Implementation commit: `31fc6ca21 chore: checkpoint current int_main work`，包含表单解析生产批记录 JSON 下载前后端代码、MES parse-only API、总识别 extractor、前端下载入口、静态合同与共享 Word 解析经验文档。
- Authorized baseline commit: `5c0310dd0 chore: 保存主干现有工作区基线`，按项目规则保存授权时已存在的主干脏工作区。
- Authorized residual baseline commit: `28d2d1e07 chore: baseline residual gxp audit sql change`，保存基线推送后新出现的 GxP SQL 残留变更并已推送。
- Authorized residual baseline commit: `a59d73313 chore: baseline residual system gxp audit changes`，保存残留 SQL 基线后新出现的 system GxP 文件变更并已推送。
- Authorized residual baseline commit: `61fc34bae chore: 保存主干测试 SQL 基线`，保存并行任务产生的 system 测试 SQL 变更并已推送。
- Authorized residual baseline commit: `f3c641315 chore: baseline residual system gxp audit tests`，保存并行任务产生的 system GxP 测试类并已推送。
- Closeout records: 本任务 `task.md`、`execution-log.md`、`verification-report.md` 因 `.git/info/exclude` 忽略 `doc/tasks/*/`，收尾提交时使用 `git add -f` 纳入审计记录。
