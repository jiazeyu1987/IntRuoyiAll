# Execution Log：eDHR Jimu 在线填写、多人电子签名与最终打印

BDD: 在线填写字段并签名保存 -> Given 操作员打开一张草稿 eDHR 表单 / When 修改字段并输入当前账号密码保存 / Then 系统保存字段值，记录字段审计链，并生成 `FIELD_CHANGE` 电子签名。

BDD: 同一张表单多人签名 -> Given 一张 eDHR 表单已有填写人字段签名 / When 另一名具备权限的用户提交或审批该表单 / Then 同一执行记录下保留多条不同签名人、签名动作和签名含义的电子签名。

BDD: 基准冲突失败关闭 -> Given 用户 A 和用户 B 同时打开同一草稿表单 / When 用户 A 先保存字段变更 / Then 用户 B 使用旧 hash/revision 保存时被拒绝，不能覆盖用户 A 的签名数据。

BDD: 最终表单可打印 -> Given eDHR 表单审批通过并关闭 / When 授权用户输入电子签名密码生成 PDF 归档 / Then 系统封存签名、生成 `SEALED` PDF 归档，并提供下载用于打印。

- SETUP: 创建 worktree -> PASS，后端 `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\ruoyi-vue-pro`，前端 `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3`，分支均为 `codex/edhr_jimu`。

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionControllerTest,ExecutionArchiveRendererTest" test` -> FAIL，预期失败；`MesProBatchRecordExecutionFormReviewSignReqVO` 与 `MesProBatchRecordExecutionFormReviewSignRespVO` 尚未实现，复核签名契约先由测试锁定。

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionControllerTest,ExecutionArchiveRendererTest" test` -> PASS，60 tests；已实现 `/cosign` 表单复核签名、`FORM_REVIEW` 签名动作、字段审计证据绑定和 PDF 归档签名含义输出。

- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionControllerTest,ExecutionArchiveRendererTest" test` -> PASS，88 tests；字段审计、执行、签名、归档服务与归档渲染回归通过。

- CLEANUP_PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-edhr-jimu-online-fill-sign-print --mode preview` -> BLOCKED，delete `<none>`；脚本未找到推断 main 分支 `master-jdk17` 的 checked-out worktree，未执行 apply。
- FINAL: 后端实现已完成并验证通过，保留任务文档与正式回归测试。

- E2E_SETUP: `mvn -pl yudao-server -am -DskipTests package` -> PASS；后端 worktree jar 构建成功，并临时接管本机 `48081`；前端 worktree 临时接管本机 `8081`。
- GREEN: `pnpm e2e:edhr:tracking-signature` -> PASS；真实测试租户 `测试租户/aoteman` 完成 eDHR 追踪页、详情时间线、签名页和动作筛选验证。
- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-form-review-real-e2e.cjs` -> PASS；真实详情页执行“复核签名”，`/cosign` 返回 `FORM_REVIEW`，签名页展示“表单复核”。
- RED: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs` -> FAIL；旧 `SEALED` 归档下载返回 JSON 错误“归档文件存储侧 Retention/Object Lock/legal hold 证据校验失败，拒绝封存或下载”；改走真实页面“重新生成最终表单归档”后，后端 `/generate` 返回 `500: 归档文件保存失败`。最终表单下载/打印真实 E2E 未放行，当前阻塞为归档文件存储/Retention 前置不满足。

BDD: 最终表单归档保存必须显式声明存储保全策略 -> Given eDHR 表单审批通过并关闭 / When 授权用户生成最终 PDF 归档 / Then 后端保存归档文件时必须向文件服务传入 Object Lock、retention、legal hold 和归档 SHA-256 策略，缺少真实存储保全能力时失败关闭且不得生成占位成功归档。

- RED: `mvn -pl yudao-module-infra,yudao-module-mes -am "-Dtest=FileServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败；`FileServiceImpl` 还没有 `createFileWithStorageRetention(configId, ...)` 指定客户端保存方法，eDHR 归档仍无法强制走受保护存储客户端。

- GREEN: `mvn -pl yudao-module-infra,yudao-module-mes -am "-Dtest=FileServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；`FileServiceImplTest` 30 tests，`MesProBatchRecordExecutionArchiveServiceImplTest` 20 tests。文件服务已支持指定 `configId` 的受保全保存，eDHR 归档生成改为运行时注册 `EDHR_S3_*` 专用 S3/Object Lock 客户端并传入上传策略。

- VERIFY: `python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS；`edhr-protected-storage-20260601` 具备 versioning、Object Lock、COMPLIANCE retention、legal hold=ON、protected version delete denied 和同版本读取证据。

- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionControllerTest,ExecutionArchiveRendererTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，88 tests。

- REGRESSION: `python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS，7 tests。

- E2E_SETUP: 临时切换本机 `8081/48081` 到 `edhr_jimu` worktree 后端 jar 与前端 Vite，确认服务命令行均指向 `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu`。

- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs` -> PASS；真实测试租户 `测试租户/aoteman` 登录固定入口 `http://localhost:8081`，进入已关闭执行记录 `40`，重新生成最终表单归档返回 `SEALED`，archiveId=`25`，contentType=`application/pdf`，downloadedBytes=`14740`，downloadedSha256=`6146b2141dabc9677c043802410d3c36b25b812466e1e4bc2dee15b7c50b03ca`，与归档 SHA-256 一致。结论：最终表单归档下载/打印真实路径放行，失败根因不是本机未连接打印机，而是归档保存未走带 Object Lock/retention/legal hold 证据的 eDHR 专用受保护存储客户端。

- REGRESSION: `mvn -pl yudao-module-infra,yudao-module-mes -am "-Dtest=FileServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；`FileServiceImplTest` 30 tests，`MesProBatchRecordExecutionArchiveServiceImplTest` 20 tests。

- VERIFY: `python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS，7 tests。

- VERIFY: `python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS；受保护 bucket 具备 versioning、Object Lock、COMPLIANCE retention、legal hold=ON、受保护版本删除被拒绝且版本仍可读。

- CLEANUP_PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-edhr-jimu-online-fill-sign-print --mode preview` -> BLOCKED，delete `<none>`；脚本未找到推断 main 分支 `master-jdk17` 的 checked-out worktree，未执行 apply。
