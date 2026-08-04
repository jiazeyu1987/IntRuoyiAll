# 20260804 DCC Approval Upload View

## Task Goal

审批中心 DCC 文控待办点击“打开”后进入用户上传信息视角，只展示上传页相关只读信息、当前审批处理区和文件预览，不再展示全量追溯、历史、分发、打印、培训、签核留痕等详情区块。

## Milestones

- [ ] M1: 建立任务专用静态契约，先 RED 锁定当前审批处理态缺少上传视角文件预览且仍混入全量追溯区块。
- [ ] M2: 在 DCC 详情页显式建模审批上传视角，复用现有 `handling=approval&from=approval-center` 路由。
- [ ] M3: 在审批上传视角渲染提交范围、文件信息、审批要求、正式文件预览和当前审批处理区。
- [ ] M4: 隐藏审批上传视角中的追溯、历史、分发、打印、培训和签核留痕区块。
- [ ] M5: 运行聚焦契约、相邻回归契约和类型检查，记录 GREEN/REGRESSION 证据。

## Expected Verification

- `node tests/e2e/dcc-approval-upload-view-static.spec.js`
- `pnpm e2e:dcc:approval-center-handling-entry:static`
- `pnpm e2e:dcc:detail-retired:static`
- `pnpm e2e:dcc:detail-lifecycle-timeline:static`
- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `pnpm ts:check`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按同路由多入口分面门禁显式建模审批上传视角，不依赖按钮文案或隐式来源推断。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待读取 `docs/experience-index.md` 后补充适用经验。
