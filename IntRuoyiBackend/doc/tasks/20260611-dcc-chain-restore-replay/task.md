# 20260611-dcc-chain-restore-replay

## 任务目标

实现阶段 4：链式恢复。DCC 恢复不能只读取目标恢复点当前快照，而必须能从 full baseline 开始，按 incrementalChain 顺序重放到指定目标恢复点，并校验 V1/V2、删除、作废、权限变更和 preview 对象状态。恢复计划缺段、乱序、状态不一致或对象引用缺失时必须 fail fast。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。链式恢复缺少任一 segment、事件或对象引用时阻断，不允许回退到目标点当前状态快照冒充链式恢复。
- 是否从根因和长期维护角度解决：是。恢复计划以 manifest 的 baseline、incrementalChain、databaseRecords、objectInventories 和 dccEvents 为统一契约。
- 是否存在临时补丁或绕过：否。本阶段只实现本地脚本和测试，不访问正式服务器。

## BDD 场景

- BDD: baseline 到 V1 恢复 -> Given baseline B1 和增量 B2/B3 形成连续链 / When 计划恢复到 B3 / Then 恢复计划从 baseline 开始重放到 B3，并证明文件 B 的 V1 记录、原文对象和 preview 对象可恢复。
- BDD: V2 修改恢复 -> Given B4 在 B3 后修改文件 B / When 计划恢复到 B4 / Then 恢复计划包含 B4 segment，文件 B 指向 V2 对象，V1 不覆盖 V2。
- BDD: 删除恢复 -> Given B5 记录文件 B 删除事件 / When 计划恢复到 B5 / Then 文件 B 在目标状态中为 deleted/tombstone，恢复对象集不包含已删除对象，但历史 B3/B4 仍可计划恢复。
- BDD: 作废和权限恢复 -> Given 增量链中包含 void 与 permission_change 事件 / When 计划恢复到对应恢复点 / Then 恢复计划保留作废状态和权限摘要，不把作废文件恢复为 active。
- BDD: preview 对象校验 -> Given DCC 文件存在 preview 对象 / When 计划恢复到任一包含该文件的恢复点 / Then 原文与 preview 对象必须同时可由 object-store 引用；缺少 preview 对象时阻断。

## 里程碑

- [x] M1：审查现有 DCC chain validator 和 Linux restore_data 的链式恢复能力。
- [x] M2：补 RED 测试覆盖 baseline + increment replay 到 B3/B4/B5。
- [x] M3：实现链式恢复计划与状态校验。
- [x] M4：运行回归、更新长期经验和提交。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q`
- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`
- `task-closeout-cleanup --mode preview`

## 当前状态

completed

## Verification Result

- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -q -k "dcc_chain_plan_restore_replays_baseline_incremental_states"` -> PASS，1 passed。
- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，131 passed。
- 阶段经验已写入 root worktree `docs/release-backup-restore.md`。
