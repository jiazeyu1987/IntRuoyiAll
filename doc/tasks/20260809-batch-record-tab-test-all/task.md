# 批记录测试 Tab 全量测试

## Task Goal

在批记录测试页的五个 Tab 顶部各增加“测试全部”按钮；点击后通过现有 Codex CLI 执行链路，按当前 Tab 的完整行集合逐行测试，并将每行结果保留到对应“历史”入口。

## Milestones

- [completed] M1：记录 BDD 场景并建立失败测试。
- [completed] M2：实现五个 Tab 的批量测试入口、互斥状态和顺序执行。
- [completed] M3：完成静态回归、真实前端 Playwright 验证和证据归档。
- [completed] M4：完成任务清理与项目级经验沉淀。

## Expected Verification

- 静态契约测试确认五个 Tab 均有批量测试按钮，且使用完整 Tab 行集合顺序执行。
- 现有批记录测试相关静态测试全部通过，逐行测试和“历史”行为无回归。
- 在真实前端路径登录并选择测试租户后，点击当前 Tab 顶部按钮能够完成该 Tab 全部行测试，按钮展示进度，每行“历史”可查看 Codex CLI 回复。

## Applicable Experience Gates

- 适用 `docs/frontend-development.md#前端行级异步结果归属门禁`：按稳定 `caseName` 保存逐行历史；启动时冻结 history key、executionId 和 run token；轮询写入校验 poll token、executionId 与 history key；重测只清空当前行，终态后才允许查看历史；禁止旧轮询串写、快速连点和自动展示最后一次全局回复。
- 批量测试在上述门禁上增加单任务顺序执行：完整 Tab 行集合逐行等待终态，任一时刻只允许一个 Codex CLI 执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用正式 Codex CLI 执行接口并将单行执行抽成可等待的共享流程，批量操作严格顺序执行。
- `是否存在临时补丁或绕过`：否。

## Completed Work

- 已建立任务目录和实施边界。
- 五个 Tab 顶部均增加带执行图标的“测试全部”按钮，并绑定各自完整行集合。
- 单行和批量测试共用可等待到终态的 Codex CLI 轮询流程；批量严格逐行执行，展示 `完成数/总数`，并与其它测试入口互斥。
- 终态 PASS、FAIL、BLOCKED 等均写入对应行历史；接口异常明确停止后续行并显示失败行。
- 增加 `1280px` 桌面窄视口工具栏收缩规则，筛选区让出空间，确保“测试全部”和“新增”不被裁切。
- 已执行 task-closeout-cleanup preview/apply，仅删除临时 `frontend-feature-evidence.md`，保留任务文档、验证报告和正式截图。
- 已将顺序批量轮询和窄视口单行工具栏经验合并至 `docs/frontend-development.md` 与 `docs/experience-index.md`。

## Verification Evidence

- RED：新增静态契约按预期失败，页面全量测试按钮数量为 0（期望 5）。
- RED：真实 `1280x720` 截图发现右侧批量按钮组被裁切；新增响应式契约按预期失败。
- GREEN：8 个批记录测试静态契约全部通过；`pnpm ts:check` 通过。
- Playwright：五个 Tab 分别只有一个“测试全部”按钮；生产组长批量进度依次观察到 `0/5`、`1/5`、`3/5` 后完成，五个“历史”均由禁用变为可点击。
- Codex CLI：execution `139..143` 全部到达正式终态，分别为 FAIL、FAIL、PASS、FAIL、FAIL；失败/阻塞为代码检查结果，不是批量执行中断，五行均完成。
- 运行态目标请求 `tenant/simple-list`、`codex-test-case/page` 与五个列配置接口均为 HTTP 200；新建干净页面控制台错误数为 0。
- 截图：`output/playwright/batch-record-tab-test-all.png`、`output/playwright/batch-record-tab-test-all-1280.png`。

## Remaining Blockers

- 无。

## Current Status

completed
