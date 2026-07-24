# 权限角色分配人数展示执行日志

BDD: 角色列表展示分配人数 -> Given 角色 A 已分配给 2 个用户且角色 B 未分配用户 When 管理员打开权限角色页 Then 列表中角色 A 显示分配人数 2，角色 B 显示 0。

BDD: 角色详情和导出保持同一人数口径 -> Given 角色已分配用户 When 管理员查看角色详情或导出角色 Excel Then `assignedUserCount` 使用与分页相同的 `system_user_role` 聚合口径。

RED: 待执行 -> FAIL, 当前角色响应缺少 `assignedUserCount` 字段，前端角色表格也没有“分配人数”列。

GREEN: mvn.cmd -pl yudao-module-system -Dtest=RoleServiceImplTest test -> PASS, 23 个角色服务测试通过，新增 `testGetAssignedUserCountMap` 覆盖已分配角色、未分配角色和非查询角色隔离。

GREEN: node tests/e2e/system-role-category-static.spec.js -> PASS, 前端角色静态契约确认 API 类型暴露 `assignedUserCount` 且角色表格展示“分配人数”列。

GREEN: pnpm.cmd exec eslint src/api/system/role/index.ts src/views/system/role/index.vue tests/e2e/system-role-category-static.spec.js -> PASS, 目标前端类型、页面和静态测试无 ESLint 问题。

## 收尾

- 状态：completed
- 后端提交范围：角色响应、用户角色聚合 Mapper、角色服务聚合方法、控制器填充逻辑和后端单元测试。
- 前端提交范围：角色 API 类型、角色表格分配人数列和静态契约测试。

GREEN: mvn.cmd -pl yudao-server -am -DskipTests package -> PASS, 本机后端新 jar 构建成功。

GREEN: experience-preflight -> PASS, 使用系统 Chrome 执行官方登录预检进入本机 `http://localhost:8081/system/role`，租户 `测试租户`，账号 `aoteman`，路径验证通过。

GREEN: role-page-assigned-user-count-runtime -> PASS, 真实登录后监听 `/admin-api/system/role/page` 返回 HTTP 200、业务 `code=0`；首条角色 `assignedUserCount` 为 number，页面表头“分配人数”可见，未捕获 `系统异常`、`role/page` 或 `Uncaught` 控制台错误。

