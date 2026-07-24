# DCC 项目代码关联文件 pageSize 超限执行日志

## BDD

BDD: 关联文件请求不超过后端分页上限 -> Given DCC 项目代码详情有关联文件, When 打开详情并加载关联文档, Then 前端请求 `/controlled-files/page` 的 `pageSize` 不超过 200。

BDD: 右侧文件表分页保持局部刷新 -> Given 三列关联文档已加载, When 用户切换右侧文件页码, Then 第一列阶段和第二列文件类型分组不因右侧页码切换重新刷新。

## TDD Evidence

- RED: `node tests/e2e/dcc-project-code-associated-three-column-static.spec.js` -> FAIL，断言 `DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 200` 失败，当前代码仍使用 10000。
- GREEN: `node tests/e2e/dcc-project-code-associated-three-column-static.spec.js` -> PASS，关联文档导航请求 pageSize 固定为 200。
- GREEN: `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-project-code-basic-data-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## Verification

- 已确认关联文档导航请求不再超过后端分页上限 200。
- 已确认三列关联文档静态契约仍通过。
- 已确认项目代码识别和基础数据静态契约仍通过。
- 已确认 TypeScript relaxed 校验通过。
