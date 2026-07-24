# 执行日志

BDD: 所有节点卡片等高完整显示 -> Given 左侧工序数量超过可视高度 / When 列表进入内部滚动 / Then 特殊节点、普通工序和放行节点保持统一高度且不被压缩遮挡。

BDD: 普通工序显示业务名称 -> Given 普通工序同时具有编码和名称 / When 用户查看左侧导航 / Then 卡片主文本只显示工序名称，不显示工序编码。

BDD: 当前工序名称完整显示 -> Given 工序名称与状态同时展示 / When 用户查看桌面端左侧导航 / Then 当前真实工序名称不出现横向省略或遮挡。

GREEN: previous-task-check -> PASS，前序同页面任务 `20260710-edhr-batch-process-order-layout` 已完成；当前目标组件没有未提交改动。

ROOT_CAUSE: 左侧列表使用纵向 flex 布局，但普通工序组与放行卡片未禁用 `flex-shrink`，长列表会优先压缩直接子项而不是保持卡片高度并滚动；普通工序模板仍将 `processCode` 与 `processName` 拼接为主文本。

RED: `node tests/e2e/edhr-batch-process-item-uniform-name-static.spec.js` -> FAIL，预期原因：普通工序仍拼接显示编码和名称，且列表未定义统一高度与防压缩样式。

GREEN: experience-preflight -> PASS，已读取 `docs/login-access.md` 和 Playwright 执行门禁；真实验证限定本机 `http://localhost:8081`、管理员只读路径和真实批次，不产生 MES 写请求。

RED: `node tests/e2e/edhr-batch-process-item-uniform-name-static.spec.js` -> FAIL，预期原因：左侧工序栏仍为 203px，真实工序名称与状态同时显示时存在横向省略。

GREEN: target-static-regression -> PASS，等高名称、顺序、卡片密度、展示序号、左栏宽度和主区域填满共 6 个静态测试通过。

GREEN: node-syntax -> PASS，新增静态和真实 E2E 脚本语法检查通过。

GREEN: targeted-eslint -> PASS，直接执行 `node_modules/eslint/bin/eslint.js` 完成目标组件与测试检查。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。

GREEN: official-login-preflight -> PASS，本机 `芋道源码/admin` 真实登录进入批次执行页。

GREEN: real-readonly-e2e -> PASS，真实批次 `900000000480` 共 19 个左侧节点高度均为 48px，普通工序显示接口 `processName` 且无横向裁切，状态标签未越界，MES 写请求为 0。

GREEN: visual-review -> PASS，真实页面截图确认工序名称完整显示，卡片等高且列表由内部滚动承载。

GREEN: git-diff-check -> PASS。

GREEN: implementation-commit -> PASS，本任务实现、回归测试与正式任务记录已提交 `615da58ac`。

GREEN: task-closeout-cleanup-preview -> PASS，保留正式任务记录与并行性能诊断脚本，仅计划清理本任务临时证据和 E2E 输出。

GREEN: task-closeout-cleanup-apply -> PASS，临时缺陷证据、前端证据和 `tests/output/20260710-edhr-batch-process-item-uniform-name/` 已清理。

GREEN: final-status -> PASS，本任务状态更新为 `completed`。

DIAGNOSTIC: batch-detail-slow-load -> 用户反馈进入批次详情加载时间过长；本轮仅执行只读性能诊断，不覆盖当前组件并行样式改动。

PERFORMANCE_SCOPE: 首屏由 `/get`、`/workbench`、`/review-timeline` 三个接口组成，页面必须等三者全部结束后才关闭全页 loading。

GREEN: experience-preflight -> PASS，已读取登录与真实 Playwright 门禁；诊断限定本机 `http://localhost:8081`、测试租户真实批次和只读 GET 请求。

ROOT_CAUSE: 前端 `loadDetail` 先并行等待批次详情和工作台，再串行等待复盘时间线，且全页 `loading` 直到三者全部完成才关闭；首屏耗时等于 `max(/get, /workbench) + /review-timeline`。

ROOT_CAUSE: `/get` 不是轻量只读查询。它会对活动批次执行 `syncBatchStatus`，逐任务查询执行记录并更新任务状态，然后重新读取批次、任务、待办、分配规则、用户、关闭阻塞项、放行状态和当前工序填写人；关闭阻塞检查还会再次逐任务查询执行、签名和附件。

ROOT_CAUSE: `/review-timeline` 存在按执行记录展开的 N+1 聚合。每个已有 `executionId` 的任务至少分别查询执行记录、签名、字段审计、审批快照、主数据追溯和附件，并把 `sheetLayoutJson`、`metaJson`、`executionSnapshotJson`、`cellValuesJson` 等完整表单数据全部放入首屏响应。

ROOT_CAUSE: `/workbench` 再次读取批次和任务，并扫描操作审计、字段审计和主数据追溯；与 `/get`、`/review-timeline` 存在明显重复读取。

BLOCKER: runtime-latency-sample -> 官方本机测试租户登录通过，但当前批次列表为空，旧诊断批次 `900000000472` 已不存在；当前上下文没有可使用的 `芋道源码/admin` 密码，未猜测凭据。影响：本轮不能给出可信的真实批次毫秒耗时，但代码级根因与请求依赖链已确认。
