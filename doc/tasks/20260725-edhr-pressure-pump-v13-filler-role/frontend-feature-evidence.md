# Frontend Feature Evidence: Filler Select Full Display

## Feature Goal and Non-Goals

- Goal: 批记录表单填写人设置弹窗中，“填写人”选中的人员或角色名称完整显示。
- Non-goal: 不变更填写人 API、角色配置、保存逻辑、权限逻辑或真实业务数据。

## Requirements and Acceptance

- Requirement: 用户截图中 `填写人` 选中项显示为 `压...`，需要显示全。
- Acceptance: “填写人”选择框使用专用加宽布局，选中标签不再使用 Element Plus 默认省略宽度。

## UI Entry and Owned Files

- Entry: `src/views/mes/pro/batchrecordformlist/index.vue` 的 `批记录表单填写人设置` 弹窗。
- Owned files: `src/views/mes/pro/batchrecordformlist/index.vue`、`tests/e2e/edhr-batch-record-form-list-filler-static.spec.js`。

## API Contracts and Data States

- API contract unchanged: 继续使用 `EdhrProcessFormPermissionRuleApi.getByReport` 和 `saveByReport`。
- Data states unchanged: `candidateSourceType`、`candidateSourceIds`、`completionPolicy` 保存结构不变。

## BDD Scenarios

- BDD: filler select displays full selected name -> Given 批记录表单填写人设置弹窗已打开且“填写人”选择了角色或人员, When 选中名称较长例如 `压力泵生产1`, Then “填写人”选择框应完整展示该名称，不得显示为 `压...` 等截断文本。

## RED / GREEN

- RED: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> FAIL, 缺少填写人专用布局类。
- GREEN: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> PASS。

## Responsive and State Checks

- Responsive/layout: 弹窗保持 760px 宽度，三列改为 180px / 280px / 220px 最小布局，中间“填写人”列更宽。
- Accessibility: 标签文案和表单项顺序不变，未新增隐藏控件。
- Loading/empty/error/permission: 不改动加载、空态、错误或权限路径。

## Verification Path

- Component/static verification: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js`。

## Blockers and Follow-Up

- Blocker: 工作区存在其它任务预先产生的未提交改动；本次未提交/推送，避免混入非本任务改动。
