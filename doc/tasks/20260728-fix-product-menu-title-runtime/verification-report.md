# 验证报告：动态菜单旧标题修复

## Result

- Status: completed。
- 本地动态菜单数据已修复为 `展厅主数据`。
- 真实页面展开“基础数据”后可见 `展厅主数据`，无可见 `产品主数据`。

## Evidence

- RED: 数据库只读查询目标菜单旧值 `HEX(name)=E4BAA7E59381E4B8BBE695B0E68DAE`。
- RED: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> FAIL，缺少修复迁移。
- GREEN: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
- GREEN: 聚焦 migration policy gate -> PASS，`migrationCount=3`。
- GREEN: 数据库复核目标菜单 `HEX(name)=E5B195E58E85E4B8BBE695B0E68DAE`，路由/权限/组件字段不变。
- GREEN: Playwright 真实页面验证 -> PASS，`visibleNew=["展厅主数据","展厅主数据","展厅主数据"]`，`visibleOld=[]`。
- GREEN: task-closeout-cleanup preview/apply -> PASS，blocked/warnings 均为 `<none>`。
- GREEN: project-experience-consolidation -> PASS，新增中文菜单名称 ASCII 安全迁移门禁和索引路由。
- GREEN: final verification rerun -> PASS，目标静态测试、页签静态测试、聚焦 migration policy gate、数据库 HEX 核验和 `git diff --check` 均通过。
- Commit: implementation `1374f802`。

## Notes

- 官方 `login-preflight` 直接等待 `展厅主数据` 时曾定位到隐藏 span；改用真实页面展开“基础数据”后验证可见菜单项。
- 修复迁移使用 ASCII hex 写入目标中文，避免 MySQL 客户端字符集造成 mojibake。
