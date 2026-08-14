# 工艺路线编辑器栈溢出复现

## Task Goal

复现并修复用户反馈的工艺路线进入编辑器时前端异常：每次进入编辑器新增 2 条 `RangeError: Maximum call stack size exceeded`，来源 `form-designer-3YqQ_Q1F.js`，同时出现 6 条重复“没有该操作权限”提示和 1 条自动布局提示。

## Milestones

- [x] 建立只读复现任务记录并读取适用门禁
- [x] 确认本机前端、后端、登录与浏览器前置条件
- [x] 通过真实前端路径进入工艺路线编辑器并采集 console/page/network 证据
- [x] 汇总复现结论、阻塞项与后续修复建议
- [x] 按用户追加要求在测试服务器真实前端复跑同类证据采集
- [x] 定位测试服工艺路线编辑页加载 `form-designer` chunk 的根因
- [x] 先补 RED 静态合同，再移除全局 FcDesigner 安装并保留设计器页面局部注册
- [x] 完成本机修复后 route/BPM 真实页面复验和 TypeScript 检查
- [x] 通过 bug-regression 与 frontend-feature evidence validator

## Expected Verification

- 使用本机 `http://127.0.0.1:8081` 或 `http://localhost:8081` 的真实前端页面。
- 使用测试服务器 `http://172.30.30.58:8081` 的真实前端页面，仅执行只读复现，不发布、不重启、不改远端数据。
- 通过 Playwright 真实登录默认本机身份 `芋道源码/admin`，不记录密码或 token。
- 采集进入工艺路线编辑器前后的 `pageerror`、`console error`、重复权限提示、自动布局提示、目标网络请求和截图/JSON 证据。
- 静态合同必须先 RED 后 GREEN，证明 `setupFormCreate(app)` 不再全局 import/install `@form-create/designer`，BPM/Infra 设计器页面仍局部 import。
- 运行 `pnpm ts:check`，并复验本机工艺路线编辑页与 BPM 表单设计器均无新增 `RangeError`。
- evidence 文件必须通过对应 skill validator，并将关键结论归档到保留的任务日志和验证报告。
- 若前置条件缺失，记录准确 blocker 与影响，不使用 API-only、mock 或静态扫描替代真实复现。

## Current Status

completed

本地已完成根因修复：`@form-create/designer` 不再由全局 `setupFormCreate(app)` 安装，改为仅在 BPM/Infra 真实设计器页面局部 import。静态合同、`pnpm ts:check`、本机 route/BPM 真实页面复验和 evidence validator 均通过；cleanup apply 已清理临时截图、JSON 和临时 evidence，并保留复现脚本。测试服需部署此前端改动后再复测 `pageRangeErrorCount=0`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从全局插件注册边界移除重型设计器，避免非设计器页面加载并触发 `form-designer` chunk。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `docs/e2e-rules.md#基本规则`：复现必须使用真实前端页面，API 只能用于只读辅助或最终核验。
- `docs/e2e-rules.md#playwright-浏览器可执行文件门禁`：若 Playwright 缓存浏览器缺失，先使用本机 Chrome/Edge 显式路径并记录来源。
- `docs/e2e-rules.md#playwright-目标链路与外部资源异常归因门禁`：采集 console、pageerror、requestfailed 和非 2xx 响应时必须区分目标链路与非目标资源。
- `docs/e2e-rules.md#vite-动态导入-500-与冲突标记门禁`：若进入编辑器时出现动态导入失败或 Vite 500，先定位模块和源码冲突，不把编译失败误判为业务缺陷。
- `docs/e2e-rules.md#worktree--int_main-运行态-url-门禁`：本轮主工作区复现只使用 `int_main` 的 `8081/48081` 成对 URL，先确认端口归属。
- `docs/frontend-development.md#实施规则`：后端、权限或前端请求失败必须在 UI、网络、控制台或测试证据中明确暴露，不做吞错或默认成功判断。
- `docs/frontend-development.md#前端重型设计器全局注册隔离门禁`：`@form-create/designer` 等只服务专用页面的重型设计器不得在应用全局插件中安装。
- `docs/server-access.md#测试服务器`：测试服前端为 `http://172.30.30.58:8081/`，后端健康检查为 `http://172.30.30.58:48081/actuator/health`；本轮仅访问测试服，不执行 SSH、发布或重启。
- `docs/login-access.md#环境门禁`：访问测试服务器前需当前任务明确授权；本轮授权来自用户“测试服务器可以复现吗”，账号来源使用既有默认登录来源并脱敏。

## Reproduction Result

- 工艺路线编辑页：两次进入 `http://127.0.0.1:8081/mes/pro/route/edit/980091?tab=flow`，路线 `RT000028-IDI / 按压式球囊扩充压力泵`；`RangeError`、`pageerror`、权限响应、权限 toast、自动布局 toast 均为 0。
- form-create 设计器页：两次进入 `http://127.0.0.1:8081/bpm/manager/form/edit`；同样未出现 `RangeError`、权限 toast 或自动布局 toast。
- 工艺路线编辑页每次进入新增 6 条 warning，其中包括 Vue instance key 枚举 warning 和 VueFlow `Edge source or target is missing`；这些 warning 与用户反馈的 `form-designer-3YqQ_Q1F.js` 栈溢出不是同一类错误。
- 源码核对：工艺路线流转关系图使用 `RouteFlowGraphDesigner.vue` 和 VueFlow；`@form-create/designer` 主要出现在 BPM/Infra 表单设计器入口。
- 测试服工艺路线编辑页：两次进入 `http://172.30.30.58:8081/mes/pro/route/edit/922119?tab=flow`，路线 `RT000028 / 球囊扩张压力泵`；`pageRangeErrorCount=4`，每次进入后新增 2 条 `RangeError`，stack 指向 `assets/form-designer-3YqQ_Q1F.js`。
- 测试服 BPM form-create 设计器页：两次进入 `http://172.30.30.58:8081/bpm/manager/form/edit`；`pageRangeErrorCount=0`，未复现栈溢出。
- 测试服本轮未捕获到“没有该操作权限”接口响应或 UI toast，也未捕获到自动布局 toast。

## Fix Result

- 根因：`IntRuoyiFronted/src/plugins/formCreate/index.ts` 全局 import/install `@form-create/designer`，导致 MES 工艺路线这类非设计器页面也加载生产构建中的 `form-designer` chunk。
- 修复：移除全局 FcDesigner import/install；`IntRuoyiFronted/src/views/bpm/form/editor/index.vue` 与 `IntRuoyiFronted/src/views/infra/build/index.vue` 保持局部 `import FcDesigner from '@form-create/designer'`。
- RED/GREEN：`node tests\e2e\mes-route-form-designer-global-import-static.spec.cjs` 先失败于全局 FcDesigner 安装，修复后通过。
- 本机复验：route 编辑页 `pageRangeErrorCount=0`；BPM 表单设计器 `pageRangeErrorCount=0`。
- 未完成项：`pnpm build:local` 曾长时间运行无失败输出后被手动中断，未作为通过证据；测试服尚未部署本地修复。

## Cleanup Keep

- doc/tasks/20260808-process-route-editor-stack-overflow-repro/process-route-editor-repro.e2e.cjs
