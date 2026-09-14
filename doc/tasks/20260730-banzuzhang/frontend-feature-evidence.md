# F9/F10 Frontend Feature Evidence

## Feature

F9/F10 前端新增班组长工作台页面，覆盖：

- 提交看板：按 `PRODUCTION/PQC` 班组长类型查看负责员工提交。
- 提交详情：展示原始 `originalPayloadJson`。
- 提交复核：提交复核状态和说明。
- 异常上报：按生产工单 ID 标记并上报异常。
- 班组维护：添加/禁用员工、维护不良原因、维护设备参数上下限。

## Acceptance

- Route: `pro/process-pool/team-leader`
- Component: `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- API wrapper: `src/api/mes/pro/processpool/teamLeader.ts`
- Permission: `mes:pro-process-pool-team-leader:query`
- 前端请求类型不得接受 `leaderUserId`，班组长身份由后端登录态注入。
- 页面不得使用 `ignoreErrorMessage: true` 隐藏后端错误。

## BDD

- BDD: 班组长查看提交看板 -> Given 生产或 PQC 班组长进入工作台 / When 选择班组长类型并查询 / Then 页面调用提交分页接口并展示提交列表。
- BDD: 班组长复核提交 -> Given 提交详情已经打开 / When 输入复核状态和说明并确认 / Then 页面调用复核接口，不改写原始 payload。
- BDD: 班组长异常上报 -> Given 班组长知道目标生产工单 / When 填写异常原因编码和说明 / Then 页面调用生产工单异常上报接口。
- BDD: 班组长维护设备参数 -> Given 班组长维护负责工序 / When 填写设备和参数上下限 / Then 页面调用设备参数规则接口，并保留上下限字段。

## RED

- RED: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> FAIL，expected reason: API wrapper、页面组件和路由入口缺失。

## GREEN

- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS，输出 `mes-process-pool-team-leader-static PASS`。
- GREEN: `pnpm ts:check` -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 无错误输出。
- GREEN: 真实 Playwright 登录前置使用本机 Chrome 显式路径访问 `http://127.0.0.1:8098/mes/pro/process-pool/team-leader` -> PASS，页面展示 `工序池班组长工作台`。
- GREEN: 真实 Playwright 页面冒烟切换 `提交看板/异常上报/班组维护/PQC 班组长` -> PASS，提交看板 API HTTP 200 且业务 `code=0`，无控制台 error。

## Verification

- 静态合同检查所有 F9/F10 接口路径均存在。
- 静态合同检查页面调用提交查询、详情、复核、异常上报、员工绑定、员工禁用、不良原因、设备参数规则 API。
- 静态合同检查页面包含提交看板、异常上报、班组维护三个 tab。
- 静态合同检查页面支持 `PRODUCTION/PQC` 类型。
- TypeScript 检查通过。
- 真实浏览器只读冒烟通过，截图：`output\playwright\20260730-banzuzhang\team-leader-workbench-smoke.png`。

## Responsive and UX Checks

- 页面沿用 Element Plus 表单、表格、抽屉、弹窗和 tabs 模式。
- 提供 loading、empty、错误透传、详情抽屉和维护表单。
- 已完成真实浏览器截图验证，目标工作台首屏和 tab 结构可渲染。

## Blockers

- 写入型真实 E2E 仍需测试租户、班组长账号、生产工单、负责员工、工序和设备参数样本数据；本次已完成只读页面入口、tab 切换和提交看板接口冒烟。
