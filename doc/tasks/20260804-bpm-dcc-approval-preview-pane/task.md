# 20260804 BPM DCC 审批详情预览区修复

## Task Goal

修复 BPM 文控受控文件审批详情页：截图黄框内容不显示；截图红框主内容区直接显示受控文件预览，同时保留右侧审批时间线和底部审批操作按钮。

## Milestones

- [x] M0: 读取前端、E2E、任务收尾、PowerShell 编码和经验门禁，并保存开始前脏工作区基线。
- [x] M1: 编写聚焦静态合同，先 RED 锁定顶部编号/提示栏残留且主区缺少预览。
- [x] M2: 最小修改 BPM DCC 审批详情模板，隐藏黄框内容并在主区嵌入正式受控文件预览组件。
- [x] M3: 运行聚焦静态合同、相邻回归合同和类型检查。
- [x] M4: 更新验证报告、经验归档判断和收尾状态。

## Expected Verification

- `node tests/e2e/bpm-dcc-approval-preview-pane-static.spec.js`
- `node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js`
- `node tests/e2e/dcc-approval-upload-view-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- <task-owned files>`

## Applied Experience Gates

### 前端同路由多入口分面门禁

- Trigger: BPM 详情页通过 DCC 自定义业务表单展示审批内容，且用户要求调整截图区域信息边界。
- Preflight check: 明确 BPM 审批详情页只展示审批判断和文件预览，不显示通用编号/打印行或跳转提示栏。
- Blocker: 只用 CSS 透明隐藏黄框、仍保留旧跳转提示、或主内容区未使用正式受控文件预览组件。
- Verification: 聚焦静态合同断言黄框内容移除、`ProtectedPdfViewer` 正式嵌入、非 DCC 自定义表单仍保留原挂载路径。
- Forbidden action: 禁止用 mock/placeholder/fallback 数据、吞异常或猜测文件 URL 代替正式预览链路。

### DCC 预览不可用原因短路门禁

- Trigger: 使用 `ProtectedPdfViewer` 展示 DCC 受控文件预览。
- Preflight check: 复用既有组件，不新增二进制预览请求拼接或默认下载兜底。
- Blocker: 直接猜测 blob、OnlyOffice 地址、published/stamped 文件 ID 或绕过现有 preview metadata。
- Verification: 静态合同确认只传 `controlledFileId` 给 `ProtectedPdfViewer`。
- Forbidden action: 禁止默认下载、空 Blob、OnlyOffice token fallback 或 API-only 断言。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 BPM DCC 审批页的正式展示边界修复模板结构。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

实现、聚焦验证、相邻回归、类型检查、技能证据校验、cleanup 和本地实现提交均已完成。`git push origin int_main` 因本机代理 `127.0.0.1` 无法连接 GitHub 443 失败，本地分支仍领先 `origin/int_main`，按项目规则任务不能标记为 completed。
