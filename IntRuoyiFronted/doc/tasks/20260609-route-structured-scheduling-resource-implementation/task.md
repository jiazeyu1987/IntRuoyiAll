# 工艺路线结构化排产资源实现（前端）

## 任务目标

在 `paichan_new` 前端 worktree 中实现 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260609-route-structured-scheduling-resource-requirements\requirements.md` 中与前端相关的第一期能力：将排产员主入口收敛到工艺路线详情下，按工序展示标准资源、今日可用资源、标准班次产能、今日班次产能、状态原因和排产资源详情；保留资源大表作为全局数据治理入口。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260608-route-process-shift-capacity-display/task.md`。
- 检查结果：该任务在当前 `int_main` 基线已提交，当前 worktree 从该提交创建；本任务在其班次产能展示基础上重构为结构化排产资源视图。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口错误、缺少设备明细、产能冲突等继续显式暴露，不显示假成功。
- `是否从根因和长期维护角度解决`：是。前端只改变工艺路线下的结构化入口和展示，底层仍使用现有资源接口和工艺路线接口。
- `是否存在临时补丁或绕过`：否。不新增测试控件，不使用 mock 数据，不复制资源大表成为第二套系统。

## BDD 场景

- BDD: 工艺路线下显示结构化排产资源表 -> Given 排产员打开工艺路线详情 / When 查看组成工序 / Then 表格显示资源类型、标准资源、今日可用、标准班次产能、今日班次产能和状态。
- BDD: 点击工序排产资源查看详情 -> Given 某工序有设备或人工资源 / When 点击排产资源 / Then 设备工序显示设备明细和维修影响，人工工序显示单人产能、人数和班次小时。
- BDD: 资源大表保留但不作为主入口 -> Given 用户进入工艺路线列表页 / When 切换视图 / Then 资源大表仍可打开，但工艺路线详情提供更清晰的结构化排产资源入口。

## 里程碑

- [x] M1：补充前端静态契约 RED 测试。
- [x] M2：扩展 API 类型并重构 `RouteProcessList.vue`。
- [x] M3：补充资源详情交互。
- [x] M4：运行类型检查和静态契约测试。
- [x] M5：使用真实数据执行 Playwright E2E。

## 预期验证

- `node tests\e2e\mes-route-structured-scheduling-resource.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 使用真实登录和真实数据打开 `/mes/pro/route?openId=900026` 或实际可用路线，验证结构化排产资源字段与详情。

## 当前状态

completed

## 完成记录

- `RouteProcessList.vue` 的组成工序表格已显示资源类型、标准资源、今日可用、标准班次产能、今日班次产能和资源状态。
- 设备工序详情显示设备列表、今日可用、单台产能、今日班次产能、设备状态，以及标准/今日总产能汇总。
- 人工工序详情显示单人产能/h、标准人数、今日人数、今日总产能/h、今日总产能/班次和班次小时。
- 资源大表入口未删除，继续作为全局数据治理入口；本任务未新增重复排产页面。

## 最终验证

- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS。
- `node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> PASS。
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8084 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin` 并打开 `/mes/pro/route?openId=900026` 验证。
- 融入 `int_main` 后，主目录前端 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 以 `8085` 启动并代理到主目录后端 `48081`，执行 `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8085 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS。
