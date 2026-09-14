# Frontend Feature Evidence

## Feature

DCC 原版发布链路前端体验优化：上传预览错误归因、编号唯一性提前校验、上传前权限提示、审批处理态、签名弹窗业务语义、四级审批轨迹、发布后受控浏览跳转、当前有效版标识、审批待办关键信息。

## Non-goals

- 不修改 DCC 审批、签名、发布状态机。
- 不通过 API/SQL 制造 DCC 文件状态、审批任务或签名证据。
- 不扩大到升版、作废、分发、培训等其它 DCC 场景。

## Acceptance

- UX-01 上传预览失败区分文件存储不可用、文件格式问题、权限不足、编号重复。
- UX-02 文件编号和 V1.0 重复在提交前提示并阻止；新编号提示将创建 master 主档。
- UX-03 分类、DCC 项目代码、文件类型候选加载或权限缺口在上传页前置展示。
- UX-04 审批详情页区分只读预览态和待我审批/签名处理态。
- UX-05 签名弹窗展示节点、签名人、动作、提交后流转和电子签名审计证据说明。
- UX-06 文件详情页固定展示文控审核、会签审核、会签批准、文控批准四级轨迹。
- UX-07 ACTIVE 文件详情页提供查看受控浏览当前有效版入口。
- UX-08 详情页和受控浏览突出当前有效版 / ACTIVE / V1.0。
- UX-09 DCC 审批待办行展示文件编号、版本、文件类型、当前审批节点和申请人。

## UI Entry Points

- `src/views/dcc/controlled-file/upload/index.vue`
- `src/views/dcc/controlled-file/detail/index.vue`
- `src/views/dcc/controlled-file/browser/index.vue`
- `src/views/approval-center/index.vue`

## API Contracts And Data States

- 上传预览仍调用正式 `uploadControlledFilePreview`，失败只做 UI 归因展示，不改成功/失败语义。
- 文件编号仍调用正式 `getControlledFileCurrentVersion`，重复版本由前端预检查拦截，后端唯一性仍保留 fail-fast。
- 审批详情仍使用正式 `todoTask`、BPM task list、routeSnapshots 和 signatureSummaries。
- 受控浏览仍使用正式版本摘要数据，只新增 ACTIVE 最新版标签。
- 审批中心 DCC 关键信息从现有 summary 字段和 businessContextTags 展示，不新增后端 mock 字段。

## BDD

- BDD: 上传预览错误可解释 -> Given 上传人位于 DCC 原版上传页, When 上传预览接口返回文件存储不可用、文件格式错误、权限不足或编号重复, Then 页面在上传区域展示对应业务原因和处理建议。
- BDD: 文件编号提前唯一性校验 -> Given 上传人填写文件编号, When 文件编号变更并触发校验, Then 页面显示将创建 master 主档或编号已存在, And 提交前阻止重复 V1.0 原版。
- BDD: 上传前权限前置提示 -> Given 上传人进入上传页, When 分类、项目代码或文件类型列表为空或无权限, Then 页面在对应控件旁显示明确缺口。
- BDD: 审批详情处理态清晰 -> Given 审批人从 DCC 待办进入详情, When 当前账号有待处理任务, Then 页面展示待我审批/签名处理态；viewer 展示只读预览态。
- BDD: 签名弹窗业务提示增强 -> Given 审批人点击签名动作, When 弹窗打开, Then 弹窗展示当前节点、签名人、动作、提交后流转和电子签名审计提示。
- BDD: 审批进度固定可视化 -> Given 用户查看文件详情, Then 页面固定展示四级审批轨迹和处理人、处理时间、签名状态。
- BDD: 发布后直达受控浏览 -> Given 原版 V1.0 已 ACTIVE, Then 详情页提供查看受控浏览当前有效版按钮。
- BDD: 当前有效版标识突出 -> Given 用户查看详情或受控浏览, Then 当前 master 有效版本显示当前有效版 / ACTIVE / 版本号。
- BDD: 审批待办展示 DCC 关键信息 -> Given 审批人查看 DCC 待办, Then 行内展示文件编号、版本、文件类型、当前审批节点和申请人。

## RED

- RED: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> FAIL, missing `resolveUploadPreviewErrorMessage`; 上传预览缺少专用错误归因 helper。

## GREEN

- GREEN: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-current-version-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-approval-center-handling-entry-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-browser-version-summary-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-handling-summary-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- Error states: 上传预览失败保留真实错误详情，并按存储、格式、权限、编号重复展示可解释文案。
- Permission and empty states: 上传页对 DCC 项目、文件分类、文件类别候选失败或无可上传类别显示前置提示。
- Loading states: 保留既有项目代码、文件分类、上传预览 loading 状态。
- Accessibility and stable selectors: 新增 `data-testid="dcc-upload-preview-error"` 和 `data-testid="approval-center-dcc-key-fields"`。
- E2E boundary: 未运行真实审批/签名写入型 Playwright E2E；本任务不创建业务文件、不推进审批、不写签名证据。

## Blockers

- Closeout blocker: 当前工作区已有大量非本任务脏改动且 `int_main` ahead `origin/int_main`，未执行基线提交、cleanup apply 或 push，避免混入其它任务改动。
