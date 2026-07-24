# 任务：DCC 基础数据页新增产品目录页签

## 任务目标

- 将 `src/views/dcc/controlled-file/basic-data/index.vue` 重构为同一路由下的双页签壳：
  - `项目代码`：保留现有功能与 `projectCodeId` 抽屉行为。
  - `产品目录`：新增只读表格、基础筛选、分页和刷新。
- 页签状态使用路由 query `tab=project-code|product-catalog` 同步；默认 `project-code`。
- 切到 `product-catalog` 时必须移除 `projectCodeId` query 并关闭项目代码详情抽屉。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-bu-select-restriction\task.md`
- 状态：`COMPLETED`
- 处理：上一前端任务已收口，不阻塞本次 DCC 基础数据页签改造。
- 用户计划中指定的旧 DCC 任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260623-dcc-browser-batch-recognition\task.md` 当前已明确为 `BLOCKED`，允许本次新任务启动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁：
  - 双页签页面沿用当前 DCC / IntPP 操作台样式，不做与本需求无关的视觉重构。
  - 前端不得用 mock、placeholder、fallback、静默 catch 或空表兜底掩盖后端真实错误。
  - 若执行真实登录验收，必须先跑官方 `login-preflight.mjs`，并在高风险动作前记录 `experience-preflight` 结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。产品目录页签直接消费真实接口错误，不提供前端兜底数据。
- `是否从根因和长期维护角度解决`：是。把 `basic-data/index.vue` 拆为页签壳与子面板组件，避免继续堆大单文件。
- `是否存在临时补丁或绕过`：否。不会通过隐藏 query、保留失效抽屉状态或写死表格样例绕过正式实现。

## BDD 场景

- `BDD: 默认进入项目代码页签 -> Given 用户首次访问基础数据页面 / When 页面加载完成 / Then active tab 为 project-code，原项目代码主表与导入入口仍可见。`
- `BDD: 产品目录页签展示基础筛选和表格列 -> Given 用户切换到 product-catalog / When 页面渲染 / Then 展示关键词、产品类别 I、产品类别 II、产品状态、数据来源筛选项，以及计划内表格列和刷新按钮。`
- `BDD: 切页签时清理项目代码详情状态 -> Given 当前 URL 带 projectCodeId 且详情抽屉已打开 / When 用户切到 product-catalog / Then URL 不再保留 projectCodeId，详情抽屉关闭。`
- `BDD: 注册证链接按只读链接按钮渲染 -> Given 产品目录行含注册证信息链接 / When 表格渲染 / Then 该列展示可点击链接按钮；无值时显示 -。`

## 里程碑

1. M1：建立前端任务台账并补旧 DCC 任务状态。`COMPLETED`
2. M2：新增 RED 静态契约，锁定双页签、query 同步和产品目录列。`COMPLETED`
3. M3：实现页签壳、子面板组件与新 API 接线。`COMPLETED`
4. M4：运行静态契约、类型检查和真实浏览器验收并补齐证据。`COMPLETED`

## 最终验证结果

- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\frontend-feature-evidence.md` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/basic-data?tab=product-catalog --target-text 产品目录` -> PASS
- Playwright 真实只读验收 -> PASS，产品目录页签显示 `共 213 条`，关键词 `导管鞘组（大腔鞘）` 查询后返回 `共 1 条`。

## 预期验证

- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/task.md`
- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/execution-log.md`
- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/frontend-feature-evidence.md`
