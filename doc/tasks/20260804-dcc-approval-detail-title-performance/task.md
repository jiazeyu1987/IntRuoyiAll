# DCC 审批详情标题中文化与加载性能修复

## Task Goal

修复 BPM/DCC 审批详情页顶部标题仍显示英文 `DCC Controlled File Approval` 的问题，并定位从详情入口进入该页面加载很慢的前端链路，按根因减少非必要详情加载；追加删除截图红框中的“流程图”“流转记录”入口及其组件/API 准备链路，进一步加快首屏。

## Milestones

- [x] M0 读取项目规则、技能契约并保存开始前脏工作区基线
- [x] M1 定位审批详情标题来源、DCC 嵌入摘要/详情加载链路和现有静态合同
- [x] M2 先补 RED 静态合同，覆盖标题中文化和详情进入不应加载完整 DCC 详情的性能边界
- [x] M3 最小修改前端标题映射与详情加载条件
- [x] M4 运行目标静态合同、相邻合同和必要类型检查
- [x] M5 更新验证报告并完成收尾提交
- [ ] M6 推送本地提交到 `origin/int_main`
- [x] M7 删除审批详情页“流程图”“流转记录”入口和关联前端加载链路
- [x] M8 复跑目标静态合同、相邻合同和类型检查
- [x] M9 按用户要求补跑真实浏览器 E2E 验证

## Expected Verification

- RED：目标静态合同在旧代码下失败，证明英文标题仍可见或 DCC 审批详情仍触发完整详情加载。
- GREEN：目标静态合同通过，证明页面标题使用中文审批名称，且 BPM 审批详情只加载审批摘要/必要动作入口。
- GREEN：追加静态合同通过，证明 BPM 审批详情不再渲染“流程图”“流转记录”Tab，不再保留流程图 viewer、任务列表组件、流程图状态或 BPMN 模型视图请求链路。
- REAL_E2E：使用 Playwright 登录本机 `芋道源码/admin`，从审批中心 BPM 待办真实打开文控流程详情，断言中文标题、精简审核视图、文控正式处理入口、无“流程图/流转记录”、无 BPMN 模型视图/流转记录请求、目标写请求为 0。
- REGRESSION：运行相邻 BPM/DCC 审批详情静态合同或 `pnpm ts:check`；若全量检查被无关历史问题阻塞，记录隔离门禁和阻塞摘要。

## Current Status

blocked_on_push

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是修复标题来源和详情加载边界，不用默认成功或隐藏错误掩盖慢加载。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：若全量 `ts:check` 或大合同被无关历史问题阻塞，必须用任务专用最小静态合同完成 RED/GREEN，并记录无关阻塞。
- DCC 文控审批处理入口门禁：审批处理态不能被只读 viewer 或 API-only 替代，页面入口、处理控件和 DCC 链路必须保持真实可见。
- 前端同路由多入口分面门禁：BPM 审批详情通过业务表单嵌入 DCC 内容时，只应展示审批摘要和正式处理入口，不应无条件挂载完整业务详情页。

## Verification Evidence

- RED：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> FAIL，旧代码缺少 `DCC Controlled File Approval` 中文标题映射。
- GREEN：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/dcc-bpm-dcc-approval-viewer-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/form-center-bpm-dcc-approval-bypass-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/bpm-process-timeline-current-node-green-static.spec.js` -> PASS。
- REGRESSION：`pnpm ts:check` -> PASS。
- QUALITY：`git diff --check -- <task-owned files>` -> PASS，只有 Git LF/CRLF working-copy warnings。
- PUSH_BLOCKER：`git push origin int_main` -> FAIL，GitHub 443 连接经本机 `127.0.0.1` 代理失败；当前本地提交尚未推送到远端，任务不能标记为 completed。
- RED：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> FAIL，本轮追加合同证明旧源码仍存在 `<el-tabs v-model="activeTab">`、“流程图/流转记录”Tab 和流程图/任务列表链路。
- GREEN：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> PASS，追加删除 Tab 与加载链路合同通过。
- REGRESSION：`node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/dcc-bpm-dcc-approval-viewer-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/form-center-bpm-dcc-approval-bypass-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/bpm-process-timeline-current-node-green-static.spec.js` -> PASS。
- REGRESSION：`pnpm ts:check` -> PASS。
- EVIDENCE：frontend feature evidence validator -> PASS；bug regression evidence validator -> PASS。
- EXPERIENCE：本轮经验已合并到 `docs/frontend-development.md#前端同路由多入口分面门禁` 和 `docs/experience-index.md`，未新建长期经验文档。
- CLEANUP：`task_closeout.py --task-id 20260804-dcc-approval-detail-title-performance --mode preview` -> PASS。
- CLEANUP：`task_closeout.py --task-id 20260804-dcc-approval-detail-title-performance --mode apply` -> PASS，删除已归档的本轮 evidence 文件。
- PUSH_BLOCKER：本轮实现完成后仍需推送；历史 GitHub 443 本机代理连接问题尚未解除，任务不能标记为 completed。
- REAL_E2E_CHECK：`node --check doc\tasks\20260804-dcc-approval-detail-title-performance\bpm-dcc-approval-detail-real.e2e.cjs` -> PASS。
- REAL_E2E：`node doc\tasks\20260804-dcc-approval-detail-title-performance\bpm-dcc-approval-detail-real.e2e.cjs` -> PASS；从审批中心 BPM TODO 第 1 行打开 `processInstanceId=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0`，详情页可见 `文控受控文件审批`、`精简审核视图` 和 `进入文控审批处理页`；`extraLoadRequests=[]`、`targetWriteRequests=[]`、`pageErrors=[]`，详情可见耗时约 4.8s。
- REAL_E2E_ARTIFACT：`output\playwright\20260804-dcc-approval-detail-title-performance\bpm-dcc-approval-detail-real-evidence.json`；截图 `output\playwright\20260804-dcc-approval-detail-title-performance\bpm-dcc-approval-detail-real.png`。

## Baseline Commits

- `71177c0a5`：开始前残余脏工作区基线。
- `ae0cf0d96`：并行残余脏工作区第二基线。
- `0cb7335da`：本轮删除 Tab 前并行残余脏工作区基线。
- `46e0670a7`：本轮删除 Tab 前第二个并行残余脏工作区基线。

## Cleanup Keep

- doc/tasks/20260804-dcc-approval-detail-title-performance/bpm-dcc-approval-detail-real.e2e.cjs
