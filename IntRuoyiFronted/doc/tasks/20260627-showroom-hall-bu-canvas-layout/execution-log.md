# 执行记录

BDD: 前端预览 BU 布局但不保存 -> Given 用户打开展柜画布弹窗；When 点击“按 BU 排布”；Then 前端调用后端计算接口，用返回 `itemMappings/items` 更新画布预览，不调用保存接口。

BDD: 前端保存仍走现有保存接口 -> Given 用户已预览 BU 布局；When 点击“保存布局”；Then 前端调用现有 `update-item-canvas-layout` 保存当前画布。

BDD: 接口失败暴露真实错误 -> Given 后端计算接口失败；When 用户点击“按 BU 排布”；Then 页面展示错误提示，当前画布不被错误结果覆盖。

RED: node tests/e2e/showroom-hall-bu-layout-static.spec.js -> FAIL, expected reason: frontend API marker `calculateHallBuCanvasLayout` does not exist.

GREEN: node tests/e2e/showroom-hall-bu-layout-static.spec.js -> PASS.

GREEN: pnpm ts:check with NODE_OPTIONS=--max-old-space-size=8192 -> PASS.

GREEN: frontend-feature-evidence validation -> PASS.

GREEN: task-closeout-cleanup preview in frontend repo -> PASS, apply skipped to retain evidence file.

BLOCKER: pnpm build:local -> unrelated DCC lint error in `src/views/dcc/controlled-file/approval-tasks/index.vue`: `vue/valid-template-root`.
