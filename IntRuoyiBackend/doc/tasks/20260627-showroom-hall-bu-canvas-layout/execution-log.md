# 执行记录

BDD: 后台计算 BU 画布布局 -> Given 展柜画布包含多个产品和奖项，产品有 `pipeline_layout` 或空 BU；When 调用 `POST /showroom/hall/calculate-bu-canvas-layout`；Then 后端返回产品按 BU 分组顺序和原 displayOrder 排列后的等大 grid 坐标，奖项坐标保持原样，且不写库。

BDD: 前端预览 BU 布局但不保存 -> Given 用户打开展柜画布弹窗；When 点击“按 BU 排布”；Then 前端调用后端计算接口，用返回 `itemMappings/items` 更新画布预览，不调用保存接口。

BDD: 前端保存仍走现有保存接口 -> Given 用户已预览 BU 布局；When 点击“保存布局”；Then 前端调用现有 `update-item-canvas-layout` 保存当前画布。

BDD: 接口失败暴露真实错误 -> Given 后端计算接口失败；When 用户点击“按 BU 排布”；Then 页面展示错误提示，当前画布不被错误结果覆盖。

RED: mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test -> FAIL, expected reason: `ShowroomApiRuntime.calculateHallBuCanvasLayout(...)` does not exist.

RED: node tests/e2e/showroom-hall-bu-layout-static.spec.js -> FAIL, expected reason: frontend API marker `calculateHallBuCanvasLayout` does not exist.

GREEN: mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test -> PASS, 2 tests.

GREEN: node tests/e2e/showroom-hall-bu-layout-static.spec.js -> PASS.

GREEN: backend-api-evidence validation -> PASS.

GREEN: task-closeout-cleanup preview in backend repo -> PASS, apply skipped to retain evidence file.
