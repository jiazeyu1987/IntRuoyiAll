# Execution Log

BDD: 导出后回导奖项一致 -> Given 测试租户存在可导出的奖项并具备封面 / When 用户在产品管理导出 Excel 后直接导入同一文件 / Then 导入结果识别奖项且导入前后奖项关键字段一致。

GREEN: experience-preflight -> PASS，本机 `http://localhost:8081` 与 `http://localhost:48081/actuator/health` 可访问；按 `docs/login-access.md` 使用 `测试租户/aoteman`；不访问远端服务器；API 仅用于最终对比。

RED: node tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js -> FAIL，真实导出回导后奖项快照不一致：`AWARD-001` 的 `nameEn` 被清空，多个奖项 `coverImage` 被重新上传为新日期路径。

BLOCKER: node tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js -> FAIL，后端新 jar 查询 `showroom_hall.canvas_background_image_url`，本机库缺少既有 showroom 迁移，导出响应为 500 JSON 而非 Excel。

GREEN: node --check tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js -> PASS。

GREEN: node tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js -> PASS，真实用户路径登录 `测试租户/aoteman`，产品管理导出后直接回导；导入前后奖项快照一致，`awards=20`，`awardSuccess=46`。

GREEN: task-closeout-cleanup preview -> PASS，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`。
