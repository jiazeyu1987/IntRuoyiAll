# 任务：排程日历指标点击弹框 前端实现

## 任务目标

- 在排程日历月视图中，将“任务 / 工单 / 短缺”改成可点击明细入口。
- 将“白班 / 夜班”保持为只读展示，不在该区域触发明细弹框或新增设置。
- 保持夜班仍由工艺排产路线配置，排程日历只负责展示与说明。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-smart-scheduling-four-route-defaults\task.md`
- 状态：`COMPLETED`
- 处理说明：上一前端任务已完成，本次为新的排程日历交互修复。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 必须先写 RED 测试，再做最小前端实现。
  - 真实 Playwright 或登录验证前，必须先写入 `GREEN: experience-preflight -> PASS` 并先跑官方登录预检。
  - 页面交互修复不得靠隐藏点击、吞异常或兼容分支掩盖错误行为。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。重构月格交互结构，让点击语义与展示语义保持一致。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 月格任务指标打开任务详情 -> Given 某日期存在任务统计 / When 点击该日期“任务”指标 / Then 页面应打开该日期任务详情弹框。`
- `BDD: 月格工单指标打开工单详情 -> Given 某日期存在工单统计 / When 点击该日期“工单”指标 / Then 页面应打开该日期工单详情弹框。`
- `BDD: 月格短缺指标打开短缺详情 -> Given 某日期存在短缺统计 / When 点击该日期“短缺”指标 / Then 页面应打开该日期短缺明细弹框。`
- `BDD: 白班夜班区域只读 -> Given 月格展示白班和夜班数量 / When 点击白班或夜班区域 / Then 页面不应打开详情弹框，并继续保留“夜班由工艺排产路线配置”的说明。`

## 里程碑

1. M1：建立前端任务台账并定位月格交互代码。`COMPLETED`
2. M2：补 RED 静态测试。`COMPLETED`
3. M3：实现月格指标点击修复。`COMPLETED`
4. M4：完成静态验证与真实只读复验。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-tabs-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-calendar --target-text 返回排产 --timeout 90000`

## 当前阻塞

- 无。

## 完成结果

- 月格“任务 / 工单 / 短缺”已具备独立点击入口，复用现有日详情与短缺明细弹框，不新增接口。
- 月格外层已改为可聚焦容器，避免内部详情按钮与整格班次编辑发生语义冲突。
- 白班 / 夜班区域保持只读展示，真实只读验证确认点击后不会弹出详情对话框。
