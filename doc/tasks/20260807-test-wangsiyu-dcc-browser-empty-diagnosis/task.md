# 测试服 wangsiyu 受控浏览空列表诊断

## Task Goal

定位测试服 `wangsiyu` 已能进入文控中心后，在 `文控中心 > 受控浏览` 选择目录 `作废保留` 时仍显示“无权限或无匹配当前有效文件”的根因。

## Milestones

- [x] 读取服务器、登录、数据库、E2E、PowerShell 编码和权限审查规则。
- [x] 建立本次只读诊断记录。
- [x] 核对后端受控浏览权限逻辑。
- [x] 只读核对测试服目录、ACTIVE 文件、当前有效版本和用户查看矩阵命中。
- [x] 输出根因和最小修复建议。
- [x] 将“菜单入口不等于文件可见权限”的可复用门禁合并到现有 `docs/e2e-rules.md`。

## Expected Verification

- 只读 SQL 覆盖 `dcc_file_directory`、`dcc_controlled_file`、`dcc_controlled_file_master`、`dcc_category_view_matrix_rule`、`system_users`、`system_user_role`。
- 不修改测试服 MySQL、Redis、代码或运行态。
- 若需要修复，先明确是补查看矩阵/目录文件迁移/筛选条件，而不是继续扩大菜单角色。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按受控浏览正式权限链定位。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed
