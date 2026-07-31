# Bug Regression Evidence: Filler Select Text Truncation

## Bug Summary and Expected Behavior

- Bug: 批记录表单填写人设置弹窗中，“填写人”选中角色名称在多选标签内显示为省略文本。
- Expected: “填写人”选中人员或角色名称完整显示，例如 `压力泵生产1` 不应显示为 `压...`。

## Reproduction

- Path: 批记录表单列表页打开 `批记录表单填写人设置` 弹窗，选择较长的角色或人员作为填写人。
- Evidence: 用户截图显示 `填写人` 选择框内选中标签被截断。

## Root Cause

- 弹窗内三个字段使用 `repeat(3, minmax(0, 1fr))` 三等分网格，`填写人` 列扣除 108px label 后输入区域过窄。
- Element Plus 多选标签文本仍应用默认省略宽度，进一步导致角色名显示为截断文本。

## Regression Test

- Updated: `tests/e2e/edhr-batch-record-form-list-filler-static.spec.js`。
- Coverage: 断言填写人字段拥有专用布局类、中间列更宽、选中标签文本取消默认省略宽度。

## RED / GREEN

- RED: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> FAIL, 当前实现缺少专用布局类。
- GREEN: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> PASS。

## Verification

- Verification: `node tests/e2e/edhr-batch-record-form-list-filler-static.spec.js` -> PASS。
- Verification: 证据覆盖弹窗专用布局类、中间列宽和多选标签取消默认省略宽度。

## Risk and Regression Scope

- Risk: 仅影响该弹窗内填写人配置行的列宽和多选标签显示。
- Regression scope: 不改变候选人加载、表单保存、完成策略、接口或权限逻辑。

## Blockers and Follow-Up

- Blocker: 工作区存在其它任务预先产生的未提交改动；未执行提交/推送，避免混入非本任务改动。
