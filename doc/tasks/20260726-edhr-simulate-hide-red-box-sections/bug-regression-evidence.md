# Bug Regression Evidence

## Bug Summary

eDHR 模板模拟填写页仍显示用户截图红框中的辅助标题、摘要和规则图例，造成页面信息冗余。

## Expected Behavior

红框区域不显示；返回、模拟填写、模板预览和错误处理保持不变。

## Reproduction

打开 `/mes/pro/feedback/edhr-batch-execution/template-simulate` 并加载任一有效模板，可见红框标注内容。

## Root Cause

页面模板无条件渲染标题、摘要和左侧说明，共享可编辑模板组件也无条件渲染规则图例。

## Regression Test

新增聚焦静态合同，断言页面不再包含对应结构，并断言共享组件通过显式属性控制图例且模拟页传入关闭值。

## RED / GREEN

- RED: 聚焦静态合同首先失败于工序标题仍渲染。
- GREEN: 聚焦隐藏合同、返回合同和 `pnpm ts:check` 通过。

## Risk And Scope

风险集中在共享组件展示开关的默认行为；默认值保持显示，只有模拟页显式关闭。

## Verification

- 聚焦静态合同、返回合同和 `pnpm ts:check` 已通过。
- 全量模拟页静态合同和本地构建存在独立环境/历史合同阻塞。

## Blockers

- 全量模拟页静态合同首个失败为本任务之前存在的旧路由 ID 断言。
- `pnpm build:local` 因 `node_modules` 中锁定依赖目录为空而无法完成。
