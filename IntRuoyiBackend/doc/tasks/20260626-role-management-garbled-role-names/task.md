# 任务：角色管理页乱码角色名回归修复（后端）

## 任务目标

- 为角色名乱码回归提供正式 SQL 修复脚本与契约测试。
- 仅更新 `system_role` 中已确认损坏且目标值明确的角色记录。
- 保证修复脚本可重复执行且不会修改无关角色。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\task.md`
- 状态：`COMPLETED`
- 处理：上一任务处理菜单结构；本次仅补角色数据修复 SQL 与数据库回归契约。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁：
  - 中文 SQL 修复必须使用 UTF-8 安全方式落盘，推荐 `CONVERT(0x... USING utf8mb4)`。
  - 数据修复必须先有 RED 契约，再落正式 SQL 修复物。
  - 本机数据库写入前必须在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 乱码角色修复 SQL 必须恢复正式名称 -> Given SQL 修复文件执行于含损坏角色数据的库 / When 更新目标角色记录 / Then 这些角色名称恢复为正式中文值。`
- `BDD: 修复 SQL 必须保持最小范围 -> Given 运行库存在大量正常角色 / When 执行修复 SQL / Then 仅命中明确列出的角色 ID，不修改其他 system_role 记录。`

## 里程碑

1. M1：补 SQL 契约 RED。`COMPLETED`
2. M2：新增正式修复 SQL。`COMPLETED`
3. M3：执行本机验证并回写证据。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest script/tests/test_role_name_garbled_repair_sql.py -q`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT id, tenant_id, code, name, remark FROM system_role WHERE id IN (910208,910209,910234,910235,910236,910237) ORDER BY id;"`

## 当前结论

- 已新增正式修复 SQL：`sql/mysql/20260626_role_name_garbled_repair.sql`。
- 已新增 SQL 契约：`script/tests/test_role_name_garbled_repair_sql.py`。
- 本机数据库执行修复后，目标记录当前为：
  - `910208 -> tenant_id=1 / EDITOR / 展厅编辑 / 提供展厅编辑`
  - `910209 -> tenant_id=122 / showroom_publicity / 企宣角色`
  - `910234~910237 -> tenant_id=1 / eDHR演练-执行人、审批人、归档员、只读`
- 当前后端任务完成口径：正式 SQL、契约测试和本机数据库回查均已完成并通过。

## Cleanup Keep

- `ruoyi-vue-pro/doc/tasks/20260626-role-management-garbled-role-names/task.md`
- `ruoyi-vue-pro/doc/tasks/20260626-role-management-garbled-role-names/execution-log.md`
