# 执行日志：重启本地后端加载 NAS 备份读取修复

- BDD: 本地后端加载新代码 -> Given 当前本地 48081 未运行或运行旧后端 / When 执行本地后端重启脚本 / Then 48081 后端健康检查返回 UP，后端代码包含 NAS 备份读取修复。
- BDD: 本地重启前置条件缺失 fail fast -> Given Maven、Java、Docker MySQL 或 Redis 等本地依赖缺失 / When 执行重启脚本 / Then 脚本必须失败并报告具体前置条件，不得伪造启动成功。
- BDD: Spring 装配回归必须阻断启动 -> Given 运行控制服务存在多构造器 / When Spring 创建运行控制 Bean / Then 必须明确选择生产构造器，不能在启动时因 `No default constructor found` 失败。

- GREEN: preflight -> Java、Maven、Docker 可用，`int-ruoyi-mysql` 与 `int-ruoyi-redis` 均为 running。
- RED: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，expected reason: mixed worktree port map fail-fast，`showroom-hall-description-export` 缺少配对前端 worktree。
- BLOCKER: first direct local backend start -> FAIL，`RuntimeOpsCandidateServiceImpl` / `RuntimeBackupDrillServiceImpl` 多构造器无明确 Spring 注入构造器，启动日志出现 `No default constructor found`。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: Spring 装配 `RuntimeBackupDrillServiceImpl` 时报 `No default constructor found`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsGuideServiceImplTest,RuntimeOpsResponsibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，51 tests。
- BLOCKER: second direct local backend start -> FAIL fast，`DCC electronic signature evidence configuration is missing`。
- GREEN: direct local backend start with explicit local DCC signature evidence config `CODEX-DCC-E2E-HMAC-SECRET-20260526` / `dcc-hmac-v1` -> PASS，Tomcat started on port `48081`，`Started YudaoServerApplication`。
- GREEN: `Invoke-RestMethod -Uri http://127.0.0.1:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`，监听进程 PID `49576`。
- NOTE: 启动后本地日志存在定时任务错误 `Table 'ruoyi-vue-pro.dcc_nas_acl_restore_plan' doesn't exist`；该问题不阻断本次后端健康检查，但会影响 DCC NAS ACL 恢复计划定时任务。
