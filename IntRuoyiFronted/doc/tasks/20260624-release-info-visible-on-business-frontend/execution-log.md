# Execution Log

## BDD

BDD: 业务前端版本可见 -> Given 发布脚本在前端 dist 生成 release-info.json When 用户打开测试服/正式服/备份服业务前端 Then 左下角显示当前 releaseTag。

BDD: 业务前端变更可见 -> Given release-info.json 包含 changeSet When 用户点击查看变更 Then 弹窗显示摘要、变更项和源码提交。

BDD: 发布信息缺失暴露 -> Given release-info.json 未生成或不可读 When 用户打开业务前端 Then 页面显示版本信息未生成，不静默隐藏。

## RED

RED: node scripts/release-info-dock-contract.test.mjs -> FAIL，`App.vue` 未挂载 `ReleaseInfoDock`，`src/components/ReleaseInfoDock/ReleaseInfoDock.vue` 不存在。

## GREEN

GREEN: node scripts/release-info-dock-contract.test.mjs -> PASS，2 tests passed。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm build:test -> PASS，输出 `Build successful. Please see dist-test directory`。

GREEN: dist-asset-scan -> PASS，`dist-test/assets/index-*.js` 包含 `release-info.json`。

## BLOCKER

- 暂无。
