# Execution Log: DCC 产品名称识别全链路稳定性审计与修复

BDD: 新版本后识别链路不应产生非预期系统异常 -> Given 测试服运行已发布的新版本 / When DCC 产品名称识别或批量识别产生记录 / Then 新后端启动时间之后不得新增数据库截断、未捕获异常、系统错误栈或非预期失败类型；业务预期的不匹配必须以受控状态和可读失败原因记录。

GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md`、`docs/powershell-memory.md` 和 `docs/server-access.md`；本阶段仅执行测试服只读审计，不进行服务器写入、重启或发布。

GREEN: 测试服发布后自然运行态审计 -> PASS，测试服运行 `release-20260708-dcc-status-guard-v3-e1bd69ce96`，后端启动时间 `2026-07-08T10:08:30.462225196Z`，后端 health `{"status":"UP"}`；正确目标库为 `intruoyi-mysql/ruoyi-vue-pro`，以后端启动时间 `2026-07-08 10:08:30` UTC 为基准，`dcc_controlled_file_recognition_record` 新增记录数 0、`data_too_long=0`、`suspicious_failure=0`。

EVIDENCE: 历史记录边界 -> 当前最新识别记录仍是旧版本产生的 `Data truncation: Data too long for column 'status'`，最新时间为 `2026-07-08 08:47:11`，早于新后端启动时间；说明旧问题没有在新版本后自然新增。

EVIDENCE: 真实单文件识别触发 tenant=1 -> `aoteman` 可登录但只有 `approval_center_entry` 角色，对 tenant 1 文件 `2054545668044057075` 调用 `/admin-api/dcc/controlled-files/{id}/recognize-project-code` 返回业务 `403 没有该操作权限`，未执行识别，未新增识别记录或截断错误；该路径不作为识别业务成功证据。

EVIDENCE: 测试租户权限与样本 -> `aoteman` 在 tenant 122 具备 `doc_control`、`tenant_admin` 角色和 DCC 菜单/接口权限；测试租户存在真实受控文件样本 `2054545668044050589`，调用前未绑定 DCC 基础数据且无识别记录。

EVIDENCE: 真实单文件识别触发 tenant=122 -> 使用 `tenant-id=122`、账号 `aoteman` 登录成功，用户 ID `113`；调用 `/admin-api/dcc/controlled-files/2054545668044050589/recognize-project-code` 后业务返回 `1080000132 DCC project-code recognition failed: Codex CLI timed out after 300 seconds`。该 Codex 超时属于用户明确排除的外部问题，但识别记录已受控写入 `FAILED`，`post_probe_records=1`、`data_too_long=0`、`suspicious_failure=0`，证明 DCC 识别记录表不再发生 `status` 截断。

ROOT-CAUSE: 新增非 Codex 问题 -> 真实触发 tenant 122 单文件识别后，后端异步访问日志写入失败：`Data truncation: Data too long for column 'operate_name' at row 1`。根因是接口 `@Operation(summary = "Recognize controlled file DCC basic data with Codex CLI")` 长度 55，而 `infra_api_access_log.operate_name` 当前为 `varchar(50)`；这是合法接口名落库容量不足，不是识别记录状态错误。

RED: `mvn.cmd -pl yudao-module-infra "-Dtest=ApiAccessLogServiceImplTest#testCreateApiAccessLog_longOperateName" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，H2 报 `Value too long for column "operate_name CHARACTER VARYING(50)"`，复现 `Recognize controlled file DCC basic data with Codex CLI` 无法写入访问日志。

GREEN: `mvn.cmd -pl yudao-module-infra "-Dtest=ApiAccessLogServiceImplTest#testCreateApiAccessLog_longOperateName" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，测试 schema 已将 `infra_api_access_log.operate_name` 扩容到 `varchar(128)`，合法长操作名可完整保存。

GREEN: `python -X utf8 -m pytest script/tests/test_infra_api_access_log_operate_name_length_sql.py -q` -> PASS，4 tests，覆盖 release 元数据、`ALTER TABLE infra_api_access_log MODIFY COLUMN operate_name varchar(128)`、测试 schema 一致性和非破坏性约束。

GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260708_infra_api_access_log_operate_name_length.sql` -> PASS，migrationId `20260708_infra_api_access_log_operate_name_length`，type=schema，allowedEnvironments=`test,backup,prod`，riskLevel=low。

GREEN: release-preflight -> PASS，已读取 `docs/release-backup-restore.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md` 和 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`；下一步只发布测试服，目标固定 `172.30.30.58`、`/opt/intruoyi/runtime`，不操作正式服或备份服。


GREEN: release-worktree-targeted-verification -> PASS，干净发布 worktree 后端 `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260708oplog\b` 指向 `d25b8d1404922471a21f63abea4d7c63cbd3f4ca`、前端 `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260708oplog\f` 指向 `b019c504e77f0f6cf090e22916c246455baafdbf`，二者 `git status --short` 为空；在后端 worktree 上复跑 targeted Maven、SQL pytest、migration policy gate 均 PASS。

GREEN: build-release -> PASS，运行控制台 operation `op-2026-07-08T104450030578300Z-1b6cd10b-aaa6-4992-be23-b7bf173a14ba` 构建 `release-20260708-dcc-operate-log-v1-d25b8d1404` 成功；manifest 确认 `publishScope=code-only`、backend commit `d25b8d1404922471a21f63abea4d7c63cbd3f4ca`、frontend commit `b019c504e77f0f6cf090e22916c246455baafdbf`、两个 sourceRepos `dirty=false`，并包含 `required-sql/20260708_infra_api_access_log_operate_name_length.sql`。

EVIDENCE: publish-test transient blockers -> 第一次测试服发布 operation `op-2026-07-08T105753955788500Z-638f4553-e66e-48f0-af58-03c6e92db675` 因 SSH 到 `172.30.30.58:22` 超时失败，发布锁释放为 `FAILED`；只读复查确认 `.env` 已写入新 tag 但容器仍为旧 tag、`operate_name` 仍为 `varchar(50)`、目标 migration count 为 0。SSH 恢复后第二次发布 operation `op-2026-07-08T111304660883500Z-58a7034f-7cbf-449c-9165-73caa09c0d39` 因本地 release cache 半拷贝目录只剩 `docker-compose.yml`，从 NAS `Copy-Item` 大 tar 失败；只清理本次 releaseTag 本地缓存目录后，NAS 包 `intruoyi-images_release-20260708-dcc-operate-log-v1-d25b8d1404.tar` 仍可读且长度 `814707200`。

GREEN: publish-test -> PASS，清理本次 releaseTag 本地半成品缓存后重跑测试服部署，operation `op-2026-07-08T111702295391Z-7ef8099a-5180-4523-b3b9-f03aa00fc7c6` 最终 `SUCCESS`；测试服 `/opt/intruoyi/runtime/.env` 为 `IMAGE_TAG=release-20260708-dcc-operate-log-v1-d25b8d1404`，backend/frontend 镜像 tag 均为该 releaseTag，backend health `{"status":"UP"}`，frontend HTTP `200`，后端启动时间 `2026-07-08T11:30:45.013587689Z`。

GREEN: migration-runtime-verification -> PASS，测试服真实库 `ruoyi-vue-pro` 中 `infra_api_access_log.operate_name` 为 `varchar(128)`，`infra_release_migration` 记录 `20260708_infra_api_access_log_operate_name_length` 状态 `APPLIED`、releaseTag `release-20260708-dcc-operate-log-v1-d25b8d1404`，测试服发布锁状态 `APPLIED`。

GREEN: real-recognition-probe-after-fix -> PASS，使用测试租户 tenant `122`、账号 `aoteman`，真实调用 `/admin-api/dcc/controlled-files/2054545668044050589/recognize-project-code`，耗时约 `301s`，业务返回 `1080000132 DCC project-code recognition failed: Codex CLI timed out after 300 seconds`；该 Codex CLI 超时为用户明确排除项。探针开始时间 `2026-07-08 19:33:36.928958` 后，`infra_api_access_log` 已保存完整 `operate_name=Recognize controlled file DCC basic data with Codex CLI` 的访问日志 `id=130449`，`result_code=1080000132`；`status_trunc_after_probe=0`、`suspicious_recognition_after_probe=0`、`operate_name_trunc_after_probe=0`。

EVIDENCE: non-DCC-noise -> 新版本日志中仍存在 `ShowroomReleaseAutoPublishScheduler` 的 `SHOWROOM_RELEASE_SOURCE_MISSING: failed to read file 2272`，该错误属于 showroom 自动发布任务，不在 DCC 产品名称识别链路；DCC 识别请求日志仅出现受控业务异常处理和访问日志完成，无 `Data too long for column 'status'` 或 `Data too long for column 'operate_name'`。

GREEN: maintenance-config-restore -> PASS，发布后已将 `D:\ProjectPackage\Int\IntRuoyiMaintance\config\runtime-control.local.yaml` 恢复为主路径 `D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro` 与 `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3`，重启维护控制台后 health `UP`；build-release preview 确认 `BackendRepoRoot`/`FrontendRepoRoot` 为主路径，且不再包含 `D:/ProjectPackage/Int/IntRuoyiWorktrees/r260708oplog/*`。

GREEN: task-closeout-cleanup preview -> PASS，workspace `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`，task id `20260708-dcc-recognition-full-stability-audit`，keep `task.md` 和 `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
