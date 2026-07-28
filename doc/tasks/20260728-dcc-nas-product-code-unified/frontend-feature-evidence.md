# Frontend Feature Evidence

## Feature

- Goal: DCC/NAS 前端产品编号统一为 DCC 项目代码来源，用户选择 DCC 项目后只读显示自动生成的产品编号。
- Non-goals: 不改 MDM 产品主数据管理模块，不改展厅链路，不删除历史响应字段 `productMasterId`。
- UI entry points: DCC 上传页 `/dcc/controlled-file/upload`、DCC 外来文件评审页、受控文件元数据弹窗、系统 NAS 导入/转移弹窗。
- Owned files: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`、`submitter.ts`、`external-review/index.vue`、`shared/ControlledFileMetadataDialog.vue`、`src/views/system/nas/index.vue`、`src/api/dcc/controlledFile/workflow.ts`、`tests/e2e/dcc-nas-product-code-unified-static.spec.js`。

## Acceptance

- DCC/NAS 前端不得出现用户可见“产品主数据”选择或 DCC `/product-options` 调用。
- DCC/NAS 写请求以 `dccProjectCodeId` 为项目编号来源，`productMasterId` 只能显式清空为 `null`，不接受数字输入类型。
- 产品编号输入框只读，值来自选中 DCC 项目的 `projectCode`。
- 历史响应 VO 中 `productMasterId` 仍允许读取展示，不作为写入来源。

## BDD

- `BDD: DCC/NAS 前端不再选择产品主数据 -> Given 用户打开 DCC 上传、外来评审、元数据弹窗或 NAS 转移 / When 需要产品编号 / Then 页面提供 DCC 项目选择，产品编号只读自动生成，且不加载 product-options。`
- `BDD: DCC/NAS 写请求清空历史字段 -> Given 用户提交 DCC/NAS 写请求 / When 前端构造 payload / Then productMasterId 固定为 null，dccProjectCodeId 才是权威项目来源。`

## RED

- `RED: pnpm ts:check -> FAIL, 上传页表单提交边界中 reactive productMasterId 推导为 unknown，不能满足 UploadFormDraft productMasterId:null。`
- `RED: static contract design -> 旧实现存在产品主数据选项接口和数字 productMasterId 写入类型，不能证明统一口径。`

## GREEN

- `GREEN: pnpm e2e:dcc:nas-product-code-unified:static -> PASS。`
- `GREEN: pnpm e2e:dcc:upload-product-autofill:static -> PASS。`
- `GREEN: pnpm e2e:dcc:product-category-rule:static -> PASS。`
- `GREEN: pnpm ts:check -> PASS。`
- `GREEN: readonly Playwright upload path -> PASS, 选择 DCC 项目后产品编号自动填充为 IDI，输入框 readonly，product-options 调用 0，DCC 写请求 0。`

## Verification

- Responsive/layout: 本任务沿用既有 Element Plus 表单布局，不做视觉重构。
- Accessibility/loading/empty/error: DCC 项目下拉保留 loading、clearable、filterable、remote；加载失败仍显示真实错误消息。
- Permission: 上传页真实登录 `芋道源码/admin` 可进入 `/dcc/controlled-file/upload` 并显示 DCC 项目选择；写入权限未在 admin 基线租户上验证。
- E2E path: Playwright 真实登录本机 `http://127.0.0.1:8081`，进入上传页并选择真实 DCC 项目完成只读交互验证。

## Blockers

- None. 写入型真实 E2E 已通过，测试租户上传后已撤回并删除已撤回流程。
