# 任务：工艺排产路线补齐遗漏的排产内容

## 任务目标

- 修复 `工艺排产路线` 页面在职责拆分后遗漏原 `工艺路线` 中排产相关内容的问题。
- 保持基础 `工艺路线` 只承载基础路线定义，不把基础 CRUD、导入导出和非排产内容重新混回专用页。
- 仅把属于排产维护链路、且应由 `工艺排产路线` 承接的用户可见内容补齐到位。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-fit-width-clipping-round2\task.md`
- 状态：`BLOCKED`
- 处理说明：用户已切换到 `工艺排产路线` 缺项修复；上一任务已显式挂起，不与本轮前端修改混做。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 当前页面属于 MES 运维型列表与配置页，表格密度、按钮样式和工作面布局必须保持 IntPP 风格。
  - PowerShell 读取、记录和比对中文内容时必须显式使用 UTF-8。
  - 本轮先做源码与静态契约回归；如需真实 E2E，再单独补 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过补齐 `工艺排产路线` 应承接的排产维护入口与展示内容，避免职责拆分后长期残缺。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工艺排产路线承接排产维护缺失内容 -> Given 用户进入工艺排产路线列表 / When 查看路线列表与操作入口 / Then 页面展示拆分后仍应由排产专用页承接的排产相关内容，不要求用户回到基础工艺路线补做。`
- `BDD: 基础工艺路线职责边界不回退 -> Given 用户进入基础工艺路线页 / When 查看列表与详情入口 / Then 仍不重新出现已收口的排产用途、批记录用途和资源维护大表写入口。`
- `BDD: 工艺路线详情仍可看到设备与资源信息 -> Given 用户从基础工艺路线打开某条路线详情 / When 查找该路线对应的设备与资源信息 / Then 用户可直接在组成工序主表查看资源类型、标准资源、标准班次产能、资源状态与工作站，不要求额外切换页签。`
- `BDD: 组成工序表格继续显示资源与工作站数据 -> Given 用户打开基础工艺路线详情的组成工序表格 / When 查看每道工序的资源配置 / Then 表格内继续显示资源类型、标准资源、标准班次产能、资源状态与工作站，而不是只剩精简工序字段。`

## 里程碑

1. M1：补任务文档并用静态 RED 锁定当前缺口。
2. M2：最小化补齐 `工艺排产路线` 漏掉的排产相关内容。
3. M3：跑静态契约并回填执行记录。

## 预期验证

- `node tests/e2e/mes-route-equipment-visibility-static.spec.js`
- `node tests/e2e/mes-schedule-route-missing-scheduling-content-static.spec.js`
- `node tests/e2e/mes-process-use-route-tabs-static.spec.js`
- `node scripts/mes-route-responsibility-split-static.test.mjs`
- `node tests/e2e/mes-route-structured-scheduling-resource-static.spec.js`
- `node tests/e2e/mes-pro-route-process-machinery-column.spec.js`
- `node tests/e2e/mes-pro-route-process-shift-capacity-display.spec.js`
- `node tests/e2e/mes-route-process-hide-wait-color-columns.spec.js`
- `node tests/e2e/mes-route-process-remove-today-columns.spec.js`
- `node tests/e2e/mes-route-process-shortage-inline-ratio.spec.js`

## 最终验证结果

- `node tests/e2e/mes-route-equipment-visibility-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-route-missing-scheduling-content-static.spec.js` -> PASS
- `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> PASS
- `node scripts/mes-route-responsibility-split-static.test.mjs` -> PASS
- `node tests/e2e/mes-route-structured-scheduling-resource-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-route-process-machinery-column.spec.js` -> PASS
- `node tests/e2e/mes-pro-route-process-shift-capacity-display.spec.js` -> PASS
- `node tests/e2e/mes-route-process-hide-wait-color-columns.spec.js` -> PASS
- `node tests/e2e/mes-route-process-remove-today-columns.spec.js` -> PASS
- `node tests/e2e/mes-route-process-shortage-inline-ratio.spec.js` -> PASS

## 完成记录

- `工艺排产路线` 已补回原基础路线中与排产维护直接相关、但职责拆分后遗漏的状态筛选/状态展示能力。
- `工艺排产路线` 已补回复制路线入口和复制弹窗，直接复用既有 `ProRouteApi.copyRoute`。
- 本次未把基础 `工艺路线` 的新增、导入、导出、删除等基础 CRUD 重新混回专用页，仍保持职责边界。
- `基础工艺路线详情` 未新增额外 `设备信息` 页签；设备与资源信息继续收敛在 `组成工序` 主表中展示，避免职责边界再次漂移。
- `RouteProcessList.vue` 已恢复为完整工序资源版，重新在主表展示 `资源类型 / 标准资源 / 标准班次产能 / 资源状态 / 工作站`，并保留设备详情与人工产能编辑链路。
