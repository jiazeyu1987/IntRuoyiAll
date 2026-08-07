# 导入表单模板弹窗布局优化

## Task Goal

优化“导入表单模板”弹窗的信息层级、字段对齐、上传区域和窄屏适配，解决当前上传区域横向溢出、文件名超出弹窗以及表单视觉拥挤的问题；保持现有导入、升版、校验和错误暴露逻辑不变。

## Milestones

- [x] M1：确认现有弹窗入口、组件、交互契约、设计参考和适用项目规则。
- [x] M2：以专用静态合同记录目标布局并取得 RED 证据。
- [x] M3：完成最小布局与样式调整并取得 GREEN 证据。
- [x] M4：完成类型检查、相邻合同、响应式页面检查和差异检查。
- [x] M5：完成证据归档与任务清理；最终记录进入提交与推送门禁。

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

## Cleanup Candidates

- doc/tasks/20260807-form-template-import-dialog-layout/frontend-feature-evidence.md
- output/playwright/form-template-import-dialog-layout/
- .playwright-cli/console-2026-08-07T02-18-57-747Z.log
- .playwright-cli/page-2026-08-07T02-19-00-036Z.yml
- .playwright-cli/page-2026-08-07T02-28-50-808Z.yml
- .playwright-cli/console-2026-08-07T02-30-39-798Z.log
- .playwright-cli/page-2026-08-07T02-30-41-181Z.yml
- .playwright-cli/page-2026-08-07T02-32-15-599Z.yml
- .playwright-cli/page-2026-08-07T02-33-35-805Z.yml
- .playwright-cli/console-2026-08-07T02-31-34-859Z.log
- .playwright-cli/console-2026-08-07T02-35-57-285Z.log
- .playwright-cli/page-2026-08-07T02-35-58-794Z.yml
- .playwright-cli/page-2026-08-07T02-37-14-576Z.yml
- .playwright-cli/page-2026-08-07T02-40-06-441Z.yml
- .playwright-cli/page-2026-08-07T02-43-27-184Z.png
- .playwright-cli/console-2026-08-07T02-37-13-181Z.log
- .playwright-cli/console-2026-08-07T02-51-39-835Z.log
- .playwright-cli/page-2026-08-07T02-51-41-271Z.yml
- .playwright-cli/page-2026-08-07T02-55-09-494Z.png
- .playwright-cli/console-2026-08-07T02-55-16-805Z.log
- .playwright-cli/page-2026-08-07T02-55-17-986Z.yml
- .playwright-cli/page-2026-08-07T02-56-08-527Z.yml
- .playwright-cli/page-2026-08-07T02-57-26-846Z.yml
- .playwright-cli/page-2026-08-07T02-57-42-241Z.png
- .playwright-cli/page-2026-08-07T02-58-32-384Z.yml

## Current Status

completed：实现、静态合同、类型检查和真实页面桌面/窄屏验证均已通过；任务临时产物清理已完成，最终记录进入提交与推送门禁。
