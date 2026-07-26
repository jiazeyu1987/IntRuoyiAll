# Frontend Feature Evidence

## Feature Goal

隐藏 `eDHR 放行资料限制` 卡片中用户截图红框标注的辅助说明类内容，保留核心配置能力。

## Non-goals

- 不修改后端 API。
- 不修改配置字段、默认值或 hash 保存校验。
- 不改变权限、确认框、接口失败回滚和错误展示。
- 不新增 fallback、mock 或兼容分支。

## Requirements

- `AC-1`: 不渲染顶部辅助说明。
- `AC-2`: 不渲染 `默认关闭` 标签。
- `AC-3`: 不渲染每个开关项的说明文案。
- `AC-4`: 不渲染 `当前配置 hash`。
- `AC-5`: 仍渲染标题、4 个开关名称和开关控件。

## Acceptance

- `AC-1` 至 `AC-5` 全部由任务专用静态合同覆盖。

## UI Entry

- Page: 个人中心配置页。
- Component: `IntRuoyiFronted/src/views/Profile/components/EdhrReleaseDossierRequirementSetting.vue`。

## API And State Contract

- 继续调用 `getEdhrReleaseDossierRequirementSetting` 和 `updateEdhrReleaseDossierRequirementSetting`。
- 后端仍可返回 `configHash`，但前端卡片不再显示该元信息。

## BDD

BDD: 红框说明隐藏 -> Given 金手指用户打开个人中心配置页 / When `eDHR 放行资料限制` 卡片渲染 / Then 不显示顶部辅助说明、默认关闭标签、每个开关项说明和当前配置 hash，同时仍显示标题、4 个开关标签和开关控件。

## Verification

- RED: `node tests/e2e/edhr-release-dossier-requirement-copy-hidden-static.spec.js` 首先失败于顶部辅助说明仍渲染。
- GREEN: `node tests/e2e/edhr-release-dossier-requirement-copy-hidden-static.spec.js`、`node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js`、`node --check tests/e2e/edhr-release-dossier-requirement-setting-real.e2e.js`、`pnpm ts:check` 均通过。
- Wide regression: `pnpm build:local` 900 秒超时，已停止确认属于本次构建的遗留 node 进程；构建未取得 GREEN。
- Responsive/accessibility: 移除说明后沿用现有两列网格；开关仍有可见文本标签。
- Loading/empty/error/permission: 本次不改变加载错误、权限和保存错误路径。

## Blockers

- `pnpm build:local` 宽回归超时；必需聚焦验证已通过。
