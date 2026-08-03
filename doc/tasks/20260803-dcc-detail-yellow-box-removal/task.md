# 20260803 DCC 详情页黄框内容删除

## Task Goal

删除用户截图中 DCC 受控文件详情页黄框标注的辅助说明、筛选/列设置/视图切换、签核导出/打印和重置列入口，同时保留签核追溯与签名留痕表格数据展示。

## Milestones

- [x] M1: 定位截图对应的 DCC 详情页签核追溯与签名留痕区块。
- [x] M2: 用静态契约先锁定黄框内容必须隐藏且核心表格必须保留。
- [x] M3: 最小修改前端组件，移除目标黄框内容。
- [x] M4: 运行定向静态验证并记录结果。

## Expected Verification

- RED: `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js` 先因黄框控件仍存在失败。
- GREEN: `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js` 通过。
- GREEN: `node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js` 通过。
- REGRESSION: `node tests/e2e/dcc-detail-secondary-lists-standard-template-static.spec.js` 通过。
- REGRESSION: `node tests/e2e/dcc-upload-governance-ux-static.spec.js` 通过。
- REGRESSION: `pnpm ts:check` 通过。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue IntRuoyiFronted/tests/e2e/dcc-detail-signature-view-mode-static.spec.js IntRuoyiFronted/tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js IntRuoyiFronted/tests/e2e/dcc-upload-governance-ux-static.spec.js IntRuoyiFronted/tests/e2e/dcc-detail-secondary-lists-standard-template-static.spec.js doc/tasks/20260803-dcc-detail-yellow-box-removal` 通过。

## Current Status

blocked: 实现、验证与 cleanup apply 已完成；提交/推送因仓库存在大量非本任务脏改动、分支 behind 2 和损坏 target 目录扫描警告而阻塞。

## Closeout Blocker

- `task-closeout-cleanup` preview/apply 均通过，delete 为 `<none>`，blocked 为 `<none>`。
- 提交/推送未执行：当前 `int_main` 分支显示 `behind 2`，且仓库已有大量非本任务前后端/文档脏改动和损坏 target 目录扫描警告；为避免混入无关并行任务，当前只完成实现、验证与 cleanup apply，不做宽泛 baseline、commit 或 push。

## Applicable Gates

- `docs/frontend-development.md#前端截图按钮统一静态契约门禁`: 黄框内按钮/工具控件删除必须先有任务专用静态契约，保留目标区块核心能力，不删除共享组件能力。
- `docs/frontend-development.md#前端同路由多入口分面门禁`: 签核/签名分面已有 `showSignatureTraceSections` 包裹，本任务只调整当前分面可见内容，不改变入口 scope、接口或权限链路。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`: 本任务使用静态合同覆盖截图 UI 删除，未声明真实 Playwright E2E PASS。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`: 否。
- `是否从根因和长期维护角度解决`: 是，直接移除目标可见入口和对应未使用逻辑。
- `是否存在临时补丁或绕过`: 否。
