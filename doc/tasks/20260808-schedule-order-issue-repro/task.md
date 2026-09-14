# 排产工单交互问题复现

## Task Goal

通过本机真实前端路径复现用户列出的排产工单交互问题，并输出逐项是否可复现、证据来源、阻塞条件和后续修复建议；本任务只做复现确认，不改生产代码、不执行写入型排产操作。

## Scope

- 目标页面：`IntRuoyiFronted/src/views/mes/pro/scheduleorder/` 对应的排产工单页面。
- 目标入口：本机 `http://localhost:8081` 或 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 只读边界：允许登录、筛选、翻页、打开详情/抽屉、点击只读按钮和采集 DOM/网络证据；禁止入池、重排发布、强制完成、调整交期提交、冻结或其它写操作。
- 产物：任务自有 Playwright 复现脚本、JSON 结果、截图和验证报告。

## Milestones

- [x] M1：读取 task closeout、E2E、登录、本机运行、PowerShell 编码和 Playwright 规则。
- [x] M2：建立任务记录并写入 BDD/验证口径。
- [x] M3：确认本机前端、后端、登录和排产工单入口可用。
- [x] M4：用真实页面逐项尝试复现六类用户问题。
- [x] M5：输出验证报告，标记 `completed` 或阻塞项。

## Expected Verification

- 真实页面登录默认本机身份 `芋道源码/admin`，不记录密码或 token。
- 采集 `/admin-api/mes/pro/schedule-order/page` 等目标请求的参数、HTTP 状态、业务码和返回摘要。
- 逐项记录：组合筛选删除条件、跳页输入框、固定列点击坐标、详情“报工对比”、反向承诺交期、优先级/排序/详情图标可访问性。
- 只读复现目标写请求数为 0；若页面阻塞于登录、入口、运行态或样本数据不足，记录 precise blocker。

## BDD Scenarios

- BDD: 组合筛选删除单个条件 -> Given 排产工单页面已有多个已执行筛选条件 When 删除其中一个条件 Then 页面只应移除该条件并重新查询，不能清空全部条件或保留旧结果。
- BDD: 跳页输入框同步 -> Given 排产工单存在多页数据 When 在分页跳页输入框输入目标页码并确认 Then 当前页码、输入框显示和列表请求 pageNo 必须一致。
- BDD: 固定列点击命中 -> Given 表格存在固定列和“当前工序”可点击内容 When 点击当前工序文本 Then 命中元素应属于当前工序，不应被固定列遮挡到其它操作。
- BDD: 报工对比按钮响应 -> Given 排产工单详情窗口已打开 When 点击“报工对比” Then 页面应打开对比内容或发出对应只读请求，不应无响应。
- BDD: 反向承诺交期筛选 -> Given 已设置反向承诺交期筛选 When 执行查询 Then 该筛选值不能被静默清空，也不能恢复全量数据。
- BDD: 可访问性与校验状态 -> Given 排产工单表格存在优先级、排序状态和详情图标 When 通过 DOM 与交互检查这些控件 Then 必须有明确校验、可见状态和可访问名称。

## Applicable Experience Gates

- `docs/e2e-rules.md#playwright-浏览器可执行文件门禁`：真实 E2E 优先使用本机 Chrome/Edge，浏览器缺失不能归因为业务失败。
- `docs/e2e-rules.md#playwright-目标链路与外部资源异常归因门禁`：区分目标业务请求与非目标资源异常，记录目标写请求数。
- `docs/e2e-rules.md#表格行定位`：Element Plus 表格需按可见 body 行和业务文本定位，避免 header/fixed 重复 DOM 误点。
- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：筛选草稿、已执行条件、标签和结果必须一致。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；本任务先复现和定位证据，不用静态推测替代真实页面结论。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 当前阶段：任务已完成。
- 已完成：规则读取、任务目录创建、BDD 场景、真实页面复现、验证报告、cleanup preview/apply。
- 最终验证：3 项可复现，3 项未复现；MES 写请求数为 0。
- 清理结果：已删除本任务 stale `failure.txt`；保留复现脚本、结果 JSON、截图和验证报告。
- 阻塞项：暂无。

## Cleanup Keep

- doc/tasks/20260808-schedule-order-issue-repro/reproduce-schedule-order-issues.cjs
- output/playwright/20260808-schedule-order-issue-repro/result.json
- output/playwright/20260808-schedule-order-issue-repro/schedule-order-final.png
