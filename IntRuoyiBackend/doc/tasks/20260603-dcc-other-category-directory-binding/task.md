# 任务：定位 DCC 其他类别目录绑定缺失

## 任务目标

定位前端“转移到 DCC”选择模板类别 `其他` 时返回 `selected category is not bound to a directory: 906104` 的原因，确认这是本机数据缺失、配置缺失还是代码行为仍需调整，并在有明确根因和验证路径后完成最小修复。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-nas-transfer-local-backend-restart/task.md`
- 状态：`completed`
- 影响：上一个任务已完成本机后端重启并确认最新修复已加载；本任务继续处理修复加载后暴露出的类别 `906104` 目录绑定缺失问题。

## BDD 场景

- BDD: 其他类别必须有目录绑定 -> Given 用户在 NAS 转移弹窗中选择目录 `1. QMS documents` 且模板类别为 `其他(906104)` / When 提交转移任务 / Then 后端必须确认该类别存在有效目录绑定且覆盖所选目录，否则明确返回类别未绑定目录错误。
- BDD: 数据修复不得跨租户或误绑目录 -> Given 类别、目录和绑定数据属于具体租户 / When 修复 `906104` 的目录绑定 / Then 只能在同一租户、唯一确认的目标目录上新增或恢复绑定；如果租户或目标目录不唯一，必须失败并报告。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：只读查询类别 `906104`、`1. QMS documents` 目录和现有绑定状态。
- [x] M3：复现或确认当前接口错误与数据状态一致。
- [x] M4：基于根因选择正式修复路径并执行最小变更。
- [x] M5：运行回归验证并记录证据。
- [x] M6：完成任务收尾、清理预览和提交。

## Expected Verification

- 只读 SQL 能说明 `906104` 的租户、名称、启用状态、删除状态和现有绑定情况。
- 若执行数据修复，修复前必须确认目标目录唯一、租户一致、类别未有有效绑定。
- 修复后再次查询可证明 `906104` 存在有效目录绑定，且接口/页面不再返回 `selected category is not bound to a directory: 906104`。
- 若缺少唯一目标目录、租户一致性或验证入口，必须记录阻塞和影响，不得用 fallback 或静默跳过代替。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。绑定缺失继续显式失败，不新增兜底类别或默认目录。
- `是否从根因和长期维护角度解决`：是。先确认运行数据与业务目录关系，再决定配置/数据/代码的正式修复。
- `是否存在临时补丁或绕过`：否。不会通过前端隐藏错误或后端默认成功绕过绑定校验。

## 当前状态

completed

## 已完成工作

- 用户反馈当前前端报错：`selected category is not bound to a directory: 906104`。
- 已确认上一后端重启任务完成，当前报错说明本机后端已加载类别绑定校验。
- 只读查询确认 `906104` 为 tenant `1` 下启用的 `其他` 类别，但没有活动目录绑定。
- 只读查询确认 tenant `1` 当前活动 DCC 目录数为 `0`，历史 `1. QMS documents` 目录记录均为 `deleted=1`。
- 已新增本机数据修复脚本 `apply-local-other-qms-binding.sql`，仅在类别、租户、目录状态和绑定状态满足唯一预检时创建 `1. QMS documents` 根目录并绑定到 `906104`。
- 执行本机数据修复：创建活动 DCC 根目录 `906306 / 1. QMS documents`，新增活动绑定 `906254`，`category_id=906104 -> directory_id=906306`。

## 验证结果

- RED：本机只读 SQL -> FAIL，`category=906104/其他/active=1/tenant_id=1/deleted=0`，但 `active_dirs=0`、`active_qms_roots=0`、`active_bindings_906104=0`。
- GREEN：执行 `apply-local-other-qms-binding.sql` -> PASS，返回 `category_id=906104`、`directory_id=906306`、`directory_name=1. QMS documents`。
- GREEN：修复后只读 SQL -> PASS，tenant `1` 活动 DCC 目录数为 `1`，`906104` 活动绑定数为 `1`，绑定指向活动目录 `906306`。
- GREEN：重复执行 `apply-local-other-qms-binding.sql` -> PASS，返回同一个 `directory_id=906306`，未新增重复目录或绑定。
- GREEN：`validate_database_schema.py --evidence doc/tasks/20260603-dcc-other-category-directory-binding/database-schema-evidence.md` -> PASS。
- GREEN：`validate_bug_regression.py --evidence doc/tasks/20260603-dcc-other-category-directory-binding/bug-regression-evidence.md` -> PASS。
- CLOSEOUT PREVIEW：`task_closeout.py --task-id 20260603-dcc-other-category-directory-binding --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- none.

## Cleanup Keep

- `doc/tasks/20260603-dcc-other-category-directory-binding/apply-local-other-qms-binding.sql`
- `doc/tasks/20260603-dcc-other-category-directory-binding/database-schema-evidence.md`
- `doc/tasks/20260603-dcc-other-category-directory-binding/bug-regression-evidence.md`
