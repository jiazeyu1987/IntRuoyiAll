# DCC 审批操作区左栏布局调整

## Task Goal

将 DCC 受控文件审批处理页底部的审批要求、当前任务与审批操作按钮，移动到上传提交信息左侧文件信息栏下方，填充用户截图黄框位置；右侧附件预览保持不变。

## Milestones

- [ ] M1：确认页面入口、现有布局契约与适用经验门禁。
- [ ] M2：新增聚焦静态合同并记录 RED。
- [ ] M3：完成审批操作区左栏布局调整并记录 GREEN。
- [ ] M4：完成相邻回归、证据校验、提交、推送与收尾。

## Expected Verification

- `node tests/e2e/dcc-approval-upload-view-static.spec.js`
- 新增 DCC 审批操作区左栏布局聚焦静态合同
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-dcc-approval-action-panel-left-column/frontend-feature-evidence.md`
- `git diff --check`

## Applicable Experience Gates

- 待从 `docs/experience-index.md` 命中的 DCC 审批中心处理入口与前端静态合同门禁补充。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接调整正式审批处理页 DOM 布局，不复制动作状态或增加页面级旁路。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress

任务已建立，正在核对适用门禁与当前工作区基线。
