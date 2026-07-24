# eDHR 批次执行前端执行日志

## 2026-06-08

- BDD: 批次工作台打开详情 -> Given 用户有 eDHR 批次查询权限 When 打开 eDHR 批次执行菜单 Then 看到批次列表、任务进度和阻塞数。
- BDD: 工序任务打开单张执行页 -> Given 批次任务有默认批记录 When 用户点击打开填写 Then 前端跳转现有 eDHR 执行页并携带批次上下文。
- BDD: 最终打印入口 -> Given 批次已关闭且归档成功 When 用户点击打印 Then 前端触发受控 PDF 打印入口，不把物理打印机状态写成业务失败。

- RED: `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> FAIL, expected reason: E2E 脚本尚不存在，Node 返回 `MODULE_NOT_FOUND`。
- GREEN: 前端实现 -> PASS, 新增 `src/api/mes/pro/edhr/batchExecution.ts`、批次执行列表/详情/复盘页面、remaining hidden routes、真实路径 E2E 脚本；列表提供归档生成、查看、下载入口。
- GREEN: `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> BLOCKED, missing prerequisite: 当前 worktree 缺少 `node_modules\vue-tsc\bin\vue-tsc.js`，pnpm 提示 `Local package.json exists, but node_modules missing`。
- E2E: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> BLOCKED, missing prerequisites: `EDHR_BATCH_E2E_PASSWORD`、`EDHR_BATCH_E2E_WORK_ORDER_ID`、`EDHR_BATCH_E2E_BATCH_CODE`、`EDHR_BATCH_E2E_FIRST_FIELD_VALUE`、`EDHR_BATCH_E2E_CLOSE_PASSWORD`。
- REGRESSION: 现有 eDHR E2E 静态检查 -> BLOCKED, same prerequisite: worktree 未安装依赖，无法执行依赖 Playwright runtime 的真实前端回归。

## 2026-06-09

- BDD: 无可编辑字段模板仍可复核和提交 -> Given 某些报表快照没有可编辑字段元数据但执行快照、cellValues、字段审计基线有效 When 用户完成复核签名和提交 Then 页面不得用“无法生成最小表单”阻塞提交/复核，字段保存仍保持禁用。
- RED: `node tests\e2e\edhr-execution-submit-gate-static.spec.js` -> FAIL, expected reason: `ExecutionPage.vue` 仍直接用 `formRenderError` 阻塞复核/提交。
- GREEN: `node tests\e2e\edhr-execution-submit-gate-static.spec.js` -> PASS，复核/提交门禁改为 `formSubmitGateError`，可编辑表单错误只影响字段保存。
- GREEN: `pnpm install --frozen-lockfile` -> PASS，前端依赖安装完成。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- REGRESSION: `pnpm e2e:edhr:batch-execution:check` -> PASS。
- REGRESSION: `pnpm e2e:edhr:batch-execution:submit-gate` -> PASS。
- E2E: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实测试租户从批次执行入口打开/创建批次并进入单张 eDHR 执行页。
- E2E: Playwright 手工自动化全流程 -> PASS，测试租户批次 `EDHR-BATCH-122-FULL-0609020810` 完成 15 张必填单表复核/提交/审批，批次关闭并生成最终 PDF；复盘页展示 `BATCH_CLOSE` 和归档版本；芋道源码/admin 只读打开批次执行页成功。
- BDD: 融入后 int_main 完整批次执行 -> Given 合并后的后端 `48081` 与前端 `8081` 均从 `int_main` 主目录启动 When 测试租户用户从批次执行工作台创建批次并逐工序复核签名、提交、审批、关闭、归档、下载和打印 Then 15 张必填单表全部批准，批次归档为 `SEALED`，复盘页可查看关闭与归档记录。
- RED: Playwright 融入后完整流第一轮 -> FAIL, expected reason: 自动化未填写“提交 eDHR 执行”弹窗的提交密码，未发出提交请求；未修改业务代码。
- RED: Playwright 融入后完整流第二轮 -> FAIL, expected reason: 自动化将单张执行审批终态 `status=3` 误判为批次任务终态 `status=40`；实际审批已成功，未修改业务代码。
- GREEN: Playwright 融入后完整流继续验证 -> PASS，测试租户批次 `EDHR-BATCH-122-MAIN-0609013248` 完成 15 张必填单表复核签名、主数据追溯、提交和审批，最终同步为 `taskApprovedCount=15 / taskTotal=21 / blockedCount=0`。
- GREEN: Playwright 融入后归档/打印/复盘 -> PASS，批次 `EDHR-BATCH-122-MAIN-0609013248` 最终 `status=40`，归档 `id=2`、`archiveStatus=SEALED`、PDF `EDHR-BATCH-122-MAIN-0609013248-edhr-final.pdf` 下载成功，打印窗口打开，复盘页展示关闭/归档记录。
