# 任务：排产工单可选列导出

## 任务目标

在排产工单列表新增“导出”按钮和列选择弹窗，默认导出当前主表可见业务列，允许用户取消或选择导出列；导出请求携带当前筛选条件和 `exportColumns`。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：中文文件读写与脚本验证必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：新增入口保持密集运营列表工具栏风格，不做无关重设计。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、API、权限和错误状态。
- 当前目标文件已有其它未提交排产改动；本任务只在其基础上叠加导出能力，不回退既有改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；前端维护唯一列配置，默认列与主表可见列保持同源。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 默认可见列导出 -> Given 用户打开导出弹窗 / When 未调整列直接导出 / Then 请求携带默认可见业务列。
- BDD: 自定义列导出 -> Given 用户取消部分列 / When 确认导出 / Then 请求只携带选中列。
- BDD: 空列阻止导出 -> Given 用户取消所有列 / When 点击确认导出 / Then 前端给出明确提示且不请求后端。

## 验证结果

- RED: `node tests/e2e/mes-pro-schedule-order-export-columns-static.spec.js` -> FAIL，导出按钮权限、弹窗状态、默认列、API 路径和下载调用尚不存在。
- GREEN: `node tests/e2e/mes-pro-schedule-order-export-columns-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check:schedule` -> PASS。
- GREEN: `node doc/tasks/20260708-schedule-order-export-columns/run-schedule-order-export-real-e2e.mjs` -> PASS；真实登录测试租户 `aoteman`，导出并下载 `排产工单.xls`，表头与默认可见业务列一致，工作表行数证据 37。

## 当前状态

`COMPLETED_WITH_BLOCKERS`：前端导出入口、列弹窗、空选择提示、API 下载链路、静态回归、类型检查和真实数据 E2E 已完成；最终提交仍受共享脏改与后端目标单测阻塞。
