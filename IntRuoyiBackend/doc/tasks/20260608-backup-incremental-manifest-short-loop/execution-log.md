# Execution Log

BDD: 对象级增量备份复用未变化对象 -> Given B1 已生成对象 manifest 和 object-store / When DCC 文件未变化并执行 B2 / Then B2 生成新 manifest，未变化对象计入 reused，复制计划不包含未变化对象。

BDD: 新增对象进入新备份点 -> Given 测试租户通过真实 DCC 前端路径新增文件 B / When 执行 B3 / Then B3 manifest 包含文件 B，object-store 只新增 B 对应对象，文件 A 不重复复制。

BDD: 修改对象生成可恢复新版本 -> Given 文件 B 已存在 / When 通过真实 DCC 前端路径修改 B 并执行 B4 / Then B4 manifest 指向 B 的新 repositoryKey，恢复到 B4 时内容为修改后内容。

BDD: 删除对象在新恢复点消失但历史可恢复 -> Given B3/B4 历史恢复点仍在保留范围内 / When 通过真实 DCC 前端路径删除 B 并执行 B5 / Then B5 manifest 体现删除，恢复 B5 不出现 B，恢复 B3/B4 仍可得到对应历史版本。

BDD: 恢复前校验环境和完整性 -> Given 运维选择测试服备份点 / When 执行恢复 / Then 校验 imageTag、MySQL 链、对象 manifest 引用、checksums 和测试服健康检查，且目标证明为 `172.30.30.58`。

BDD: 保留策略只清理合法备份数据 -> Given BackupPackage 中存在过期恢复点和未被保留 manifest 引用的对象 / When 执行保留策略 / Then 只清理合法备份点和孤立对象，不触碰 ReleasePackage、NAS 根目录、挂载或业务目录。

BDD: 备份执行阶段不得临时整桶镜像 -> Given 最新历史备份点已经记录未变化对象的 sourceEtag、size 和 repositoryPath / When 新备份扫描到相同对象 / Then 本次备份只复用 object-store 引用，不执行整桶 `mc mirror`，也不对未变化对象执行 `mc cp`。

## 现状检查

- 已检查旧任务 `20260606-runtime-backup-object-key-archive`：它解决的是对象 key 特殊字符导致 tar 归档失败的问题，仍属于全量对象 tar 思路，不满足本任务对象级增量 manifest 要求。
- 已检查旧任务 `20260606-runtime-console-build-test-backup-release`：它证明了旧 UI 备份和恢复链路可执行，但不是 B1-B5 连续增量验收。
- 已检查当前后端工作区：`script/backup-ops` 已存在未提交的增量 manifest、BackupPackage 根目录、恢复 stage 和保留策略相关半成品，需要用 RED/GREEN 收敛。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "incremental_object_copy_plan_excludes_reused_and_deleted_objects"` -> FAIL，预期原因：`New-BackupOpsObjectCopyPlan` 仍把 `changeType=reused` 的 `repo-d1` 放入复制计划，不满足“未变化对象不得重复复制”。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "incremental_object_copy_plan_excludes_reused_and_deleted_objects"` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q` -> PASS，60 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，25 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，91 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，79 tests。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k "linux_backup_now_reuses_unchanged_object_without_bucket_mirror" -q` -> FAIL，预期原因：Linux `backup_now` 仍先执行 `mc mirror --overwrite` 将整个 bucket 镜像到临时 staging，再发布到 object-store；这会在 DCC 文件很多时产生不必要的全量读取和临时落盘。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k "linux_backup_now_reuses_unchanged_object_without_bucket_mirror or linux_backup_now_generates_dcc_restore_candidate_contract" -q` -> PASS，2 passed；Linux `backup_now` 改为先扫描 MinIO metadata，对比上一恢复点 inventory，仅对新增/变化对象执行 `mc cp`，未变化对象直接复用历史 repositoryPath。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，35 passed。

BLOCKED: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py -q` -> FAIL，122 passed / 3 failed。失败用例为 `test_restore_candidate_scan_accepts_remote_nas_manifest_inventory`、`test_restore_candidate_scan_limits_remote_probe_to_selected_backup_id`、`test_rehearsal_candidate_accepts_inventory_only_object_backup`；失败原因集中在 PowerShell 工具侧 DCC 备份链 fixture 被当前链完整性校验判定为 `chain_status_incomplete`，与本次 Linux `backup_now` 对象级复制改动路径不同，但按提交基线不能忽略。

BLOCKED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> BLOCKED，原因：当前 linked worktree 找不到主分支 `master-jdk17` 的 checked-out worktree。未执行清理。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q -k "restore_candidate_scan_accepts_remote_nas_manifest_inventory or restore_candidate_scan_limits_remote_probe_to_selected_backup_id or rehearsal_candidate_accepts_inventory_only_object_backup"` -> PASS，3 passed；修正测试 fixture，使 `_valid_dcc_backup_manifest()` 显式声明 `chainStatus=COMPLETE`、`backupMode=full` 和 baseline 指针，保持生产链校验严格。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，125 passed。

CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview --worktree-closeout off` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`；本轮只做任务产物清理预览，不执行 worktree 合并/删除。

BLOCKED: 测试服 `172.30.30.58` 使用测试租户 DCC 真实路径完成 B1-B5 连续备份与 B3/B4/B5 恢复闭环证据仍缺失，本任务不能声明完整验收完成。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "manifest_declares_complete_recovery_set or manifest_blocks_success_without_test_target_proof or manifest_accepts_incremental_object_inventory_marker or sync_backup_to_test_server_targets_nas_backup_root"` -> FAIL，预期原因：manifest 未写入 `targetEnvironment` / `targetHost`，`production/172.30.30.57` 仍可生成 success manifest，且数据同步阶段会提前上传 `manifest.json`。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "manifest_declares_complete_recovery_set or manifest_blocks_success_without_test_target_proof or manifest_accepts_incremental_object_inventory_marker or sync_backup_to_test_server_targets_nas_backup_root"` -> PASS，4 passed。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "restore_candidate_scan_accepts_remote_nas_manifest_inventory or restore_candidate_scan_skips_manifest_without_test_target_proof or restore_candidate_scan_limits_remote_probe_to_selected_backup_id"` -> FAIL，预期原因：恢复候选扫描仍接受缺少 `targetEnvironment=test` / `targetHost=172.30.30.58` 证明的旧 manifest。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "restore_candidate_scan_accepts_remote_nas_manifest_inventory or restore_candidate_scan_skips_manifest_without_test_target_proof or restore_candidate_scan_limits_remote_probe_to_selected_backup_id"` -> PASS，3 passed。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q -k "linux_backup_manifest_declares_complete_recovery_set or linux_backup_manifest_blocks_success_without_test_target_proof"` -> FAIL，预期原因：Linux manifest 未写入目标环境证明，且 prod/172.30.30.57 未被 `write_manifest` 阻断。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q -k "linux_backup_manifest_declares_complete_recovery_set or linux_backup_manifest_blocks_success_without_test_target_proof"` -> PASS，2 passed。

BLOCKED-EVIDENCE: `backup-now -TargetEnvironment test` 旧进程 `backupId=20260608-112415` 使用修复前模块启动；本地 manifest 为 `syncedToTestServer=false` 且缺少 `targetEnvironment/targetHost`，不得作为 B1 验收点。该进程只连接测试服 `172.30.30.58`，等待其自然结束后重新执行 B1。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，88 passed。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest" test` -> FAIL，预期原因：Java 恢复候选仍把缺少 `targetEnvironment=test` / `targetHost=172.30.30.58` 的 manifest 判为 AVAILABLE。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest" test` -> PASS，13 tests。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test` -> FAIL，预期原因：运行控制台备份点列表仍把缺少目标证明的 manifest 判为 RECOVERABLE。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test` -> PASS，6 tests。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，79 tests。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，72 tests。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test` -> FAIL，预期原因：`RuntimeControlBackupPointRespVO` 还没有暴露 `imageTag`、`backupMode`、保留策略和对象增量统计字段。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test` -> PASS，5 tests。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，85 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，77 tests。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> FAIL，预期原因：运行控制台恢复候选仍把 `objects/manifest-object-inventory.json` 当目录校验，且旧 `objects/objects-yudao.tar` 仍被视为可恢复对象快照。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> PASS，12 tests。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q -k "linux_backup_manifest_declares_complete_recovery_set"` -> FAIL，预期原因：Linux 本地 manifest 仍把对象快照写为 `objects/yudao/`，没有使用 `objects/manifest-object-inventory.json`。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_manifest_carries_incremental_object_inventory_and_stats"` -> FAIL，预期原因：`ReportOps.psm1` 默认对象快照仍是旧 `objects/objects-yudao.tar`。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q -k "linux_backup_manifest_declares_complete_recovery_set"` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_manifest_carries_incremental_object_inventory_and_stats"` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q` -> PASS，60 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，25 passed。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "mysql_dump_command_spec_writes_directly_to_remote_backup_package or backup_mysql_export_records_remote_nas_dump_path_for_test_target or backup_ops_manifest_accepts_remote_mysql_dump_proof"` -> FAIL，预期原因：MySQL dump 仍要求本机 `OutputPath`，`Export-BackupOpsMySqlDump` 未使用测试服 BackupPackage 远端路径，manifest 无本地 dump 时把恢复集判为 `BLOCKED`。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "mysql_dump_command_spec_writes_directly_to_remote_backup_package or backup_mysql_export_records_remote_nas_dump_path_for_test_target or backup_ops_manifest_accepts_remote_mysql_dump_proof"` -> PASS，3 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，91 passed。

B1-EVIDENCE: `backupId=20260608-121507`，manifest `/mnt/nas/Backup/BackupPackage/20260608-121507/manifest/manifest.json`，目标证明 `targetEnvironment=test`、`targetHost=172.30.30.58`，`imageTag=20260607_232258`，`recoverySet.status=COMPLETE`，MySQL dump `mysql/ruoyi-vue-pro.sql.gz` 大小 `4085435867` bytes，object inventory `/mnt/nas/Backup/BackupPackage/20260608-121507/objects/manifest-object-inventory.json`，对象统计 `added=0 modified=0 deleted=0 reused=2`，checksums sha256 `f2ab862f5a68254b9b37bbc6b5e47658d3870a0d60b984f847cee10592977429`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with `DCC_BACKUP_E2E_BASE_URL=http://172.30.30.58:8081` and present file A `2054545668044049602` -> PASS，测试租户 `aoteman` 真实前端登录后 A 可访问，`canPreview=true`，`previewFileName=empty.docx`。

BDD: 测试服同步阶段 fail fast -> Given 备份点已经在本机生成并准备同步到测试服 `/mnt/nas/Backup/BackupPackage` / When 远程目录创建或小文件上传卡住 / Then SSH/SCP 请求必须带明确超时并失败退出，不得无限等待或静默跳过。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "sync_backup_to_test_server_targets_nas_backup_root"` -> FAIL，预期原因：`Sync-BackupOpsBackupToTestServer` 的远程 `mkdir` 请求没有传 `TimeoutSeconds=60`，导致 B2 在测试服目录创建阶段可无限等待。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "sync_manifest_to_test_server_upload_is_bounded"` -> FAIL，预期原因：最终 `manifest.json` 上传没有传 `TimeoutSeconds=300`，恢复点收尾阶段可能无限等待。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "sync_backup_to_test_server_targets_nas_backup_root or sync_manifest_to_test_server_upload_is_bounded"` -> PASS，2 passed，目录创建、元数据上传和最终 manifest 上传均带明确超时。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，92 passed。

EVIDENCE: 修复前启动的 B2 旧进程 `backupId=20260608-124924` 命令行为 `-TargetEnvironment test`，子进程只连接 `root@172.30.30.58` 并卡在 `mkdir -p '/mnt/nas/Backup/BackupPackage/20260608-124924'`；已停止该本地旧进程，不作为 B2 验收点。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded"` -> FAIL，预期原因：对象增量阶段 `mc ls` 元数据读取、历史备份点 `find/cat`、目录创建、复制脚本上传和对象复制命令没有显式超时，B2 可卡在读取 B1 manifest。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded"` -> PASS，1 passed，对象备份短 SSH 步骤均带明确超时。

EVIDENCE: 修复前重新启动的 B2 旧进程 `backupId=20260608-131221` 命令行为 `-TargetEnvironment test`，子进程只连接 `root@172.30.30.58` 并卡在读取 `/mnt/nas/Backup/BackupPackage/20260608-121507/manifest/manifest.json`；已停止该本地旧进程，不作为 B2 验收点。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_manifest_restore_ssh_steps_are_bounded"` -> FAIL，预期原因：manifest 恢复阶段读取 `manifest-object-inventory.json`、创建 restore stage、上传恢复计划和执行对象恢复命令均缺少显式超时。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded or remote_manifest_restore_ssh_steps_are_bounded"` -> PASS，2 passed，对象备份和 manifest 恢复 SSH/SCP 步骤均带明确超时。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，94 passed。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "native_process_drains_large_stdout"` -> FAIL，预期原因：`Invoke-BackupNativeProcess` 在读取 stdout/stderr 前等待子进程退出，大 stdout 会填满 pipe 并被 timeout 杀掉，exitCode=124。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "native_process_drains_large_stdout"` -> PASS，1 passed，底层进程先异步 drain stdout/stderr 再等待退出。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，95 passed。

EVIDENCE: 使用真实测试服 `172.30.30.58` 通过 `Invoke-BackupSshCommand` 带 `TimeoutSeconds=60` 读取 B1 manifest `/mnt/nas/Backup/BackupPackage/20260608-121507/manifest/manifest.json` 成功，`backupId=20260608-121507`、`targetEnvironment=test`、`targetHost=172.30.30.58`、输出长度 `7504`。

EVIDENCE: 修复前第三次重新启动的 B2 旧进程 `backupId=20260608-134722` 命令行为 `-TargetEnvironment test`，子进程只连接 `root@172.30.30.58` 并卡在读取 `/opt/intruoyi/runtime/.env`；已停止该本地旧进程，不作为 B2 验收点。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "mysql_dump_and_restore_use_explicit_long_ssh_timeouts or backup_ssh_short_reads_are_bounded_by_native_process_timeout"` -> FAIL，预期原因：SSH/SCP 默认仍为无界等待，MySQL dump/import 未显式声明 7200 秒长超时。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "mysql_dump_and_restore_use_explicit_long_ssh_timeouts or backup_ssh_short_reads_are_bounded_by_native_process_timeout"` -> PASS，2 passed，SSH/SCP 默认 300 秒，MySQL dump/import 显式 7200 秒。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

B2-EVIDENCE: `backupId=20260608-142850`，manifest `/mnt/nas/Backup/BackupPackage/20260608-142850/manifest/manifest.json`，目标证明 `targetEnvironment=test`、`targetHost=172.30.30.58`，`imageTag=20260607_232258`，`recoverySet.status=COMPLETE`，MySQL dump `mysql/ruoyi-vue-pro.sql.gz` 大小 `4085428076` bytes，对象 inventory `/mnt/nas/Backup/BackupPackage/20260608-142850/objects/manifest-object-inventory.json`，对象统计 `added=0 modified=0 deleted=0 reused=2`，inventory 对象数 `2`，复制候选 `0`，object-store 当前 `32610` objects / `137420847460` bytes，checksums sha256 `f2ab862f5a68254b9b37bbc6b5e47658d3870a0d60b984f847cee10592977429`，测试服健康检查 `172.30.30.58:48081/actuator/health -> {"status":"UP"}`。

DCC-B-EVIDENCE: `node tests\e2e\dcc-upload-test-file.e2e.js` 使用 `DCC_BACKUP_E2E_BASE_URL=http://172.30.30.58:8081`、测试租户 `aoteman`、真实前端 DCC 上传路径新增文件 B -> PASS，`controlledFileId=2054545668044049603`，文件名 `codex-incremental-backup-B-202606081450.docx`，版本 `V1.0`，源文件 `comments.docx`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B `2054545668044049603` -> PASS，A `previewFileName=empty.docx`，B `previewFileName=comments.docx`，目标前端 `http://172.30.30.58:8081`，测试租户 `aoteman`。

B3-EVIDENCE: `backupId=20260608-144502`，manifest `/mnt/nas/Backup/BackupPackage/20260608-144502/manifest/manifest.json`，目标证明 `targetEnvironment=test`、`targetHost=172.30.30.58`，`imageTag=20260607_232258`，`recoverySet.status=COMPLETE`，MySQL dump 大小 `4085443348` bytes，对象统计 `added=1 modified=0 deleted=0 reused=2`，inventory 对象数 `3`，新增对象 `dcc/original/20260608/comments.docx` / repositoryKey `1649491b261d7157babbd8b5ec283b29` / size `15526`，复制候选 `1`，A 未进入复制候选，object-store 当前 `32610` objects / `137420847460` bytes（content-addressed 仓库已有相同 blob，复制脚本未重复写入），checksums sha256 `f2ab862f5a68254b9b37bbc6b5e47658d3870a0d60b984f847cee10592977429`，测试服健康检查 `172.30.30.58:48081/actuator/health -> {"status":"UP"}`。

DCC-B-MODIFY-EVIDENCE: `node tests\e2e\dcc-upload-test-file.e2e.js` 使用测试服前端和测试租户真实 DCC 上传路径修改文件 B -> PASS，`controlledFileId=2054545668044049604`，文件名 `codex-incremental-backup-B-202606081450.docx`，版本 `V1.1`，源文件 `tables.docx`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B V1.1 `2054545668044049604` -> PASS，A `previewFileName=empty.docx`，B V1.1 `previewFileName=tables.docx`。

B4A-EVIDENCE: `backupId=20260608-145954` 备份成功，但对象 manifest 显示 `tables.docx` 是新增 path（`added=1 modified=0 reused=3`），不是修改 B3 的 `comments.docx` path；该点记录为 B4 前置纠偏证据，不作为“修改对象 path”验收点。

DCC-B-MODIFY-SAME-PATH-EVIDENCE: 使用 `tables.docx` 内容生成同名测试源 `D:\IntRuoyi-BackupOps\tmp\dcc-b4-modified-source\comments.docx`，再通过真实前端 DCC 上传路径提交 B `V1.2` -> PASS，`controlledFileId=2054545668044049605`，源文件名 `comments.docx`，大小 `13087`，用于验证同 object path 修改。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and B V1.2 `2054545668044049605` -> PASS，B V1.2 `previewFileName=comments.docx`。

B4-EVIDENCE: `backupId=20260608-151355`，manifest `/mnt/nas/Backup/BackupPackage/20260608-151355/manifest/manifest.json`，目标证明 `targetEnvironment=test`、`targetHost=172.30.30.58`，`imageTag=20260607_232258`，`recoverySet.status=COMPLETE`，MySQL dump 大小 `4085440258` bytes，对象统计 `added=0 modified=1 deleted=0 reused=3`，inventory 对象数 `4`，修改对象 `dcc/original/20260608/comments.docx` / repositoryKey `bf94ebce128c83db4f779493d7a5a866` / size `13087`，复制候选 `1`，A 未进入复制候选，object-store 当前 `32610` objects / `137420847460` bytes，checksums sha256 `f2ab862f5a68254b9b37bbc6b5e47658d3870a0d60b984f847cee10592977429`，测试服健康检查 `172.30.30.58:48081/actuator/health -> {"status":"UP"}`。

DCC-B-DELETE-EVIDENCE: `node tests\e2e\dcc-withdraw-delete-file.e2e.js` 逐个处理 B `V1.2=2054545668044049605`、`V1.1=2054545668044049604`、`V1.0=2054545668044049603` -> PASS，三个版本均通过真实前端详情页 `撤回申请` 后 `删除流程`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` with present A `2054545668044049602` and absent B ids `2054545668044049603/604/605` -> PASS，A 仍可访问，B 三个版本均返回 `Controlled file does not exist`。

B5-EVIDENCE: `backupId=20260608-153128`，manifest `/mnt/nas/Backup/BackupPackage/20260608-153128/manifest/manifest.json`，目标证明 `targetEnvironment=test`、`targetHost=172.30.30.58`，`imageTag=20260607_232258`，`recoverySet.status=COMPLETE`，MySQL dump 大小 `4085444873` bytes，对象统计 `added=0 modified=0 deleted=2 reused=2`，inventory 对象数 `4`，active 对象数 `2`，deleted 对象 `dcc/original/20260608/comments.docx` 与 `dcc/original/20260608/tables.docx`，复制候选 `0`，object-store 当前 `32610` objects / `137420847460` bytes，checksums sha256 `f2ab862f5a68254b9b37bbc6b5e47658d3870a0d60b984f847cee10592977429`，测试服健康检查 `172.30.30.58:48081/actuator/health -> {"status":"UP"}`。

RESTORE-B3-FAIL-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-144502` 首次执行在对象 manifest replay 阶段失败；MySQL 已从 `/mnt/nas/Backup/BackupPackage/20260608-144502/mysql/ruoyi-vue-pro.sql.gz` 导入成功，但对象恢复命令中 `/bin/sh` 未正确解析 `printf '\t'/'\r'`，导致 `cp` 把整行 TSV 当作 repositoryKey。失败后已在测试服 `172.30.30.58` 执行 `docker compose up -d backend frontend` 并确认健康检查 UP。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store"` -> FAIL，预期原因：对象复制/恢复脚本仍使用 `printf '\t'` / `printf '\r'`，在 alpine `/bin/sh` 中会被当作字面量而非 tab/CR。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store or remote_manifest_restore_mounts_object_store_readonly or remote_manifest_restore_ssh_steps_are_bounded"` -> PASS，3 passed，对象复制/恢复脚本改为 POSIX octal `printf '\011'` / `printf '\015'`。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

EVIDENCE: 修复后重新启动的 B2 旧进程 `backupId=20260608-140939` 命令行为 `-TargetEnvironment test`，MySQL dump 成功写入 `/mnt/nas/Backup/BackupPackage/20260608-140939/mysql/ruoyi-vue-pro.sql.gz`，但对象阶段列举恢复点仍在 300 秒后超时；日志重命名为 `20260608_140935_backup-now_fail.log`，该备份点不作为 B2 验收点。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ssh_short_reads_are_bounded_by_native_process_timeout"` -> FAIL，预期原因：SSH 公共参数未加 `-n`，Windows OpenSSH 子进程在非交互场景可能等待 stdin/会话关闭。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ssh_short_reads_are_bounded_by_native_process_timeout"` -> PASS，1 passed。

EVIDENCE: 使用真实测试服 `172.30.30.58` 通过 `Invoke-BackupSshCommand` 执行 `find '/mnt/nas/Backup/BackupPackage' ... | sort -r` 成功，返回 `66` 个恢复点，首项 `/mnt/nas/Backup/BackupPackage/20260608-140939`。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

EVIDENCE: 修复后重新启动的 B2 旧进程 `backupId=20260608-135324` 命令行为 `-TargetEnvironment test`，MySQL dump 成功写入 `/mnt/nas/Backup/BackupPackage/20260608-135324/mysql/ruoyi-vue-pro.sql.gz`，但对象阶段列举 `/mnt/nas/Backup/BackupPackage` 恢复点时 `find ... | sort -r` 60 秒超时，日志重命名为 `20260608_135322_backup-now_fail.log`；该备份点不作为 B2 验收点。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded"` -> FAIL，预期原因：BackupPackage 顶层恢复点列举仍使用 60 秒 timeout，对 NAS 元数据短时抖动过紧。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded"` -> PASS，1 passed，恢复点列举调整为 300 秒，单个 manifest 读取保持 60 秒。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

RESTORE-B3-FAIL-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-144502` 第二次执行仍在对象 manifest replay 阶段失败；MySQL 已从 `/mnt/nas/Backup/BackupPackage/20260608-144502/mysql/ruoyi-vue-pro.sql.gz` 导入成功，但 Alpine `/bin/sh` 的 `printf '\011'` 输出被解析为普通字符序列，导致 `cut` 不能按 tab 分列，`cp` 尝试读取错误 repositoryKey。失败后测试服 `172.30.30.58` backend/frontend 已恢复运行并确认健康检查 UP。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store"` -> FAIL，预期原因：对象复制/恢复脚本用 shell 变量保存 tab/CR 仍依赖目标 shell 的转义行为，BusyBox/Alpine 下无法稳定解析 object-store TSV。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store or remote_manifest_restore_mounts_object_store_readonly or remote_manifest_restore_ssh_steps_are_bounded"` -> PASS，3 passed，对象复制/恢复脚本改为 `tr -d '\015' | cut -f1/-f2-`，不再依赖 shell `printf` 转义。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

RESTORE-B3-FAIL-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-144502` 第三次执行在对象 manifest replay 阶段失败；MySQL 已导入成功，上传到 `.restore-stage` 的 `restore-object-plan.tsv` 使用 Windows CRLF，容器恢复目标路径末尾带 `\r`，导致 `cp` 报 `Invalid argument`。失败后测试服 `172.30.30.58` backend/frontend 已恢复运行并确认健康检查 UP。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "remote_nas_object_backup_short_ssh_steps_are_bounded or remote_manifest_restore_ssh_steps_are_bounded"` -> FAIL，预期原因：对象复制/恢复 TSV plan 本地写入使用 `[Environment]::NewLine`，Windows 下生成 CRLF。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store or remote_nas_object_backup_short_ssh_steps_are_bounded or remote_manifest_restore_ssh_steps_are_bounded or remote_manifest_restore_mounts_object_store_readonly"` -> PASS，4 passed，对象复制/恢复 TSV plan 固定 UTF-8 LF-only，PlanOnly 命令和真实执行命令均不再依赖 `tr` 清理 CR。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

EVIDENCE: 第三次失败恢复后测试服当前 `.env` / compose imageTag 已为 `26-06-08_16-11-25`，而 B1-B5 manifest imageTag 为 `20260607_232258`；恢复验收必须先让测试服程序 imageTag 与备份点 manifest 匹配，否则恢复流程应 fail fast。

EVIDENCE: 为执行 B1-B5 历史恢复点验收，仅在测试服 `172.30.30.58` 将 `/opt/intruoyi/runtime/.env` 的 `IMAGE_TAG` 切回 manifest 匹配值 `20260607_232258`，并用本地已存在的 `intruoyi-backend:20260607_232258` / `intruoyi-frontend:20260607_232258` 重建 backend/frontend；随后健康检查 `172.30.30.58:48081/actuator/health -> {"status":"UP"}`。

RESTORE-B3-FAIL-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-144502` 第四次执行在对象 manifest replay 阶段失败；远端 `restore-object-plan.tsv` 已确认是 LF-only，容器内 `read/cut` 调试确认 `repositoryKey` 与 `path` 不含 CR/LF，但真实恢复命令使用 `printf '%s\n' "$line" | cut ...` 时向恢复目标 path 拼入字面 `\n`，导致 `cp` 对 `/restore/yudao/...docx\n` 报 `Invalid argument`。失败后测试服 `172.30.30.58` backend/frontend 已恢复运行。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store"` -> FAIL，预期原因：对象复制/恢复脚本仍用 `printf '%s\n'` 解析 TSV 字段，可能把换行序列带入字段值。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "backup_ops_stores_incremental_objects_in_remote_object_store or remote_nas_object_backup_short_ssh_steps_are_bounded or remote_manifest_restore_ssh_steps_are_bounded or remote_manifest_restore_mounts_object_store_readonly"` -> PASS，4 passed，字段解析改为 `printf '%s' "$line" | cut ...`，不再向字段值追加换行。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

RESTORE-B3-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-144502` -> SUCCESS，日志 `D:\IntRuoyi-BackupOps\logs\202606\20260608_165849_restore-data_success.log`；恢复前检测当前 runtime `IMAGE_TAG=20260607_232258` 与 manifest 匹配，MySQL 从 `/mnt/nas/Backup/BackupPackage/20260608-144502/mysql/ruoyi-vue-pro.sql.gz` 导入成功，对象按 `/mnt/nas/Backup/BackupPackage/20260608-144502/objects/manifest-object-inventory.json` replay 成功，backend/frontend 与操作机侧健康检查均通过，目标证明为 `172.30.30.58`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B3 restore -> PASS，测试服前端 `http://172.30.30.58:8081`、测试租户 `aoteman`；A `2054545668044049602` 可访问且 `previewFileName=empty.docx`，B V1.0 `2054545668044049603` 可访问且 `previewFileName=comments.docx`，B V1.1 `2054545668044049604` 与 B V1.2 `2054545668044049605` 均返回 `Controlled file does not exist`。

RESTORE-B4-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-151355` -> SUCCESS，日志 `D:\IntRuoyi-BackupOps\logs\202606\20260608_171352_restore-data_success.log`；恢复前检测当前 runtime `IMAGE_TAG=20260607_232258` 与 manifest 匹配，MySQL 从 `/mnt/nas/Backup/BackupPackage/20260608-151355/mysql/ruoyi-vue-pro.sql.gz` 导入成功，对象按 `/mnt/nas/Backup/BackupPackage/20260608-151355/objects/manifest-object-inventory.json` replay 成功，backend/frontend 与操作机侧健康检查均通过，目标证明为 `172.30.30.58`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B4 restore -> PASS，测试服前端 `http://172.30.30.58:8081`、测试租户 `aoteman`；B V1.2 `2054545668044049605` 可访问，`versionNo=V1.2`，`previewFileName=comments.docx`，`previewKind=OFFICE`。下载接口对当前用户返回 `Current user cannot access this controlled file`，因此内容证明补充使用恢复后的 MinIO 对象与 object-store 校验。

B4-CONTENT-EVIDENCE: 恢复后测试服 MinIO `yudao/dcc/original/20260608/comments.docx` -> `size=13087`、`etag=bf94ebce128c83db4f779493d7a5a866`；BackupPackage object-store `/mnt/nas/Backup/BackupPackage/object-store/bf94ebce128c83db4f779493d7a5a866` -> `13087` bytes，SHA256 `9f75a82d36530d91989a43fcb8940f2e753051787714cecbf6a1f49a22e58ded`，与本地修改源 `D:\IntRuoyi-BackupOps\tmp\dcc-b4-modified-source\comments.docx` SHA256 一致。

RESTORE-B5-EVIDENCE: `restore-data -TargetEnvironment test -SelectedBackupId 20260608-153128` -> SUCCESS，日志 `D:\IntRuoyi-BackupOps\logs\202606\20260608_172720_restore-data_success.log`；恢复前检测当前 runtime `IMAGE_TAG=20260607_232258` 与 manifest 匹配，MySQL 从 `/mnt/nas/Backup/BackupPackage/20260608-153128/mysql/ruoyi-vue-pro.sql.gz` 导入成功，对象按 `/mnt/nas/Backup/BackupPackage/20260608-153128/objects/manifest-object-inventory.json` replay 成功，backend/frontend 与操作机侧健康检查均通过，目标证明为 `172.30.30.58`。

GREEN: `node tests\e2e\dcc-restore-verify.e2e.js` after B5 restore -> PASS，测试服前端 `http://172.30.30.58:8081`、测试租户 `aoteman`；A `2054545668044049602` 可访问且 `previewFileName=empty.docx`，B V1.0 `2054545668044049603`、B V1.1 `2054545668044049604`、B V1.2 `2054545668044049605` 均返回 `Controlled file does not exist`。

B5-DELETE-EVIDENCE: 恢复后测试服 MinIO 当前 bucket 中 `yudao/dcc/original/20260608/comments.docx` 与 `yudao/dcc/original/20260608/tables.docx` 均 `Object does not exist`；历史 B3/B4 已在 B5 生成后分别恢复成功，证明 B5 删除状态不会破坏历史备份点可恢复性。

RETENTION-PLAN-EVIDENCE: `Invoke-BackupOpsRemoteRetention -PlanOnly` 仅连接测试服 `172.30.30.58`，根目录固定 `/mnt/nas/Backup/BackupPackage`，`action=plan` 未执行删除；`keepDays=30`、`keepLast=5`、`maxNasUsedPercent=90`，容量 `usedPercent=69.555` 未超阈值，`deletedBackupPoints=[]`，`retainedBackupPoints` 包含 B1 `20260608-121507`、B2 `20260608-142850`、B3 `20260608-144502`、B4 `20260608-151355`、B5 `20260608-153128`。

RETENTION-OBJECT-EVIDENCE: 只读校验所有保留 manifest 后，关键对象 `A=e02bf390e42e9c22f046918fcd724a16`、`B3=1649491b261d7157babbd8b5ec283b29`、`B4=bf94ebce128c83db4f779493d7a5a866` 均 `exists=true`、`referenced=true`、`deleteCandidate=false`；dry-run 报告的 object-store 清理候选均为不在保留 manifest 引用集合内的孤立 blob，未执行删除。

SAFETY-EVIDENCE: `rg -n "172\.30\.30\.57|root@172\.30\.30\.57|promote-prod|targetEnvironment=prod|TargetEnvironment prod" D:\IntRuoyi-BackupOps\logs\202606 --glob "20260608_*"` -> no matches；本任务 2026-06-08 的备份/恢复日志未出现正式服 IP、prod target 或 promote-prod。

REGRESSION: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。

REGRESSION: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，82 tests。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。
