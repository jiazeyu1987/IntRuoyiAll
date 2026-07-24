# 任务：DCC OnlyOffice 只读预览关闭匿名协作姓名弹窗

## 任务目标

修复 DCC 受控文件 OnlyOffice 只读预览时总弹出“输入用于协作的名称”对话框的问题。只读预览应直接进入阅读态，不要求用户输入协作名称。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-dcc-preview-detail-panel/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 DCC OnlyOffice 只读预览配置、静态契约测试和任务记录。

## BDD 场景

- BDD: 只读预览不提示匿名协作名称 -> Given 用户打开 DCC Office 文件只读预览 / When OnlyOffice DocEditor 初始化 / Then 页面直接进入只读预览，不弹出“输入用于协作的名称”对话框。
- BDD: 只读权限保持不变 -> Given 用户打开 DCC Office 文件只读预览 / When OnlyOffice 配置完成 / Then 仍保持 edit/comment/review/download/print/copy 全部关闭。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：修改 OnlyOffice 只读预览配置。
- [x] M3：补充静态契约测试并运行验证。
- [x] M4：更新证据并完成收尾。

## Expected Verification

- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只读预览继续显式报错，不用兼容分支隐藏初始化失败。
- `是否从根因和长期维护角度解决`：是。通过 OnlyOffice 只读配置关闭匿名协作姓名请求，不靠 DOM 注入或弹窗脚本规避。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 当前证据

- GREEN：`node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- INFO：OnlyOffice 只读预览显式配置了固定只读用户 `dcc-readonly-viewer / 受控预览`，并将 `customization.anonymous.request` 设为 `false`，避免匿名协作姓名弹窗。
