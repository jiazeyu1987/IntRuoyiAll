# 填写配置弹窗左右重排

## Task Goal

按截图要求重排“填写配置”弹窗：左侧表格预览区域从红框扩大到黄框宽度，右侧所有规则、字段、辅助行、保存操作等配置内容集中放入蓝框侧栏；保持现有 API、路由、权限、保存数据契约不变，并修复重排后 `FormTemplateFillConfigDialog.vue` 在 Vite HMR 中出现的 Vue 模板结束标签错误。

## Milestones

- [x] 启动门禁：读取前端、UI 复刻、E2E/本地运行、任务收尾、PowerShell 编码与编排规则。
- [x] 脏工作区隔离记录：当前工作区存在大量并行脏改，本任务只修改填写配置布局、相关静态合同与任务证据。
- [x] RED：记录 Vite HMR overlay 复现的 Vue 模板结束标签错误，并补充 SFC 编译静态合同。
- [x] GREEN：调整弹窗模板与样式，让表格占左侧黄框主区域，所有字段、辅助行、填写人和操作按钮进入右侧蓝框侧栏，并规范 `main`/`aside` 闭合层级。
- [x] REGRESSION：运行目标静态合同、相邻静态合同、Vite 同源 Vue 模板编译、组件 lint、类型检查。
- [x] CLOSEOUT-CLEANUP：cleanup preview/apply 已完成；保留 `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`、`frontend-feature-evidence.md`，无删除、无阻塞、无告警。
- [ ] COMMIT/PUSH：实现提交 `ec63b062` 已存在于本地并领先 `origin/int_main`；最终任务记录仍未提交/推送，因为共享 `int_main` 工作区存在大量无关并行脏改。

## Expected Verification

- `node tests/e2e/form-template-fill-config-static.spec.js`
- Vite 同源 `@vue/compiler-sfc` 模板编译检查 `FormTemplateFillConfigDialog.vue`
- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/form-center-static.spec.js`
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：用聚焦合同锁定本次布局，不通过无关宽合同绕过。
- UI 复刻边界：只改真实前端组件/样式，保护 API wrapper、后端、DTO/schema、路由、mock/种子数据不变。
- E2E 真实路径门禁：若执行真实页面截图验证，必须使用本地真实前端入口和真实登录路径；缺前置则记录 blocker，不用 API-only 冒充。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，重排组件结构和样式，使主预览与配置侧栏职责清晰。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

Implementation and targeted verification are complete for the two-panel fill-config layout and the Vite missing-end-tag overlay guard. Real browser screenshot verification is blocked by local login response timeout, so no API-only or mock substitute was used. Cleanup apply kept all task evidence and deleted nothing. Implementation commit `ec63b062` contains only the component and static contract changes. Final task records remain uncommitted/unpushed because the shared `int_main` workspace still contains unrelated concurrent dirty changes; they were not staged, committed, reverted, or overwritten by this task.

## Cleanup Keep

- doc/tasks/20260728-fill-config-two-panel-layout/bug-regression-evidence.md
- doc/tasks/20260728-fill-config-two-panel-layout/frontend-feature-evidence.md
