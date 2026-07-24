# Execution Log

CHANGE: `docs/changes/20260608-runtime-console-remote-root-cleanup.md` -> ACCEPTED，根分区查询/清理范围扩展到 `172.30.30.58`、`172.30.30.57`、`172.30.30.59`，正式服/备用服务器清理必须 `PROD` 二次确认，本轮不对正式服做实机访问，不对正式服/备用服务器做实机清理验证。

BDD: 显示远程根分区剩余空间 -> Given 运维人员选择 `test`、`prod` 或 `backup` / When 点击刷新 / Then 控制台通过服务端只读探测显示对应固定 IP 根分区总量、剩余量、使用率和 inode 使用率。

BDD: 清理远程临时目录 -> Given 运维人员点击清理按钮并确认 / When 服务端执行清理 / Then 仅删除 `/opt/intruoyi/ops/backup/tmp` 下内容和 `/tmp` 下允许的历史临时文件，返回清理前后根分区容量证据。

BDD: 高危服务器清理需要 PROD 确认 -> Given 请求目标是 `prod` 或 `backup` / When 未提交 `prodConfirmText=PROD` / Then 服务端 fail fast，不执行远程清理。

BDD: 禁止未知或错配目标清理 -> Given 请求目标和固定 IP 白名单不匹配 / When 提交查询或清理 / Then 服务端 fail fast。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeTestRootDiskServiceImplTest test` -> FAIL，远程根分区 VO/Service 尚不存在，测试无法编译。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRemoteRootDiskServiceImplTest test` -> PASS，10 tests，覆盖 `test/prod/backup` 固定 IP 查询、`prod/backup` 清理 `PROD` 门禁、错配 IP fail fast、返回目标证明和允许目录校验。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\manage-int-ruoyi-remote-root-disk.ps1 -Mode status -TargetEnvironment prod -ServerHost 172.30.30.58` -> FAIL as expected，本机目标证明失败：`expected prod server 172.30.30.57, got ServerHost=172.30.30.58`，未进入 SSH。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\manage-int-ruoyi-remote-root-disk.ps1 -Mode cleanup -TargetEnvironment prod -ServerHost 172.30.30.57 -Reason verify-guard` -> FAIL as expected，本机门禁失败：`ProdConfirmText=PROD` 缺失，未进入 SSH。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\manage-int-ruoyi-remote-root-disk.ps1 -Mode cleanup -TargetEnvironment backup -ServerHost 172.30.30.59 -Reason verify-guard` -> FAIL as expected，本机门禁失败：`ProdConfirmText=PROD` 缺失，未进入 SSH。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\manage-int-ruoyi-remote-root-disk.ps1 -Mode status -TargetEnvironment test -ServerHost 172.30.30.58` -> PASS，只读查询返回 `targetEnvironment=test`、`serverHost=172.30.30.58`、`mountPoint=/`、`availableBytes=20480`、`usagePercent=100`、`backupTempBytes=35393601536`、`tmpBytes=2534170624`。本命令未执行清理。

GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_remote_root_disk_script.py -q` -> PASS，4 tests，覆盖脚本三 IP 白名单、临时目录清理范围、`prod/backup` 清理 `PROD` 门禁和运行时 IP 证明。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRemoteRootDiskServiceImplTest,RuntimeStorageGuardServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，39 tests。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260608-runtime-console-test-root-cleanup --mode preview --worktree-closeout off` -> PASS，`delete=<none>`、`blocked=<none>`。

NOTE: 本轮没有执行正式服 `172.30.30.57` 实机查询、清理或健康检查；没有执行备用服务器 `172.30.30.59` 实机清理。
