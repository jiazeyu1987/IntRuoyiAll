# 20260804 DCC Approval Upload View

## Task Goal

审批中心 DCC 文控待办点击“打开”后进入用户上传信息视角，只展示上传页相关只读信息、当前审批处理区和文件预览，不再展示全量追溯、历史、分发、打印、培训、签核留痕等详情区块。

## Milestones

- [x] M1: 建立任务专用静态契约，先 RED 锁定当前审批处理态缺少上传视角文件预览且仍混入全量追溯区块。
- [x] M2: 在 DCC 详情页显式建模审批上传视角，复用现有 `handling=approval&from=approval-center` 路由。
- [x] M3: 在审批上传视角渲染提交范围、文件信息、审批要求、正式文件预览和当前审批处理区。
- [x] M4: 隐藏审批上传视角中的追溯、历史、分发、打印、培训和签核留痕区块。
- [x] M5: 运行聚焦契约、相邻回归契约和类型检查，记录 GREEN/REGRESSION 证据。
- [x] M6: 运行真实审批中心 DCC 待办 E2E，确认上传视角、内嵌预览、追溯区块隐藏和当前审批按钮可见。

## Expected Verification

- `node tests/e2e/dcc-approval-upload-view-static.spec.js`
- `pnpm e2e:dcc:approval-center-handling-entry:static`
- `pnpm e2e:dcc:detail-retired:static`
- `pnpm e2e:dcc:detail-lifecycle-timeline:static`
- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `pnpm e2e:dcc:approval-upload-view:real`
- `pnpm ts:check`

## Current Status

ready_for_closeout

Implementation, static verification, regression verification, type check, and the requested real E2E verification are complete. The current turn did not stage, commit, push, or clean unrelated dirty workspace files.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按同路由多入口分面门禁显式建模审批上传视角，不依赖按钮文案或隐式来源推断。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/e2e-rules.md#DCC 文控审批处理入口门禁`：DCC 审批中心待办必须进入非只读处理态，不能落到 `viewer=1` 只读预览或受控浏览追溯；真实 E2E 必须断言当前任务按钮可见，不能只看处理区文案。
- `docs/frontend-development.md#前端同路由多入口分面门禁`：同一路由多入口复用时必须显式建模 mode/scope，入口 helper 和详情页渲染都要能证明目标范围与非目标范围隔离。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态契约完成 RED/GREEN，避免无关历史大契约影响当前行为判断。
- `docs/e2e-rules.md#DCC 升版发布 UX 闭环门禁`：受控浏览/追溯路径仍需保留版本历史、发布落位和签核证据，不得因审批上传视角隐藏这些正式追溯入口。
