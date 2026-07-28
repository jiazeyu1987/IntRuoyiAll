# 用户列表访问权限角色

## Task Goal

新增幂等 SQL，将 `system:user:query` 用户列表查询权限赋予权限角色 `用户列表访问`，用于人员选择弹窗加载用户列表。

## Milestones

- [x] 读取数据库、任务收尾、PowerShell 编码和数据库交付技能规则。
- [x] 核对 `system:user:query` 后端权限和 `system_menu/system_role/system_role_menu` 基线结构。
- [x] 先补充 SQL 静态合同，锁定角色和权限绑定。
- [x] 新增幂等 SQL 创建或修复 `用户列表访问` 角色并绑定真实权限菜单。
- [x] 运行 SQL 合同与安全检查。
- [ ] 更新任务文档、验证报告并提交推送。

## Expected Verification

- `python -m pytest IntRuoyiBackend/script/tests/test_user_list_access_role_sql.py`
- `git diff --check`
- `python -X utf8 IntRuoyiBackend/script/release/run-release-migration-policy-gate.py --sql-root IntRuoyiBackend/sql/mysql --sql-file IntRuoyiBackend/sql/mysql/20260707_system_role_category_management.sql --sql-file IntRuoyiBackend/sql/mysql/20260728_user_list_access_role.sql`

## Current Status

ready_for_closeout

## 经验门禁

### 菜单权限 SQL 稳定键门禁

- Trigger: 新增或修改菜单、权限、角色、角色菜单绑定 SQL。
- Preflight check: 以 `system_menu.permission`、`system_role.code` 等稳定业务键解析真实 ID；写入前核对 `system_menu/system_role/system_role_menu` 结构与字符列排序规则。
- Blocker: 目标权限菜单缺失、角色分类缺失、硬编码角色菜单关系 ID、角色 ID 冲突或排序规则不匹配时停止。
- Verification: 静态合同覆盖 release metadata、稳定 role code、权限字符串、非破坏性和不写死绑定 ID。
- Forbidden action: 禁止用 mock 权限、默认成功、固定关系 ID 或扩大授权范围掩盖权限缺失。
- Evidence: `docs/database-rules.md`、`docs/system/data-model.md`、`docs/experience-index.md` 菜单权限相关门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过正式角色权限 SQL 绑定后端真实权限。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260728-user-list-access-role/database-schema-evidence.md
