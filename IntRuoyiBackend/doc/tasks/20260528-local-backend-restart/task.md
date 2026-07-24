# 任务：重启本地后端加载 NAS 备份读取修复

## 任务目标

- 重启本地 IntRuoyi 后端，让运行中的服务加载提交 `8c07ee46f2` 的 NAS 备份目录读取修复。
- 启动后确认本地后端健康检查可访问。
- 不修改业务数据，不切换环境，不绕过缺失前置条件。

## BDD 场景

- BDD: 本地后端加载新代码 -> Given 当前本地 48081 未运行或运行旧后端 / When 执行本地后端重启脚本 / Then 48081 后端健康检查返回 UP，后端代码包含 NAS 备份读取修复。
- BDD: 本地重启前置条件缺失 fail fast -> Given Maven、Java、Docker MySQL 或 Redis 等本地依赖缺失 / When 执行重启脚本 / Then 脚本必须失败并报告具体前置条件，不得伪造启动成功。

## 里程碑

- [x] M1：确认本地 48081 当前无监听，测试服仍是旧镜像，用户当前需要本地后端重启。
- [x] M2：执行本地后端重启；官方脚本因全局 worktree 配对不一致 fail-fast，按 `int_main` 同端口同参数直接启动。
- [x] M3：修复并验证启动时暴露的 Spring 构造器装配回归，随后验证健康检查。
- [x] M4：更新任务记录，按策略提交本任务记录。

## 预期验证

- GREEN: `script\deploy\restart-int-ruoyi-local.ps1 -Component backend` 成功或明确前置条件失败。
- GREEN: `http://127.0.0.1:48081/actuator/health` 返回 `UP`。

## 当前状态

completed

## 当前发现

- 源码与 target classes 已包含 `RuntimeBackupNasRepository` 和 `yudao.runtime-control.backup-ops.nas-backup-points-root`。
- 本轮重启前本地 `48081` 未监听。
- 测试服当前后端镜像仍为旧镜像 `intruoyi-backend:20260528_company_v8_c716918154`，不包含提交 `8c07ee46f2`。
- `script\deploy\restart-int-ruoyi-local.ps1 -Component backend` 被全局 worktree 配对校验阻断：后端存在 `showroom-hall-description-export`，但前端没有同名 worktree。
- 首次直接启动失败原因是 `RuntimeBackupDrillServiceImpl` / `RuntimeOpsCandidateServiceImpl` 同时存在生产构造器和测试便利构造器，Spring 未明确选择生产构造器，导致 `No default constructor found`。
- 构造器回归修复后，第二次直接启动继续 fail-fast 于缺少 `dcc.signature.evidence` HMAC 配置；按既有本地验证任务记录补入显式本地配置 `CODEX-DCC-E2E-HMAC-SECRET-20260526` / `dcc-hmac-v1` 后启动成功。
- 当前本地后端 PID `49576` 监听 `48081`，`GET http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- 启动后日志存在非阻塞定时任务错误：本地库缺少 `dcc_nas_acl_restore_plan`，影响 DCC NAS ACL 恢复计划定时任务，不影响本次后端健康检查和运行控制 NAS 备份读取修复加载。

## 验证结果

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`RuntimeBackupDrillServiceImpl` 无明确 Spring 构造器，报 `No default constructor found`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsGuideServiceImplTest,RuntimeOpsResponsibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，51 tests。
- GREEN: direct local backend start on `48081` with explicit DCC signature evidence config -> PASS。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`{"status":"UP"}`。

## Cleanup Keep

- doc/tasks/20260528-local-backend-restart/bug-regression-evidence.md
