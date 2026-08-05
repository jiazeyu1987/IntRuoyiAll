# QA 规程版本与发布按钮标题栏布局

## Task Goal

将 QA 规程的“版本”“生效日期”和“发布规程”按钮移动到顶部 `DRAFT` 状态旁边，保留现有发布校验、正式发布 API 和错误提示。

## Milestones

- [x] M1：确认当前标题栏、总览版本字段和隐藏发布区入口。
- [x] M2：新增聚焦静态契约并取得 RED。
- [x] M3：完成标题栏布局调整并取得 GREEN。
- [x] M4：完成相邻静态回归、类型检查和证据校验。
- [x] M5：完成任务清理、提交与推送。

## Expected Verification

- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`
- `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs`
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-version-publish-header/frontend-feature-evidence.md`
- `git diff --check -- <task-owned-paths>`

## Applicable Gates

- 标题栏控件位置必须使用任务专用静态契约先 RED 后 GREEN，不依赖截图目测。
- 版本号和生效日期仍绑定 `qaRegulationDraft`，发布按钮仍调用 `runQaPublishPrecheck`，不得新增 fallback、默认成功或绕过发布完整性检查。
- 顶部仍只显示“总览 / 检验规则 / 检验项目”三个页签，不恢复“发布检查”页签。
- 当前共享分支存在并行脏改动；只修改并暂存本任务页面、测试和任务证据，不触碰其它任务文件。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；将已失去可见入口的发布动作放回正式标题栏，并保持单一版本字段绑定。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

版本号、生效日期、`DRAFT` 状态和“发布规程”按钮已集中到顶部标题栏动作区；总览和隐藏发布区的重复入口已移除。聚焦静态契约、相邻回归、`pnpm ts:check` 和 evidence validator 均通过；任务中间 evidence 已按 cleanup 规则清理，任务实现提交为 `096651841`。真实页面只读核对被现有本机会话登录超时阻断。
