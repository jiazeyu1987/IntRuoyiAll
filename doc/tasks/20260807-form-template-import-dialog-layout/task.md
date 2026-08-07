# 导入表单模板弹窗布局优化

## Task Goal

优化“导入表单模板”弹窗的信息层级、字段对齐、上传区域和窄屏适配，解决当前上传区域横向溢出、文件名超出弹窗以及表单视觉拥挤的问题；保持现有导入、升版、校验和错误暴露逻辑不变。

## Milestones

- [x] M1：确认现有弹窗入口、组件、交互契约、设计参考和适用项目规则。
- [ ] M2：以专用静态合同记录目标布局并取得 RED 证据。
- [ ] M3：完成最小布局与样式调整并取得 GREEN 证据。
- [ ] M4：完成类型检查、相邻合同、响应式页面检查和差异检查。
- [ ] M5：完成证据归档、任务清理、提交与推送。

## Expected Verification

- `node tests/e2e/form-template-import-dialog-layout-static.spec.js`
- `node tests/e2e/form-center-static.spec.js`
- `pnpm ts:check`
- Playwright 真实页面检查：桌面与窄屏弹窗无横向溢出，上传区和已选文件名保持在弹窗内容区内。
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260807-form-template-import-dialog-layout/frontend-feature-evidence.md`

## Applicable Experience Gates

- `docs/frontend-development.md#前端截图样式块静态契约门禁`：合同必须锁定目标弹窗选择器与布局块，禁止跨文件宽泛匹配。
- `docs/frontend-development.md#前端参考页面像素级布局比对门禁`：真实页面需按桌面和窄屏尺寸检查布局边界，不以单次目测替代可重复证据。
- `docs/frontend-development.md#前端静态合同隔离门禁`：使用任务专用最小静态合同，避免无关大合同失败污染当前结论。

## Design Constraints Check / 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；在弹窗自身建立稳定的响应式布局约束，保持组件和业务契约不变。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260807-form-template-import-dialog-layout/task.md
- doc/tasks/20260807-form-template-import-dialog-layout/execution-log.md
- doc/tasks/20260807-form-template-import-dialog-layout/verification-report.md

## Current Status

in_progress：用户已授权修正 6 个任务外文档的 EOF 空行；脏工作区基线提交 `35595ee9f` 已完成，正在建立当前弹窗布局的 RED 合同。
