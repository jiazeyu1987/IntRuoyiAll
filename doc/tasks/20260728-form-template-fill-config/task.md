# 表单中心模板预览区新增填写配置

## Task Goal

在表单中心模板右侧预览工具栏新增“填写配置”按钮，交互能力对齐批记录表单页签的填写配置，但数据归属于表单中心模板自身 `jimuSchemaJson`，不引入批记录 `reportId`、批记录路由或 MES 批记录保存接口。

## Milestones

- [x] 启动门禁：读取前端、E2E、任务收尾、PowerShell/Git、编码和前端功能交付规则。
- [x] 脏工作区隔离：将任务开始前既有脏改动独立提交为基线提交 `7ee56ab4`。
- [x] RED：新增表单中心填写配置静态合同，先证明当前缺少入口和模板自身保存契约。
- [x] GREEN：实现表单中心“填写配置”入口、模板自身弹窗和 `jimuSchemaJson` 合并保存。
- [ ] REGRESSION：新增静态合同、三按钮静态合同、独立按钮静态合同、批记录填写配置合同和 `pnpm ts:check` 已通过；`form-center-static` 存在相邻路由 `activeMenu` 既有 blocker。
- [ ] CLOSEOUT：更新证据、执行经验沉淀、cleanup preview/apply、提交并推送。

## Expected Verification

- `node tests/e2e/form-template-fill-config-static.spec.js`
- `node tests/e2e/form-center-static.spec.js`
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `pnpm ts:check`
- 真实页面 E2E 仅在本地前后端、登录账号、测试租户和可写草稿模板都满足时执行；缺任一前置则记录 blocker，不用 API-only 冒充通过。

## Applicable Gates

- 表单模板三按钮领域边界门禁：表单模板与批记录表单没有直接关系；按钮只能使用 `templateId + versionNo + jimuSchemaJson` 等当前模板上下文，禁止绑定批记录 `reportId` 或跳转 MES 批记录路由。
- 前端静态契约隔离门禁：新增当前需求专用静态合同并记录 RED/GREEN；不得修改无关大合同绕过历史失败。
- 前端保存链路重复错误提示门禁：模板填写配置保存失败必须展示真实错误，不吞异常、不默认成功。
- 脏工作区基线门禁：任务开始前既有脏改动已独立提交为 `7ee56ab4`，当前任务实现不得混入该基线变更。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，新增表单中心专用填写配置弹窗，复用批记录式配置交互和共享规则工具，并保留表单中心自身数据归属。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

Implementation complete. Verification has two blockers: `form-center-static` fails on existing policy route `activeMenu` expectation, and real E2E is blocked because local backend `127.0.0.1:48081` is not listening.

2026-07-28 lint overlay repair: fixed `vue/no-dupe-keys` for `FormTemplateFillConfigDialog.vue` by renaming only the internal editable `assistRows` state to `editableAssistRows`. Targeted ESLint, static contract, and `pnpm ts:check` pass; broader task status remains `in_progress` because the pre-existing blockers above are still outside this lint repair.
