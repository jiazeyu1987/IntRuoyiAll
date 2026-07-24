# Execution Log: 瑛泰医疗租户新增 admin 超级管理员账号

BDD: 瑛泰医疗 admin 超级管理员登录 -> Given 瑛泰医疗租户存在 admin/admin123 When 通过真实前端登录 Then 登录成功且角色包含 super_admin

BDD: 展厅产品管理按钮可见 -> Given 瑛泰医疗 admin 拥有与芋道源码/admin 一致的有效角色 When 打开展厅产品管理 Then 新增/导入/导出按钮可见

BDD: 源租户和现有用户不受影响 -> Given 本次只改瑛泰医疗 admin 账号 When 授权完成 Then 芋道源码/admin 与瑛泰医疗/yingtai 仍可登录

GREEN: 只读 SQL 预检 -> PASS，`system_tenant` 中 `芋道源码=1`、`瑛泰医疗=162` 均存在且未删除；`芋道源码/admin` 为启用用户并绑定 `common,showroom_publicity,super_admin`；`瑛泰医疗` 仅查到启用用户 `yingtai`，未查到未删除的 `admin`；目标租户未查到 `super_admin` 角色。

GREEN: 源租户登录预检 -> PASS，`芋道源码 / admin / admin123` 通过 `/admin-api/system/auth/login` 登录成功，返回 `userId=1`。

RED: 目标租户 admin 登录预检 -> FAIL，`瑛泰医疗 / admin / admin123` 调用真实登录接口返回业务失败，符合目标租户缺少 `admin` 用户的预期。

GREEN: 目标租户既有用户回归预检 -> PASS，`瑛泰医疗 / yingtai / admin123` 登录成功，返回 `userId=910201`。

NOTE: 端口预检发现 `127.0.0.1:23306` 由本机 `ssh.exe` 转发占用，后端实际读取该运行库；早期写入 Docker 容器库的同名任务数据不是目标库，已按本任务创建的用户/角色 ID 清理，避免非目标副作用。

GREEN: 真实运行库 JDBC 预检 -> PASS，`RuntimeDbAdminGrant precheck` 连接 `127.0.0.1:23306/ruoyi-vue-pro`，确认 `芋道源码=1`、`瑛泰医疗=162`，源 `admin` 绑定 `super_admin`，目标租户未存在未删除 `admin`，目标缺少有效 `super_admin`。

GREEN: 真实运行库事务写入 -> PASS，`RuntimeDbAdminGrant apply` 创建目标 `admin` 用户 `id=910249`，创建目标 `super_admin` 角色 `id=910216` 并完成绑定。

GREEN: 源账号角色同步 -> PASS，产品管理按钮实际依赖前端角色 `showroom_publicity`；`RuntimeDbAdminGrant sync-source-roles` 同步源 `admin` 有效角色代码 `common,showroom_publicity,super_admin`，新增目标角色 2 个、新增绑定 2 个。

GREEN: 写入后数据库校验 -> PASS，`RuntimeDbAdminGrant verify` 返回 `adminUserId=910249`，角色为 `common,showroom_publicity,super_admin`；`yingtai` 仍为 `tenant_admin`。

GREEN: 权限接口验证 -> PASS，`瑛泰医疗 / admin / admin123` 登录成功，`/admin-api/system/auth/get-permission-info` 返回 `roles=common,super_admin,showroom_publicity`，权限数 `779`，菜单数 `10`。

GREEN: 真实前端产品按钮验证 -> PASS，`node doc/tasks/20260531-yingtai-admin-super-admin/verify-yingtai-admin-showroom-buttons.cjs` 通过真实登录页登录 `瑛泰医疗 / admin / admin123` 并进入 `http://localhost:8081/showroom/product`，确认 `新增`、`导入`、`导出` 按钮可见。

GREEN: 回归登录验证 -> PASS，`芋道源码 / admin / admin123` 仍可登录；`瑛泰医疗 / yingtai / admin123` 仍可登录，用户 `id=910201` 未删除、未改密、未重绑。

GREEN: 任务收尾清理 -> PASS，`task-closeout-cleanup --mode preview` 仅识别本任务 helper/evidence 附属产物；`--mode apply` 已删除附属 Java/CJS helper 与临时 evidence 文件，保留 `task.md` 与 `execution-log.md`。
