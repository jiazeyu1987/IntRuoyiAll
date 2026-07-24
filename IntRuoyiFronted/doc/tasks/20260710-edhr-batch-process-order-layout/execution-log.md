# 执行日志

BDD: 普通工序位于收尾节点之前 -> Given 批次同时包含普通工序和成品检特殊节点 / When 用户打开批次详情 / Then 普通工序显示在灭菌、成品检报告、成品检记录之前。

BDD: 来料检仍位于普通工序之前 -> Given 批次存在来料检特殊节点 / When 用户查看左侧工序列表 / Then 来料检显示在普通工序之前。

BDD: 放行保持最后 -> Given 批次存在普通工序和收尾节点 / When 用户查看左侧工序列表 / Then 放行固定显示在全部工序之后。

BDD: 工序文本不重叠 -> Given 工序编码和名称较长 / When 左侧栏宽度受限 / Then 编码名称保持单行省略且完整内容可通过提示查看。

GREEN: previous-task-check -> PASS，已完成的工序辅助表单任务不阻塞本次左侧顺序修复；并行未提交改动将原样保留。

ROOT_CAUSE: 模板先整体渲染 `specialTaskEntries`，再渲染 `processTaskGroups`，导致展示序号 90/91/92 的收尾特殊节点全部出现在普通工序之前；工序组标题允许换行，窄栏中出现上下文本挤压。

RED: `node tests/e2e/edhr-batch-process-order-layout-static.spec.js` -> FAIL，预期原因：页面缺少 `preProcessSpecialTaskEntries`，全部特殊节点仍整体渲染在普通工序之前。

GREEN: `node tests/e2e/edhr-batch-process-order-layout-static.spec.js` -> PASS，前置特殊节点、普通工序、收尾特殊节点、放行顺序契约通过，工序标题单行省略契约通过。

REGRESSION: `node tests/e2e/edhr-batch-process-display-sort-static.spec.js`、`node tests/e2e/edhr-batch-process-card-density-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。

GREEN: experience-preflight -> PASS，已读取 `docs/login-access.md` 和 Playwright 执行门禁；真实验证限定本机、只读操作和真实登录路径。

GREEN: `login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --target-path /mes/pro/feedback/edhr-batch-execution` -> PASS，真实登录已进入目标列表页。

BLOCKER: real-data-page-verification -> 测试租户真实批次列表为空，`edhr-batch-process-companion-forms-real.e2e.js` 按预期失败并报告“测试租户批次列表没有可验收数据”。影响：无法在测试租户验证普通工序与 90/91/92/99 的实际 DOM 顺序。

BLOCKER: readonly-admin-login -> 当前环境未配置 `芋道源码/admin` 只读登录密码，登录页源码也未提供默认密码；未猜测凭据、未切换环境。影响：无法使用已知批次 `900000000480` 完成管理员只读复验。

INTEGRATION: source-change-in-head -> 当前 `int_main` 的提交 `99be6babb03c3e071f259d3efbbd8a0c38fe3e92` 已包含 `preProcessSpecialTaskEntries`、`postProcessSpecialTaskEntries` 和工序标题单行省略修复；本任务新增回归测试与阻塞证据仍保持未提交。

RESOLVED: readonly-admin-login -> 用户提供本机 `芋道源码/admin` 登录凭据，管理员官方登录预检通过。

GREEN: `login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --target-path /mes/pro/feedback/edhr-batch-execution` -> PASS。

GREEN: `EDHR_COMPANION_E2E_READONLY_ADMIN=1 EDHR_COMPANION_E2E_STRUCTURAL_ONLY=1 EDHR_COMPANION_E2E_BATCH_ID=900000000480 node tests/e2e/edhr-batch-process-companion-forms-real.e2e.js` -> PASS，真实批次和 `routeProcessId=923207` 已通过只读结构验收，MES 写请求为 0。

GREEN: real-page-visual-order -> PASS，真实页面截图确认左侧顺序为“来料检报告 -> 普通工序 1-14 -> 灭菌报告 90 -> 成品检报告 91 -> 成品检记录 92 -> 放行 99”，工序卡片无文本重叠。

DIAGNOSTIC: direct-detail-dom-probe -> FAIL，独立直接跳转探针未等待动态路由完成而超时；不作为验收结论，已由官方登录预检、既有真实 E2E 和真实页面截图完成正式验证。

GREEN: implementation-commit -> PASS，本任务回归测试、任务文档和请求命令记录已提交 `a7e0997108f9ff36a0efadd5cec7f1ca5c01e34a`；目标组件修复位于提交 `99be6babb03c3e071f259d3efbbd8a0c38fe3e92`。

GREEN: task-closeout-cleanup-preview -> PASS，仅计划删除本任务两个临时证据文件和 `tests/output/20260710-edhr-batch-process-order-layout/`，保留三份正式任务记录，无阻塞或警告。

GREEN: task-closeout-cleanup-apply -> PASS，候选临时产物已删除，`task.md`、`execution-log.md` 和 `verification-report.md` 已保留。

GREEN: final-status -> PASS，任务状态更新为 `completed`，实现、回归、真实只读验收和收尾证据完整。
