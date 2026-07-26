# Task: 隐藏导入 Word 弹窗表单类型

## Task Goal

隐藏批记录表单页“导入 Word”弹窗中的“表单类型”整行；内部继续固定使用 `MAIN` 批记录类型，不改变产品名称、Word 文件、预检和导入流程。

## Milestones

- [x] 建立任务记录并确认截图目标。
- [x] 写入 BDD 场景并完成 RED 静态契约。
- [x] 隐藏表单类型整行并同步相关测试。
- [x] 完成聚焦回归和真实页面只读验证。
- [x] 完成清理、提交和推送。

## Expected Verification

- `node tests/e2e/mes-batch-record-word-import-default-main-static.spec.js`
- `node tests/e2e/batch-record-form-import-prereq-static.spec.js`
- `node tests/e2e/batch-record-word-dcc-project-select-static.spec.js`
- `pnpm ts:check`
- Playwright 真实页面打开“导入 Word”弹窗，确认“表单类型”不可见且“产品名称”“Word 文件”仍可见。

## Current Status

completed

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：使用聚焦静态契约完成本需求 RED/GREEN，不修改无关宽合同来绕过既有失败。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：同步更新仍依赖“表单类型”下拉的真实 E2E 脚本步骤。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接停止渲染不需要的表单项，同时保留明确的 `MAIN` 默认类型契约。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `.runtime/hide-word-import-form-type-backend.err.log`
- `.runtime/hide-word-import-form-type-backend.out.log`
- `.playwright-cli/console-2026-07-26T15-03-48-151Z.log`
- `.playwright-cli/page-2026-07-26T15-03-49-344Z.yml`
- `.playwright-cli/page-2026-07-26T15-04-24-852Z.yml`
- `.playwright-cli/console-2026-07-26T15-04-58-516Z.log`
- `.playwright-cli/page-2026-07-26T15-04-59-099Z.yml`
- `.playwright-cli/page-2026-07-26T15-06-31-225Z.yml`
- `.playwright-cli/page-2026-07-26T15-08-35-484Z.png`

## Cleanup Keep

- `doc/tasks/20260726-hide-word-import-form-type/frontend-feature-evidence.md`
