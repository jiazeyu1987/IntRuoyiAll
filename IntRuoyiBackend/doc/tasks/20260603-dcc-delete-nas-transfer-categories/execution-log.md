# 执行记录：删除 NAS 转移生成的文件类别

## BDD

BDD: 删除未被引用的 NAS_TRANSFER 文件类别 -> Given 某个 `source=NAS_TRANSFER` 文件类别没有目录绑定且没有受控文件引用 / When 执行清理 / Then 该类别可以安全删除且不影响现有 DCC 文件。

BDD: 被引用的 NAS_TRANSFER 文件类别不得盲删 -> Given 某个 `source=NAS_TRANSFER` 文件类别仍有目录绑定或受控文件引用 / When 用户要求删除这些类别 / Then 系统必须先明确迁移或删除这些引用数据，不能直接删除类别。

## Evidence

- Read-only SQL: `dcc_file_category` 当前 `source='NAS_TRANSFER'` 共 `99` 条。
- Read-only SQL: 旧批次 `900298-900346` 当前 `binding_count=0` 且 `file_count=0`。
- Read-only SQL: 新批次 `906105-906154` 存在大量真实引用，不能直接删除全部类别。
- Read-only SQL: 目标类别 `其他` 的 id 为 `906104`。
- Read-only SQL: `dcc_controlled_file_master` 在 `category_id + file_name` 上有唯一键，`NAS_TRANSFER` 数据存在 `19` 组重名文件链，共 `27` 条重复 master 需要先合并。

## Cleanup

- CLEANUP: 在单个事务内先将 `27` 条重复 `master_id` 关联文件重定向到保留 master，再把 `959` 条 `dcc_controlled_file` 和 `959` 条 `dcc_controlled_file_master` 迁移到 `category_id=906104`。
- CLEANUP: 逻辑删除 `99` 条 `source='NAS_TRANSFER'` 文件类别及其目录绑定、审批路线、路线节点、权限规则、分发规则、培训规则。

## Verification

- VERIFY: `active_nas_categories=0`
- VERIFY: `nas_files=0`
- VERIFY: `nas_masters=0`
- VERIFY: `nas_bindings=0`
- VERIFY: `nas_routes=0`
- VERIFY: `nas_route_nodes=0`
- VERIFY: `nas_permission_rules=0`
- VERIFY: `nas_distribution_rules=0`
- VERIFY: `nas_training_rules=0`
- VERIFY: `other_files=959`
- VERIFY: `other_masters=932`
- VERIFY: `duplicate_master_names_in_other=0`
- VERIFY: `duplicate_file_master_links_missing=0`

## Closeout

- task-closeout-cleanup preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-delete-nas-transfer-categories --mode preview` -> PASS, keep `task.md` / `execution-log.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.

## Blockers

- none
