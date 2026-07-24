# 20260608-backup-incremental-manifest-short-loop

## 任务目标

将当前备份机制从“每次全量 MySQL dump + 全量 MinIO 对象 tar”推进为适合大数据库和大量文件的正式备份方案：MySQL 当前保留全量基线并明确增量前置条件，MinIO / 文件对象改为对象级增量 manifest 和共享 object-store，恢复按 manifest 精确还原对象集，并通过测试服 `172.30.30.58` 与测试租户 DCC 真实路径完成 B1-B5 连续备份和 B3/B4/B5 恢复闭环验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。增量前置条件不满足时必须 fail fast，不允许伪装成增量成功。
- 是否从根因和长期维护角度解决：是。对象备份以 manifest + content-addressed object-store 作为正式模型，MySQL 增量以 binlog 或 xtrabackup 前置条件证明为进入条件。
- 是否存在临时补丁或绕过：否。短闭环验收必须走测试租户真实 DCC 前端路径，不用脚本直接塞文件替代。

## BDD 场景

- BDD: 对象级增量备份复用未变化对象 -> Given B1 已生成对象 manifest 和 object-store / When DCC 文件未变化并执行 B2 / Then B2 生成新 manifest，未变化对象计入 reused，复制计划不包含未变化对象。
- BDD: 新增对象进入新备份点 -> Given 测试租户通过真实 DCC 前端路径新增文件 B / When 执行 B3 / Then B3 manifest 包含文件 B，object-store 只新增 B 对应对象，文件 A 不重复复制。
- BDD: 修改对象生成可恢复新版本 -> Given 文件 B 已存在 / When 通过真实 DCC 前端路径修改 B 并执行 B4 / Then B4 manifest 指向 B 的新 repositoryKey，恢复到 B4 时内容为修改后内容。
- BDD: 删除对象在新恢复点消失但历史可恢复 -> Given B3/B4 历史恢复点仍在保留范围内 / When 通过真实 DCC 前端路径删除 B 并执行 B5 / Then B5 manifest 体现删除，恢复 B5 不出现 B，恢复 B3/B4 仍可得到对应历史版本。
- BDD: 恢复前校验环境和完整性 -> Given 运维选择测试服备份点 / When 执行恢复 / Then 校验 imageTag、MySQL 链、对象 manifest 引用、checksums 和测试服健康检查，且目标证明为 `172.30.30.58`。
- BDD: 保留策略只清理合法备份数据 -> Given BackupPackage 中存在过期恢复点和未被保留 manifest 引用的对象 / When 执行保留策略 / Then 只清理合法备份点和孤立对象，不触碰 ReleasePackage、NAS 根目录、挂载或业务目录。
- BDD: 备份执行阶段不得临时整桶镜像 -> Given 最新历史备份点已经记录未变化对象的 sourceEtag、size 和 repositoryPath / When 新备份扫描到相同对象 / Then 本次备份只复用 object-store 引用，不执行整桶 `mc mirror`，也不对未变化对象执行 `mc cp`。

## 里程碑

- [x] M1：检查现有备份链路、运行控制台、BackupPackage / ReleasePackage 分离、manifest、恢复流程和保留策略现状。
- [x] M2：用 RED 测试锁定对象增量 manifest 行为，尤其未变化对象不得进入复制计划。
- [x] M3：最小实现 GREEN，对象复制只覆盖新增/修改对象，删除对象写入 manifest，恢复按 manifest 还原。
- [x] M4：补齐 MySQL 大库方案文档和 fail-fast 前置条件，保留当前全量基线，不静默降级为伪增量。
- [x] M5：运行控制台展示备份模式、保留策略、最近备份点和对象增量统计。
- [x] M6：在测试服 `172.30.30.58` 使用测试租户 DCC 真实路径完成 B1-B5 连续备份与 B3/B4/B5 恢复验证。
- [x] M7：更新 evidence、execution-log，运行 task-closeout-cleanup 预览，只提交本任务相关改动。

## MySQL 大库备份策略记录

当前交付阶段不声明 MySQL 增量已经实现；MySQL 仍保留一次完整逻辑 dump 作为可恢复基线，并在 manifest 的 `recoverySet.mysql.dumpPath` 中声明该基线。对象文件已经改为对象级增量 manifest，避免 MinIO / 文件对象每次全量 tar。

正式 MySQL 增量方案候选：

- binlog 链：要求测试服和正式方案均启用 `log_bin`、记录 `binlog_format=ROW`、具备 `REPLICATION CLIENT` 或等效权限、能记录全量基线对应的 binlog file/position 或 GTID，并能在恢复时校验基线 dump 加连续 binlog 链完整。缺少任一前置条件必须 fail fast，不得改跑全量并声称增量成功。
- xtrabackup 物理链：要求 MySQL 版本与 xtrabackup 兼容、容器或宿主具备可读取 datadir 的权限、NAS staging 有足够容量、恢复过程可在测试服停止 MySQL 后 prepare/apply-log，并记录 full/inc chain 的 LSN。缺少依赖、权限、容量或停机窗口证明时必须 fail fast。

本任务验收中的 B1-B5 使用当前全量逻辑基线恢复 MySQL，再用对象级增量 manifest 验证文件对象的新增、修改、删除、复用和按点恢复行为。后续若切入 MySQL 增量，必须先新增 binlog 或 xtrabackup 前置条件检测的 RED 测试，再实现链完整性校验。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q`
- `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q`
- 测试服 `targetEnvironment=test` 的 B1-B5 备份和 B3/B4/B5 恢复证据，所有备份数据位于 `/mnt/nas/Backup/BackupPackage`。
- E2E 验证必须由 Playwright 登录测试租户并操作 DCC 真实用户路径；若入口或数据前置条件不存在，记录 blocker，不用 mock 或脚本造成功。

## 当前状态

completed

## Verification Result

- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，96 passed。
- `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，35 passed；新增覆盖 Linux `backup_now` 不再使用整桶 `mc mirror`，未变化对象只复用 object-store。
- `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，82 tests。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。
- B1 `20260608-121507`、B2 `20260608-142850`、B3 `20260608-144502`、B4 `20260608-151355`、B5 `20260608-153128` 均写入测试服 NAS `/mnt/nas/Backup/BackupPackage` 并记录 `targetEnvironment=test` / `targetHost=172.30.30.58`。
- B3/B4/B5 均通过 `restore-data -TargetEnvironment test` 恢复；B3 验证 A 与 B V1.0 存在，B 后续版本不存在；B4 验证 B V1.2 修改后对象 size/etag/SHA256；B5 验证 B 三个版本不存在且当前 bucket 删除对象 path。

## Remaining Blocker

- 已解除。PowerShell 工具侧 fixture 已补齐当前 DCC 链完整性契约字段，组合回归 `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，125 passed。
- task-closeout-cleanup 使用 `--worktree-closeout off` 仅执行本任务产物清理预览 -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
- 后端提交：`3280328b89`（任务: 优化DCC对象增量备份复制）。
