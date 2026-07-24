# 任务：恢复数据取消恢复演练门禁

## 任务目标

按用户要求，运行控制台“恢复数据”不再因为缺少恢复演练报告或恢复前现场快照而阻断；仍保留真实备份点、manifest、checksum、镜像标签等基础恢复证据校验，不伪造“恢复演练成功”。

## 上一任务检查

- 上一个后端运行控制台任务 `20260602-runtime-control-mixed-rollout-default-owner` 原状态为 `in_progress`。
- 已记录为 `blocked`：用户切换到当前恢复数据门禁调整需求；上一任务已修改代码保持原样，本任务不接管。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是。用户明确要求取消恢复演练限制；本任务仅取消演练报告/现场快照对恢复候选可用性的阻断，不伪造成功报告，不吞掉 manifest/checksum/imageTag 错误。
- `是否从根因和长期维护角度解决`：是。通过调整正式门禁契约与测试实现，不通过手写证据或默认成功绕过。
- `是否存在临时补丁或绕过`：否。不写入假的 `rehearsal-report.json` 或 `现场快照.md`。

## BDD 场景

BDD: 缺少演练证据的真实备份点仍可用于恢复 -> Given `Backup/BackupPackage/<backupId>` 存在 manifest、checksum 和镜像标签 / When 恢复候选缺少 `manifest/rehearsal-report.json` 与 `manifest/现场快照.md` / Then 运行控制台仍应将该候选标记为 `AVAILABLE`。

BDD: 基础恢复证据缺失仍阻断 -> Given 备份点缺少 manifest、checksum 或镜像标签 / When 用户查询恢复候选 / Then 后端仍应返回 `BLOCKED` 并保留明确原因。

BDD: 备份点列表不再把演练证据作为可恢复条件 -> Given 备份点 manifest 与 checksum 有效但未演练 / When 查看 Backup 面板 / Then 备份点应显示为 `RECOVERABLE`，演练报告路径可为空。

## 里程碑

- [x] M1：记录用户变更决策与上一任务阻塞状态。
- [x] M2：补充 RED 测试，证明当前代码仍因缺演练证据阻断。
- [x] M3：最小修改恢复候选与备份点可恢复性门禁。
- [x] M4：运行目标单测与回归验证。
- [x] M5：执行收尾清理预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-without-rehearsal-gate --mode preview`

## 当前状态

Completed. 已按用户要求取消恢复演练报告与现场快照对恢复数据候选的阻断；仍保留 manifest、checksum、镜像标签等基础恢复证据校验。不采用默认恢复演练成功，不写入伪造演练报告。

## 最终验证结果

- RED：目标测试先失败，确认旧代码仍因缺少演练证据阻断恢复候选。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests，0 failures，0 errors。
- 变更证据校验：`validate_change_request.py` -> PASS。
- 后端证据校验：`validate_backend_api.py` -> PASS。
- 收尾清理预览：`task_closeout.py --task-id 20260603-restore-data-without-rehearsal-gate --mode preview` -> PASS，无删除项、无阻塞。

## Cleanup Keep

- doc/tasks/20260603-restore-data-without-rehearsal-gate/backend-api-evidence.md
