# Execution Log: NAS 管理页展示跳过目录（前端）

BDD: 展示成功同步的目录树 -> Given 后端返回目录树和 skipped 列表 / When 用户点击刷新目录 / Then 页面继续展示可访问目录树

BDD: skipped 目录显式展示 -> Given 后端返回 skipped 列表 / When 刷新目录成功 / Then 页面显示被跳过目录路径和原因

RED: node --test scripts\\system-nas-management.test.mjs -> FAIL, 当前页面尚未暴露 skipped 目录结果展示

GREEN: node --test scripts\\system-nas-management.test.mjs -> PASS, 2 tests green，页面已补 skipped 目录展示契约

GREEN: pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish -> PASS
