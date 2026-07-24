# 任务：删除 NAS 转移生成的文件类别

## 任务目标

清理 DCC 文件类别中由 NAS 转移历史生成的 `source=NAS_TRANSFER` 类别，并避免误删仍被真实 DCC 文件或目录绑定引用的数据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-nas-transfer-use-selected-category/task.md`
- 状态：`completed`
- 影响：上一任务修复后续 NAS 转移不再自动生成目录名类别；本任务处理历史运行库遗留数据。

## BDD 场景

- BDD: 删除未被引用的 NAS_TRANSFER 文件类别 -> Given 某个 `source=NAS_TRANSFER` 文件类别没有目录绑定且没有受控文件引用 / When 执行清理 / Then 该类别可以安全删除且不影响现有 DCC 文件。
- BDD: 被引用的 NAS_TRANSFER 文件类别不得盲删 -> Given 某个 `source=NAS_TRANSFER` 文件类别仍有目录绑定或受控文件引用 / When 用户要求删除这些类别 / Then 系统必须先明确迁移或删除这些引用数据，不能直接删除类别。

## Milestones

- [x] M1：建立任务文档并确认上一任务已完成。
- [x] M2：只读盘点 `NAS_TRANSFER` 文件类别及其绑定/文件引用。
- [x] M3：根据用户确认的清理范围执行数据清理。
- [x] M4：验证清理结果并记录证据。

## Expected Verification

- Read-only SQL 识别当前 `NAS_TRANSFER` 类别总数、孤儿类别数、被引用类别数。
- 若执行删除，仅删除用户确认范围内的数据，并通过 SQL 验证目标记录已清除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。被引用类别若无明确迁移/删除方案，直接阻塞。
- `是否从根因和长期维护角度解决`：是。先修复后续生成逻辑，再清理历史脏数据。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已确认清理方案

- 用户已明确确认：先把 `NAS_TRANSFER` 文件类别关联文件统一迁移到 `其他(906104)`，再删除这些 `NAS_TRANSFER` 文件类别。

## 已完成工作

- 盘点确认 `其他` 文件类别 id 为 `906104`。
- 识别到 `NAS_TRANSFER` 类别存在 `19` 组 `dcc_controlled_file_master.file_name` 重名冲突，若直接迁到 `其他` 会违反唯一键。
- 先将这 `19` 组中的 `27` 条重复 master 链合并到保留链，再把 `959` 条 `dcc_controlled_file` 与 `959` 条 `dcc_controlled_file_master` 的类别归属迁移到 `其他(906104)`。
- 逻辑删除 `99` 条 `dcc_file_category(source=NAS_TRANSFER)`，并同步逻辑删除其 `50` 条目录绑定、`297` 条审批路线、`1188` 条路线节点、`495` 条权限规则、`99` 条分发规则、`99` 条培训规则。

## 最终验证结果

- `active_nas_categories=0`
- `nas_files=0`
- `nas_masters=0`
- `nas_bindings=0`
- `nas_routes=0`
- `nas_route_nodes=0`
- `nas_permission_rules=0`
- `nas_distribution_rules=0`
- `nas_training_rules=0`
- `other_files=959`
- `other_masters=932`
- `duplicate_master_names_in_other=0`
- `duplicate_file_master_links_missing=0`
