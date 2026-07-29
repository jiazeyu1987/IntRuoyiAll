# 20260729 eDHR 产品信息当前工序标签修复

## Task Goal

修复 eDHR 填写页切换到“产品信息”虚拟工序后，顶部“工序”仍显示来源“粗洗工序”的问题。顶部标签、当前项高亮和填写人范围必须使用同一当前批次任务及虚拟工序口径。

## Milestones

- [x] 根据用户截图定位顶部工序标签来源。
- [x] 建立聚焦回归测试并记录 RED。
- [x] 实施最小正式修复。
- [x] 运行目标测试、相邻回归、类型检查和真实页面 E2E。
- [ ] 完成经验沉淀、清理、提交和推送。

## Expected Verification

- 当前 `batchTaskId` 指向产品信息任务时，顶部“工序”必须显示“产品信息”，不得显示来源 `task.processName` 的“粗洗工序”。
- 当前普通工序任务仍显示其正式 `processName/processCode`。
- 当前任务解析必须继续复用 `resolveCurrentAssistBatchTask` 和产品信息虚拟工序识别，不使用路由文案、`formBindings`、当前登录人或默认值猜测。
- 产品信息工序卡片、顶部工序标签和切换填写人范围使用一致的显示工序口径。
- 聚焦静态合同、相邻回归、`pnpm ts:check` 和真实页面只读 E2E 通过。

## Applicable Gates

- `docs/frontend-development.md#eDHR 产品信息虚拟 80 工序门禁`：填写页产品信息必须独立于来源工序，切换工序和切换填写人复用同一显示工序 group key。
- `docs/frontend-development.md#前端 Route Query ID 比较门禁`：当前 `batchTaskId` 必须按 route-id 语义匹配批次任务。
- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：未开始工序继续通过 `batchTaskPreview=1 + task/preview` 在填写页只读展示。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：静态合同按稳定函数定位并兼容 CRLF/LF。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；顶部标签从当前批次任务的正式显示工序解析，不继续直接读取来源工序字段。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Completed Work

- 将当前批次任务解析拆为可选解析函数和 fail-fast 必选解析函数，顶部标签与填写人解析复用同一任务识别口径。
- 顶部“工序”通过当前 `batchTaskId` 对应任务调用 `resolveAssistProcessSwitchItemName`；产品信息显示虚拟名称，普通工序保持正式名称。
- 保留产品信息任务来源 `routeProcessId/processName` 追溯语义，未修改后端任务、批记录绑定或表单槽位链路。
- 新增聚焦静态合同，锁定路由任务识别、虚拟工序显示名和旧直接读取来源工序字段的禁用。

## Verification Result

- 聚焦静态合同、5 项相邻回归、产品信息批次详情合同和 `pnpm ts:check` 均通过。
- 本机 `8081/48081` 前后端可用，官方登录前置通过。
- 真实只读 Playwright 从粗洗任务切换到产品信息任务后，顶部标签为“产品信息”，产品信息卡片为当前项，3 个填写人候选全部属于任务 `7232`，MES 写请求、MES HTTP 错误和 console error 均为 `0`。

## Remaining Closeout

- 完成任务自有收尾记录提交并推送 `origin/int_main`。

## Cleanup Candidates

- `doc/tasks/20260729-edhr-product-info-current-process-label/bug-regression-evidence.md`
- `output/playwright/20260729-product-info-current-process-label-e2e.json`
- `output/playwright/20260729-product-info-current-process-label-e2e.png`
- `output/playwright/20260729-product-info-current-process-label-e2e-failure.png`

## Cleanup Result

- `task-closeout-cleanup` preview 与 apply 均通过。
- 保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 已删除本任务 bug 中间证据、E2E JSON 和成功/失败截图。
