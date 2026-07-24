# Execution Log

BDD: baseline 到 V1 恢复 -> Given baseline B1 和增量 B2/B3 形成连续链 / When 计划恢复到 B3 / Then 恢复计划从 baseline 开始重放到 B3，并证明文件 B 的 V1 记录、原文对象和 preview 对象可恢复。

BDD: V2 修改恢复 -> Given B4 在 B3 后修改文件 B / When 计划恢复到 B4 / Then 恢复计划包含 B4 segment，文件 B 指向 V2 对象，V1 不覆盖 V2。

BDD: 删除恢复 -> Given B5 记录文件 B 删除事件 / When 计划恢复到 B5 / Then 文件 B 在目标状态中为 deleted/tombstone，恢复对象集不包含已删除对象，但历史 B3/B4 仍可计划恢复。

BDD: 作废和权限恢复 -> Given 增量链中包含 void 与 permission_change 事件 / When 计划恢复到对应恢复点 / Then 恢复计划保留作废状态和权限摘要，不把作废文件恢复为 active。

BDD: preview 对象校验 -> Given DCC 文件存在 preview 对象 / When 计划恢复到任一包含该文件的恢复点 / Then 原文与 preview 对象必须同时可由 object-store 引用；缺少 preview 对象时阻断。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q -k "dcc_chain_plan_restore_replays_baseline_incremental_states"` -> FAIL，预期原因：`New-DccRestoreReplayPlan` 只返回目标恢复点 records/inventory，没有输出 baseline + incrementalChain 重放后的 `finalFiles`，无法证明 B4 使用 V2、B5 删除、作废和 preview 状态。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q -k "dcc_chain_plan_restore_replays_baseline_incremental_states"` -> PASS，1 passed；恢复计划新增 `replayPointIds` 和 `finalFiles`，按 baseline 到目标恢复点顺序累积 databaseRecords，并绑定目标点 active object inventory。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，131 passed。

EXPERIENCE: 使用 project-experience-consolidation，将阶段 4 经验合并到 root worktree 的 `docs/release-backup-restore.md`：DCC 链式恢复计划必须输出 `replayPointIds`、segments 和重放后的 `finalFiles`，验收 V1/V2/删除/作废/权限/preview 时不能只读目标点单快照。

CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-dcc-chain-restore-replay --mode preview --worktree-closeout off` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
