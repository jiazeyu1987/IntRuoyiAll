# Execution Log: 20260629-showroom-hall-bu-layout-allow-empty

- `2026-06-29 10:05` 建立后端任务文档，定位报错发生在 `ShowroomApiRuntime.calculateHallBuCanvasLayout(...)`。
- `BDD: hall BU 自动排版在没有产品时保持非产品布局 -> Given 展柜画布只有 AWARD 等非 PRODUCT 元素 / When 调用 calculateHallBuCanvasLayout / Then 不抛出 SHOWROOM_REQUIRED_FIELD_MISSING，并原样返回这些非产品元素。`
- `BDD: hall BU 自动排版在存在产品时仍按 BU 重排 -> Given 展柜画布包含多个 PRODUCT 元素 / When 调用 calculateHallBuCanvasLayout / Then 产品仍按 BU 分组后的顺序与网格布局重排。`
- `RED: mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test -> FAIL, expected reason: calculateHallBuCanvasLayoutShouldKeepAwardsWhenNoProducts 仍抛出 SHOWROOM_REQUIRED_FIELD_MISSING: hall BU layout requires at least one product item。`
- `GREEN: mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test -> PASS`
