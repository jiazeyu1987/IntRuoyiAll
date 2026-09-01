# 执行日志：注册证下载系统异常修复

## User Intent

- 用户要求在 worktree `TR3` 修复“下载注册证提示系统异常”。
- 工作区：`D:\IntRuoyiWorktree\TR3`；分支：`codex/20260901-int-main-worktree-03`。
- 初始未要求 E2E、Playwright、服务启动、数据库写入、提交、合并或推送，因此先完成代码级 BDD/TDD 与定向非 E2E 验证。
- 用户后续明确要求“进行E2E验证”。
- 用户后续明确要求融合进 `int_main`。

## BDD

BDD: 正式授权用户下载注册证 -> Given 用户拥有有效注册证下载权限且注册证业务文件和基础设施文件存在，When 用户点击下载注册证，Then 系统返回真实文件内容和服务端生成的文件名，不返回“系统异常”。

BDD: 下载失败保持明确业务边界 -> Given 下载授权、公司范围、文件元数据或审计前置条件不满足，When 用户下载注册证，Then 系统返回明确中文业务错误且不得返回伪文件、默认成功或绕过权限。

BDD: 批准日期为空的真实注册证下载 -> Given 已授权用户通过 TR3 真实前端登录，且存在批准日期为空、首次获证日期为 `2026-08-01` 的正式注册证附件，When 用户从注册证详情点击下载，Then 浏览器接收真实文件且服务端文件名使用 `20260801`，页面不提示“系统异常”。

## Command Intent

- 只读检查 TR3 分支、注册证下载前后端链路、现有测试、近期变更和匹配经验门禁。
- 计划运行 `yudao-module-dcc` 注册证下载定向测试以复现问题。

## Milestone Updates

- M1 completed：已确认 TR3 工作树初始为干净状态，注册证下载由前端业务文件下载接口进入后端授权、文件名构造、文件读取和审计链路；本机下载审计冻结到业务文件 `990819112` 的真实错误为 `approvalDate` 为空时直接调用 `format`。
- M2 completed：TR3 当前源码已有 `resolveFileNameDate` 正式修复，并已有精确回归测试覆盖批准日期为空时使用首次获证日期；未重复修改生产代码和测试代码。
- M3 completed：后端定向 14 tests PASS；前端下载静态合同 PASS；两个技能 evidence validator PASS；`git diff --check` PASS。
- M4 completed：已把可复用下载日期来源门禁并入 `docs/backend-development.md` 和 `docs/experience-index.md`；cleanup preview/apply 仅删除本任务两个临时 evidence 文件并保留 `task.md`、`execution-log.md`、`verification-report.md`。
- M5 blocked：TR3 后端已完成 `-pl yudao-server -am -DskipTests package`，并以当前 TR3 可执行包启动在 `48084`；`/actuator/health` 返回 `UP`。前端已启动在 `8084`，真实浏览器打开登录页正常，但登录表单的租户、账号和密码均为空。当前进程、用户和机器环境均未配置 `REG_CERT_E2E_USERNAME`、`REG_CERT_E2E_PASSWORD`、`REG_CERT_E2E_TENANT`，前端 `.env` 也没有默认账号或密码。按登录门禁未猜测或复用历史凭据，未发起登录和下载。
- M5 completed：用户明确提供 `admin` 凭据后，登录预检通过；真实页面下载首先再次复现“系统异常”，后端异常栈显示 `dcc_registration_certificate_access_audit.detail_json` 为无效 JSON。完成 M6 修复、重新构建并启动 TR3 后，同一路径下载真实文件成功。
- M6 completed：下载审计详情改用项目标准 JSON 序列化器，新增浏览器/异常上下文控制字符回归；没有绕过审计、授权、公司范围或文件读取。

## Verification Evidence

- RED: 本机只读审计查询 -> FAIL，业务文件 `990819112` 的 `DOWNLOAD:FAILURE` 原因为 `getApprovalDate()` 返回空值后被直接格式化。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS，14 tests，0 failures，0 errors，0 skipped，`BUILD SUCCESS`。
- REGRESSION: `node IntRuoyiFronted\tests\registration-certificate-attachment-preview-download-static.spec.mjs` -> PASS。
- CONTRACT: bug regression evidence validator -> PASS。
- CONTRACT: backend API evidence validator -> PASS。
- STRUCTURE: `git diff --check` -> PASS。
- CLOSEOUT: task-closeout cleanup preview/apply with `--worktree-closeout off` -> PASS；默认 worktree 集成预览因主工作区脏且无法快进而阻塞，按项目 Git 政策未提交、未合并、未删除 TR3。
- E2E RUNTIME: `mvn -pl yudao-server -am -DskipTests package`（由 TR3 启动脚本执行） -> PASS，`BUILD SUCCESS`；TR3 后端启动到 `48084`，`http://127.0.0.1:48084/actuator/health` -> `{"status":"UP"}`；TR3 前端启动到 `http://127.0.0.1:8084/`。
- E2E PRECHECK: Playwright 打开 `http://127.0.0.1:8084/login` -> BLOCKED，登录表单的租户、账号、密码均未预填；所需授权凭据在进程、用户、机器环境和本工作树前端 `.env` 中均不存在。为防止密码猜测，本轮未提交登录。
- E2E CLEANUP: 通过本任务启动的前后端会话执行正常中断；后端完成 Spring 优雅停止。`8084`、`48084` 复核为 `not-listening`。
- E2E LOGIN: `node scripts/preflight/login-preflight.mjs`，凭据由当前进程注入且不落盘 -> PASS，真实登录 `芋道源码/admin` 并进入注册证页面。
- E2E RED: `node doc/tasks/20260901-registration-certificate-download-system-error/registration-certificate-download.e2e.cjs` -> FAIL，真实页面下载接口 HTTP 200 但返回 `{"code":500,"msg":"系统异常"}`，页面显示“系统异常”；后端异常为 MySQL 拒绝 `dcc_registration_certificate_access_audit.detail_json` 的无效 JSON。
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateFileDeliveryServiceTest#registrationManagerDownloadSerializesControlCharactersInAuditUserAgent" test` -> FAIL，手工拼接的审计 JSON 含控制字符时标准解析失败。
- GREEN: 同一聚焦测试命令 -> PASS，1 test，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS，15 tests，0 failures，0 errors，0 skipped。
- RUNTIME BUILD: TR3 `start-branch-backend.ps1 -Slot 3 -Build` -> PASS，30 个 reactor 模块 `BUILD SUCCESS`，当前修复已进入 `yudao-server-exec.jar` 并在 `48084` 健康启动。
- E2E GREEN: 同一真实页面下载脚本 -> PASS，注册证 `990819202` 的正式附件 `990819112` 下载 HTTP 200，文件名 `20260801_5555555_33333333.png`，内容类型 `image/png`，76,733 字节；页面错误、控制台错误和失败响应均为 0。
- FINAL CLOSEOUT: task-closeout cleanup preview/apply with `--worktree-closeout off` -> PASS，仅删除 `registration-certificate-download.e2e.cjs` 和结果 JSON，保留正式任务记录与后端回归测试；TR3 前后端正常停止，`8084/48084` 均为 `not-listening`。按项目 Git 政策未提交、未合并、未删除 worktree。
- INTEGRATION PREP: TR3 先通过 `git merge --ff-only int_main` 从 `37ca712e3` 快进到 `70a30f8e3`，现有未提交改动保持；branch runtime port guard -> PASS。
- IMPLEMENTATION COMMIT: `7905873f8 fix(dcc): 修复注册证下载审计异常`，仅包含注册证下载服务、回归测试、`docs/backend-development.md` 和 `docs/experience-index.md`；TR3 的登录页相关并行改动未暂存、未提交。
- INT_MAIN MERGE: 主工作区先精确保全 `docs/backend-development.md`、`docs/experience-index.md` 的并行差异，再执行 `git merge --ff-only codex/20260901-int-main-worktree-03`，`int_main` 从 `70a30f8e3` 快进到 `ee06f18e1`。恢复并行文档时保留双方内容，恢复后的两文件差异规模与保全前一致（`5/2`、`15/6`），未提交并行内容。
- INT_MAIN REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS，15 tests，0 failures，0 errors，0 skipped；前端注册证附件下载静态合同 -> PASS；`7905873f8`、`ee06f18e1` 均为 `int_main` 祖先。
- INT_MAIN CLOSEOUT: task-closeout preview/apply with `--worktree-closeout off` -> PASS，删除项为空；未停止、重启或删除 TR3，未释放其槽位或端口登记，未推送远端。

## Blockers

- 无代码或 E2E 阻塞。
- E2E 下载按正式链路新增成功审计记录，未修改注册证业务数据；未提交、未合并、未推送。
