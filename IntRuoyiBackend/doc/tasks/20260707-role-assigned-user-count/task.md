# 权限角色分配人数展示

## 任务目标

- 在权限角色列表中展示每个角色已分配的用户人数。
- 分配人数由后端基于 `system_user_role` 正式聚合后返回，前端只展示接口字段。
- 不改变现有菜单权限、数据权限、角色分类和用户角色分配语义。

## 经验门禁

- PowerShell / Windows shell：已先读取 `docs/powershell-memory.md`；中文文本读写使用 UTF-8 aware 路径或 `apply_patch`，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；角色列表新增紧凑数字列，保持蓝/中性运维控制台风格。
- 真实 E2E：如进入真实浏览器验证，必须先读取 `docs/login-access.md` 并执行官方登录预检；本阶段先做源码、单元与静态验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。分配人数缺失时不由前端兜底猜算，后端统一返回数值。
- 是否从根因和长期维护角度解决：是。服务端基于角色用户关联表聚合，角色分页、详情、导出响应统一扩展。
- 是否存在临时补丁或绕过：否。

## 里程碑

- [x] M1：补齐 BDD 场景、RED 测试和任务执行日志。
- [x] M2：后端角色响应增加 `assignedUserCount`，并基于 `system_user_role` 聚合填充。
- [x] M3：前端角色 API 类型和角色表格新增“分配人数”列。
- [x] M4：执行后端定向测试、前端静态契约/类型验证，更新文档并准备提交本次相关改动。

## 预期验证

- 后端：角色列表响应每条角色包含准确的 `assignedUserCount`，无用户分配时为 0。
- 前端：权限角色表格显示“分配人数”列，展示后端返回的人数。
- 导出：角色 Excel 响应模型包含“分配人数”字段。

## 当前状态

- 状态：completed
- 已完成：后端基于 `system_user_role` 聚合角色分配人数，角色分页、详情和导出响应统一返回 `assignedUserCount`；前端角色表格新增“分配人数”列。
- 验证：`mvn.cmd -pl yudao-module-system -Dtest=RoleServiceImplTest test`、`node tests/e2e/system-role-category-static.spec.js`、`pnpm.cmd exec eslint src/api/system/role/index.ts src/views/system/role/index.vue tests/e2e/system-role-category-static.spec.js` 均通过。
- 阻塞：暂无本任务阻塞；前端全量类型检查仍被既有未触碰 `BatchRecordHistoryPage.vue closedAt` 类型问题阻塞。
