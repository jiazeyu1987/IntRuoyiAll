# 执行日志

BDD: 待处理工序卡片紧凑显示 -> Given 批记录存在多个待处理工序 When 用户打开批记录详情 Then 每个工序卡片保持序号和名称可读且高度不超过紧凑规格，首屏可看到更多工序。

BDD: 已填写工序信息紧凑但完整 -> Given 工序已有报告和状态 When 用户查看左侧工序列表 Then 工序名称、报告和状态仍可见，状态标签不额外占用整行高度。

BDD: 长工序名称不撑高卡片 -> Given 工序名称超过左栏宽度 When 用户查看列表 Then 名称保持单行省略，不引起卡片高度增长或横向溢出。

BDD: 选中与键盘操作保持 -> Given 用户点击工序或使用 Enter/Space 选择待处理工序 When 选中状态变化 Then 蓝色边框和焦点反馈仍然清晰，业务行为不变。

BDD: 特殊节点使用两位展示序号 -> Given 左侧列表包含灭菌报告、成品检报告和成品检记录 When 用户查看工序序号 Then 三个节点依次显示 `90`、`91`、`92`，不再显示后端的 `9000`、`9010`、`9020`。

BDD: 放行显示末尾序号 -> Given 左侧列表末尾存在放行虚拟工序 When 用户查看放行卡片 Then 序号徽标显示 `99`，工序名称仍为“放行”。

BDD: 序号文字统一为黑色 -> Given 左侧列表显示普通工序、特殊节点和放行工序 When 用户查看序号徽标 Then 徽标内数字均使用黑色文字。

REPRODUCTION: 用户截图中待处理工序卡片最小高度为 `72px`、列表间距为 `8px`，在窄侧栏中单屏可见工序数量偏少。

ROOT_CAUSE: `BatchExecutionDetailPage.vue` 为待处理工序卡片设置固定 `min-height: 72px` 和 `padding: 10px`；列表和待处理分组均使用 `8px` 间距，已填写工序卡片采用纵向三段布局，状态标签额外占行。

RED: `node tests/e2e/edhr-batch-process-card-density-static.spec.js` -> FAIL，现有工序列表仍使用 `8px` 间距，待处理卡片仍为 `72px` 最小高度。

GREEN: `node tests/e2e/edhr-batch-process-card-density-static.spec.js` -> PASS，待处理卡片最小高度为 `48px`，列表间距为 `6px`，已填写卡片使用紧凑双列布局。

RED: `node tests/e2e/edhr-batch-process-display-sort-static.spec.js` -> FAIL，灭菌报告缺少展示序号 `90`，当前仍直接显示后端 `routeProcessSort`。

GREEN: `node tests/e2e/edhr-batch-process-display-sort-static.spec.js` -> PASS，特殊节点显示 `90/91/92`，放行显示 `99`，序号文字为黑色。

REGRESSION: `node tests/e2e/edhr-pending-task-name-single-line-static.spec.js`、`node tests/e2e/edhr-process-header-compact-context-static.spec.js`、`node tests/e2e/edhr-batch-review-rail-width-static.spec.js`、`node tests/e2e/mes-edhr-batch-review-remove-task-index-static.spec.js` -> PASS。

BLOCKER: none

GREEN: experience-preflight -> PASS，已读取 PowerShell、登录和 Playwright 门禁；真实验证仅访问本机 `http://localhost:8081`，使用 `芋道源码/admin` 执行最终只读页面检查，不操作远端环境或业务数据。

GREEN: official-login-preflight -> PASS，真实登录已进入 `/mes/pro/feedback/edhr-batch-execution`。

GREEN: real-page-visual -> PASS，本机 `芋道源码/admin` 只读打开 `881MO090889`；真实批次详情中 18 个待处理卡片高度均为 `48px`、2 个已填写卡片高度均为 `54px`，页面无业务写请求。

GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS；默认 4GB 类型检查曾因 Node 堆内存不足退出，提升到项目构建约定的 8GB 后通过。

GREEN: task-closeout preview/apply -> PASS，仅保留 `task.md` 和 `execution-log.md`，一次性视觉脚本与截图已清理。

GREEN: task-completion -> PASS，任务目标、静态回归、类型检查和真实页面只读验证均完成。
