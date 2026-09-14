# Bug Regression Evidence

## Bug Summary

`eDHR 放行资料限制` 卡片显示过多辅助说明，包括顶部说明、默认关闭标签、开关说明和当前配置 hash，与用户截图要求不符。

## Expected Behavior

红框里的说明类内容不显示；核心标题、开关名称、开关控件、确认保存和错误处理保持不变。

## Reproduction

打开个人中心配置页并查看 `eDHR 放行资料限制` 卡片，可见截图红框标注的说明类内容。

## Root Cause

组件模板无条件渲染辅助说明、默认关闭标签、每项 description 和 config hash 元信息。

## Regression Test

新增任务专用静态合同，断言组件模板不包含已废弃的说明节点，同时保留开关项、接口调用和确认保存链路。

## RED / GREEN

- RED: `node tests/e2e/edhr-release-dossier-requirement-copy-hidden-static.spec.js` 首先失败于顶部辅助说明仍渲染。
- GREEN: 聚焦隐藏合同、既有配置合同、真实 E2E 语法检查和前端类型检查均通过。

## Verification

- `node tests/e2e/edhr-release-dossier-requirement-copy-hidden-static.spec.js` -> PASS。
- `node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js` -> PASS。
- `node --check tests/e2e/edhr-release-dossier-requirement-setting-real.e2e.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `pnpm build:local` -> TIMEOUT，未取得 GREEN。

## Blockers

- `pnpm build:local` 宽回归超时；必需聚焦验证已通过。
