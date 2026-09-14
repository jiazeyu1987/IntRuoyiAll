# 工序池完整一线闭环 E2E 验证任务

## Task Goal

针对生产一线完整路径执行真实 Playwright E2E 验证：从设备账号进入报工入口，切换工艺路线、工序和实际员工，按员工 UI 模板提交生产/PQC 报工数据，落入报工、记录本和工序池，再验证 FIFO 分配、审核副本、原始记录修改限制和工序池时间线展示。

## Milestones

- [x] M1 建立任务门禁、BDD 场景、测试矩阵和适用经验摘录。
- [x] M2 核对当前前端/后端运行态、登录租户账号、菜单权限、页面入口和 E2E 脚本入口。
- [ ] M3 准备或确认任务自有测试数据：设备账号、员工、工艺路线、工序、模板、PQC、生产工单、计划开始时间和电子签名身份。
- [ ] M4 通过真实浏览器完成报工到工序池和 FIFO 的完整写入路径验证。
- [ ] M5 验证审核副本、原始记录修订限制、已分配数据不可改和时间线展示。
- [x] M6 形成 verification-report，记录 PASS/BLOCKED、数据清理结果和剩余缺口。

## Expected Verification

- `pnpm run test:e2e <full-chain-spec>` 在 `IntRuoyiFronted` 下操作真实前端页面并通过。
- E2E 必须记录前端 URL、后端 URL、租户/账号标签、任务自有数据标识、关键页面断言、关键 API/DB 只读核验和清理方式。
- 不允许用 API-only、静态合同、mock、历史固定数据或直接 SQL 写入代替真实前端路径。
- 如果缺少测试租户、设备账号绑定、员工绑定、电子签名、生产工单、菜单权限、运行态或页面入口，必须记录为 BLOCKED。

## Applicable Gates

- E2E 脚本入口存在性门禁：运行前核对 `package.json` 脚本、spec 文件、真实页面入口、route、权限 meta、页面按钮和写 API wrapper。
- Worktree / int_main 运行态 URL 门禁：本任务默认只使用 `E:\IntRuoyi` 的 `int_main` 本机 `8081/48081`，前后端 URL 必须成对。
- 官方登录前置门禁：登录只使用本机授权入口和默认本机身份标签，不记录密码，不在 admin 基线数据上无授权写入。
- 全链路 E2E 阶段归因门禁：若完整链条某一阶段失败，必须记录失败阶段，不把前一阶段通过冒充全链通过。
- 工序池验收门禁：工序池必须是独立模型，不复用余量/资源池；FIFO 只按生产工单计划开始时间；原始记录和审核副本都必须保留。

## Current Status

blocked

## Blocker

完整路径真实 E2E 目前被前端入口阻塞：

- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` 只提供固定模板解析和 payload 校验，没有调用 `/mes/pro/feedback/frontline/submit`。
- `IntRuoyiFronted/src/views/mes/pro/feedback/index.vue` 没有接入 `loadFrontlineDeviceProcesses`、`switchFrontlineActualEmployee` 或 `frontlineSubmit`。
- `IntRuoyiFronted/src/router/modules/remaining.ts` 未登记 `TimelinePage.vue` 对应的 `/mes/pro/process-pool/timeline` 页面路由；当前只登记了审核副本和原始记录修改两个页面。
- 因此无法通过真实浏览器完成“报工入口提交 -> 工序池 -> FIFO -> 时间线查看”的正式用户路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只验证正式页面、正式接口和正式工序池链路。
- `是否存在临时补丁或绕过`：否。
