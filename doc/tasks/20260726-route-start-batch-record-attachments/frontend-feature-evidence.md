# Frontend Feature Evidence

## Scope

- Component: `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- API wrapper: `IntRuoyiFronted/src/api/mes/pro/route/flowconfig.ts`

## Contract

- “批记录附件”只在“工序开始”边界节点左侧固定出现。
- 点击“工序开始”默认选中“批记录附件”，右侧展示 4 个固定负责人配置项。
- 每项显示默认上传角色名称，并支持个人/权限角色候选选择。
- 初始化和保存按钮只在草稿候选版本可编辑；非草稿或无路线版本时禁用。

## Acceptance

- 工序开始节点出现“批记录附件”固定入口。
- 工序结束节点不出现“批记录附件”入口。
- 右侧明细展示 4 项记录/报告和对应默认上传角色。
- 前端 API 暴露读取、初始化和保存批记录附件负责人配置方法。

## BDD

- BDD: 工序开始批记录附件入口 -> Given 用户选中“工序开始”，When 查看左侧固定页签，Then 可看到并打开“批记录附件”。
- BDD: 四项附件负责人配置 -> Given 用户打开“批记录附件”，When 查看右侧配置，Then 看到来料检报告、灭菌报告、成品检报告、成品检记录 4 项。
- BDD: 工序结束隔离 -> Given 用户选中“工序结束”，When 查看左侧固定页签，Then 不出现“批记录附件”。

## Verification

- RED: `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> FAIL，API wrapper 与组件入口/明细区缺失。
- GREEN: `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-end-release-owner-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-release-owner-candidate-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-end-release-owner-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-release-owner-candidate-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Non-Goals

- 未新增页面路由或菜单。
- 未修改“工序结束”放行责任人业务规则。

## Blockers

- 无前端实现 blocker。
- 未做真实页面 E2E：本任务按静态合同和类型检查验证，未启动本地前后端服务。
