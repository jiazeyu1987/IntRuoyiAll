# 任务：DCC 预览详情快捷信息弹框

## 任务目标

在 DCC 受控文件预览页右侧基础信息面板顶部移除“基础信息”标题，在原标题区域新增“审批”“分发”“版本”三个按钮；点击后分别弹出弹框展示当前文件的审批矩阵批准情况、分发信息和版本信息。保持原有“修改”按钮权限和基础信息内容不变。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-dcc-preview-return-to-origin/task.md`
- 状态：`blocked`
- 处理：上一任务缺少真实 Playwright 四入口返回验证证据与独立提交边界，已记录阻塞。本任务只在现有工作区基础上做本次按钮与弹框增量，提交时不得混入上一任务既有改动。

## BDD 场景

- BDD: 预览详情顶部显示快捷按钮 -> Given 用户打开 DCC 受控文件预览页 / When 右侧详情面板加载 / Then 顶部不再显示“基础信息”，并显示“审批”“分发”“版本”三个按钮。
- BDD: 打开审批弹框 -> Given 当前文件已加载审批路线快照 / When 用户点击“审批” / Then 弹框展示该文件审批矩阵批准情况，包括阶段、审批方式、通过比例和解析审批人。
- BDD: 打开分发弹框 -> Given 当前文件已加载分发状态 / When 用户点击“分发” / Then 弹框展示该文件分发信息，包括部门、发放方式、状态、接收人、发放人、回收信息。
- BDD: 打开版本弹框 -> Given 当前文件已加载版本历史 / When 用户点击“版本” / Then 弹框展示该文件版本信息，包括标题、文件编号、版本、状态、发布时间和作废时间。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务状态。
- [x] M2：新增 RED 静态契约测试覆盖标题移除、按钮和弹框。
- [x] M3：实现预览详情按钮与三个信息弹框。
- [x] M4：运行静态测试、类型检查和真实 Playwright 验证。
- [x] M5：更新任务证据，运行前端证据校验、收尾清理预览并提交本任务改动。

## Expected Verification

- `node scripts/dcc-controlled-file-preview-detail-panel.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-dcc-preview-info-actions-dialogs/frontend-feature-evidence.md`
- 如本地入口 `http://localhost:8081` 已可用，使用 Playwright 登录测试租户打开受控文件预览页，点击三个按钮确认弹框真实出现并展示数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本次只展示详情接口已有的真实数据；无数据时使用表格空状态，不创建替代数据。
- `是否从根因和长期维护角度解决`：是。复用现有受控文件详情数据结构和共享基础信息面板扩展点，不新建旁路数据源。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## Current Status

completed

## 当前证据

- RED：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> FAIL，预览详情仍显示 `title="基础信息"` 且缺少三个弹框。
- GREEN：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：Playwright CLI 使用 `测试租户/aoteman` 打开真实文件 `2054545668044046252` 预览页，三个按钮和三个弹框验证通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-dcc-preview-info-actions-dialogs/frontend-feature-evidence.md` -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，保留 `task.md` 与 `execution-log.md`，清理临时证据文件和 E2E 脚本。
- Screenshot：`output/playwright/dcc-preview-info-actions-dialogs.png` 已生成并用于人工查看，作为临时测试产物收尾清理。

## Cleanup Candidates

- `doc/tasks/20260607-dcc-preview-info-actions-dialogs/frontend-feature-evidence.md`
- `doc/tasks/20260607-dcc-preview-info-actions-dialogs/verify-preview-info-actions.e2e.mjs`
- `output/playwright/dcc-preview-info-actions-dialogs.png`
