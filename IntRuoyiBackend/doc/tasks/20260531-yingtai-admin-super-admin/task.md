# 任务：瑛泰医疗租户新增 admin 超级管理员账号

## 任务目标

- 在本机运行库 `127.0.0.1:23306/ruoyi-vue-pro` 中，为租户 `瑛泰医疗` 新增后台账号 `admin / admin123`。
- 账号权限需与 `芋道源码 / admin` 的有效超级管理员权限一致。
- 只改目标租户账号与角色绑定数据，不改代码，不复制业务数据，不修改 `芋道源码` 租户数据，不影响现有 `yingtai` 用户。

## 维护性与安全评估

- 本任务属于本机运行库账号授权变更，涉及高权限账号，必须先完成只读预检和 RED 证据。
- 采用最小数据写入：`system_users`、`system_role`、`system_user_role`，必要时只创建目标租户内缺失的源账号同名角色。
- 实测展厅产品管理按钮由前端角色 `showroom_publicity` 控制；为满足“与芋道源码/admin 权限一致”和按钮验证，目标 admin 最终同步源 admin 的有效角色代码：`common`、`showroom_publicity`、`super_admin`。
- 缺少租户、源账号、目标账号冲突、目标超级管理员角色重复、数据库不可连接或真实前端不可用时，必须 fail fast。
- 明文密码只作为用户要求的登录凭据输入，不在额外日志中扩散；数据库只保存 BCrypt 密文。

## 前序任务检查

- 已检查 `doc/tasks/20260525-tenant-yudao-to-yingtai-copy/task.md`，该全量租户复制任务未完成且保持阻塞。
- 已在该任务中记录本次请求为独立窄范围账号授权任务，不继续全量租户复制。

## BDD 场景

- BDD: 瑛泰医疗 admin 超级管理员登录 -> Given 瑛泰医疗租户存在 admin/admin123 When 通过真实前端登录 Then 登录成功且角色包含 super_admin
- BDD: 展厅产品管理按钮可见 -> Given 瑛泰医疗 admin 拥有与芋道源码/admin 一致的有效角色 When 打开展厅产品管理 Then 新增/导入/导出按钮可见
- BDD: 源租户和现有用户不受影响 -> Given 本次只改瑛泰医疗 admin 账号 When 授权完成 Then 芋道源码/admin 与瑛泰医疗/yingtai 仍可登录

## 里程碑

- [x] M1：建立任务记录，明确安全边界、BDD、验证和回滚策略。
- [x] M2：完成只读预检与 RED 登录/账号缺失证据。
- [x] M3：事务化写入目标租户 admin 账号与源账号角色绑定。
- [x] M4：完成 GREEN 与 REGRESSION 真实验证。
- [x] M5：记录最终证据，执行 task-closeout-cleanup 预览并提交任务文档。

## 预期验证

- RED：写入前确认 `瑛泰医疗/admin/admin123` 不可登录或目标租户无 `admin` 用户。
- GREEN：写入后通过真实前端登录 `瑛泰医疗 / admin / admin123`，权限接口返回 `common,super_admin,showroom_publicity`，展厅产品管理显示 `新增`、`导入`、`导出`。
- REGRESSION：`芋道源码/admin/admin123` 与 `瑛泰医疗/yingtai/admin123` 仍可登录。

## 回滚策略

- 若创建了目标租户 `admin` 用户，回滚时删除该用户对应 `system_user_role` 绑定，并将该 `system_users` 记录标记删除或物理删除。
- 若本任务创建了目标租户授权角色，且回滚时无其他用户依赖这些角色，则同步删除这些角色及其菜单绑定。
- 不回滚、不删除任何既有 `yingtai` 用户或源租户数据。

## 当前状态

状态：已完成。已在真实运行库 `127.0.0.1:23306/ruoyi-vue-pro` 为 `tenant_id=162` 创建 `admin` 用户 `id=910249`；同步源 `admin` 有效角色 `common`、`showroom_publicity`、`super_admin`，其中 `super_admin` 角色 `id=910216`。真实前端 `http://localhost:8081/showroom/product` 已验证 `新增`、`导入`、`导出` 可见；`芋道源码/admin` 与 `瑛泰医疗/yingtai` 回归登录通过。

## Current Status

completed. Final verification passed for target admin login, permission roles, product management buttons, source admin regression, and existing yingtai regression. Task-closeout-cleanup preview/apply removed only task helper/evidence artifacts and kept `task.md` plus `execution-log.md`.
