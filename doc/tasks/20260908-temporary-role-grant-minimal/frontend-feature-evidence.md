# Frontend Feature Evidence

## Feature goal and non-goals

目标：提供“临时角色授权”最小可操作页面，让管理员完成申请、审批、撤销、查看状态与审计记录。

非目标：本次不做复杂审批流引擎、不做批量授权、不做 E2E 实测环境验收。

## Acceptance

- 清单 1.10：临时权限管理失控。
- 验收：前端存在完整入口页面与 API 调用，操作均受权限按钮控制，页面展示有效期、状态、原因与审计。

## UI entry, routes, components, and owned files

- 页面组件：`IntRuoyiFronted/src/views/system/temporary-role-grant/index.vue`
- API：`IntRuoyiFronted/src/api/system/temporaryRoleGrant/index.ts`
- 菜单由 SQL 迁移注册，组件路径为 `system/temporary-role-grant/index`。

## API contracts and data states

- 列表：`GET /system/temporary-role-grant/page`
- 创建：`POST /system/temporary-role-grant/create`
- 审批：`PUT /system/temporary-role-grant/approve`
- 撤销：`PUT /system/temporary-role-grant/revoke`
- 审计：`GET /system/temporary-role-grant/audit-list`
- 状态：`PENDING`、`ACTIVE`、`REVOKED`、`EXPIRED`。

## BDD scenarios

- BDD: 前端闭环 -> Given 管理员进入临时角色授权页面 When 创建、审批、撤销或查看 Then 页面能显示状态、有效期、原因和审计入口。

## RED and GREEN evidence

- RED: 前端静态合同测试创建前，页面/API/权限点缺失。
- GREEN: `node tests\e2e\system-temporary-role-grant-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive, accessibility, loading, empty, error, and permission checks

- 使用现有 `ContentWrap`、`Search`、`Dialog`、`Pagination` 和 Element Plus 表格/表单。
- 列表 loading、空数据由表格组件承接。
- 创建与撤销表单有必填校验。
- 按钮使用 `v-hasPermi` 控制创建、审批、撤销、审计查询。

## Verification

当前按用户规则未执行真实 E2E；本轮验证为静态合同测试与 TypeScript 检查。若后续当轮明确授权 E2E，应使用 Playwright 登录真实前端，以真实页面完成创建、审批、撤销和审计查看。

## Blockers and follow-up skills

无。
