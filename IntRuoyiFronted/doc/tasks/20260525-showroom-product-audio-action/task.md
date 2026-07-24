# 任务：展厅产品行级语音入口调整

## 任务目标

- 将产品编辑弹框中的 `Generate Audio` 入口提取出来。
- 在产品列表行操作区中，放到“指派”按钮旁边，按钮命名为“语音”。
- 点击“语音”后仍复用现有单个产品语音生成逻辑，不改变后端接口契约。

## 非目标

- 不调整一键语音批量入口。
- 不修改产品发布、基础信息、版本中心、整单指派的业务规则。
- 不新增 mock 数据、测试专用入口或降级路径。

## 前置任务检查

- 当前 worktree 分支：`task/20260525-showroom-product-audio-action`。
- 最近前端任务 `20260525-runtime-control-publish-scope` 状态为 `completed`。
- 已阅读统一前端样式 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次沿用产品表格行内文字操作风格。

## BDD 场景

- BDD: 产品行展示语音入口 -> Given 企宣人员打开展厅产品列表, When 行操作区渲染, Then “语音”按钮显示在“指派”旁边并触发现有单品语音生成事件。
- BDD: 编辑弹框不再承载语音生成 -> Given 企宣人员打开产品基础信息弹框, When 弹框渲染, Then 不再出现 `Generate Audio` 或“生成语音”按钮。
- BDD: 单品语音生成复用现有接口 -> Given 产品行点击“语音”, When 当前产品满足生成条件, Then 前端调用 `ShowroomAdminApi.generateProductNarrationAudio` 并刷新产品列表。

## 里程碑

- [x] M1：创建独立 worktree，建立任务记录和 BDD 场景。
- [x] M2：补齐 RED 静态契约测试，证明当前行级语音入口缺失且弹框仍有 `Generate Audio`。
- [x] M3：实现列表行级“语音”入口，移除编辑弹框入口。
- [x] M4：运行定向测试、类型/静态验证，并记录 GREEN。
- [x] M5：执行 closeout 预览，提交并快进合并本任务改动。

## 预期验证

- RED：`node tests\e2e\showroom-product-row-audio-action.spec.js` 先失败，证明行级“语音”入口缺失、弹框仍含 `Generate Audio`。
- GREEN：同一静态契约通过。
- GREEN：相关展厅产品静态测试通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-showroom-product-audio-action\frontend-feature-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：独立 worktree 创建；前序任务检查；任务文档、BDD 场景和验证计划已建立；RED 静态契约已补齐并失败于行级“语音”入口缺失；行级“语音”入口已实现；编辑弹框 `Generate Audio` 已移除；定向静态测试、类型检查和 eslint 已通过。
- 阻塞与影响：无。非本任务阻塞：`scripts\showroom-admin-frontend.test.mjs` 全量运行仍失败于既有公司角色门禁断言；`tests\e2e\showroom-product-toolbar-layout.spec.js` 仍失败于既有工具栏 `flex-wrap` 断言。

## Current Status

completed

## 最终验证

- GREEN: `node tests\e2e\showroom-product-row-audio-action.spec.js` -> PASS.
- GREEN: `node tests\e2e\showroom-product-whole-assignment.spec.js` -> PASS.
- GREEN: `node scripts\showroom-admin-product-bilingual-tabs.test.mjs` -> PASS.
- GREEN: `node scripts\showroom-admin-product-narration-editor.test.mjs` -> PASS.
- GREEN: `node scripts\showroom-product-narration-action-disabled.test.mjs` -> PASS.
- GREEN: `node scripts\showroom-admin-product-list.test.mjs` -> PASS.
- GREEN: `node --test --test-name-pattern "showroom-admin product editor keeps bilingual product tabs while list owns publish entry" scripts\showroom-admin-frontend.test.mjs` -> PASS.
- GREEN: `node tests\e2e\showroom-product-publish-entry.spec.js` -> PASS.
- GREEN: `node tests\e2e\showroom-product-detail-basic-info.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js <changed files>` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-showroom-product-audio-action\frontend-feature-evidence.md` -> PASS.
- 合并后主工作区验证：上述定向产品测试、`pnpm ts:check`、受影响文件 eslint 与 frontend evidence 校验均已在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 通过。
- Closeout preview: 默认 worktree closeout 预览因脚本检测 `master` 主 worktree 而阻塞；使用 `--worktree-closeout off --extra-keep doc\tasks\20260525-showroom-product-audio-action\frontend-feature-evidence.md` 后预览 ready，删除项为空。

## Cleanup Keep

- `doc/tasks/20260525-showroom-product-audio-action/frontend-feature-evidence.md`
