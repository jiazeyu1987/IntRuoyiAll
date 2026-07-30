# 标准模板列表搜索别名与后端启动 block 修复

## Task Goal

- 保留用户要求的入口名称“标准模板列表”，同时让一线/业务仍可通过旧关键词“MES工序”找到该页签。
- 修复上轮遗留的本地后端启动 block，避免过时的独立 MES 工序目录测试阻塞标准本地后端构建。

## Milestones

1. 复现并锁定“mes工序”搜索不到“标准模板列表”的根因。
2. 用最小契约测试覆盖“标准模板列表 + MES工序搜索别名”的预期行为。
3. 修复本地后端启动 block，并完成定向前后端验证。
4. 记录验证证据、风险和收尾状态。

## Expected Verification

- 前端静态契约证明页面标题仍为“标准模板列表”，菜单正式配置保留“MES工序”搜索入口。
- 后端阻塞测试不再引用不存在的独立目录包。
- 本地菜单 SQL 使用 UTF-8 HEX 安全写入并通过迁移门禁。
- 定向前端静态契约、TypeScript 检查、后端重启或构建验证通过；若运行态前置缺失则记录 fail-fast blocker。

## Current Status

ready_for_closeout

## Applicable Gates

- 动态菜单页签重命名门禁：入口主标题保持“标准模板列表”，不把业务列名“MES工序名称 / MES工序编码”扩大改名。
- 前端静态契约隔离门禁：用 `mes-pro-mes-process-readonly-static.spec.js` 覆盖本次搜索别名与只读页面契约。
- 中文菜单名称 ASCII 安全迁移门禁：菜单 SQL 仍使用 `CONVERT(UNHEX(... ) USING utf8mb4)`，并用依赖链 migration policy gate 复验。
- 本地后端 Maven test-compile blocker 门禁：过时独立目录测试引用不存在包时必须移除冲突测试，不能用跳过 test-compile 代替修复。

## Verification Summary

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`：RED 命中缺少 `ROUTER_SEARCH_ALIASES`，GREEN 后 PASS。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile`：RED 命中 `MesProMesProcessCatalogSchemaTest` 不存在包，移除过时测试后 PASS。
- 真实只读 Playwright 路径：`芋道源码/admin` 登录后搜索 `mes工序`，返回 `标准模板列表/mes/pro/mes-process`。
- `pnpm ts:check`、相邻静态契约和菜单 migration policy gate 均 PASS。
- 复发修复：静态契约 RED 命中 `RouterSearch` 缓存 `router.getRoutes()`；改为 `getSearchRoutes()` 实时读取最新动态路由后 GREEN。
- 复发复验：`芋道源码/admin` 真实登录后，顶部搜索 `mes工序` 返回 `标准模板列表/mes/pro/mes-process`，MES 写请求数为 0。

## Reopened Regression

- 2026-07-30 用户反馈：在 `芋道源码/admin` 运行态中仍然搜索不到 `mes工序`。
- 新增复发假设：搜索组件初始化时缓存 `router.getRoutes()`，登录后动态菜单路由追加到 Vue Router 后，搜索下拉仍使用旧路由快照。

## Closeout Status

- 当前源码修复和定向验证已完成。
- Closeout 仍被共享分支状态阻塞：`int_main` 当前 `ahead 14, behind 8`，且存在非本任务脏改动；本任务不执行提交/推送，避免混入并行任务范围。

## Cleanup Keep

- doc/tasks/20260730-standard-template-list-search-alias/bug-regression-evidence.md
- doc/tasks/20260730-standard-template-list-search-alias/migration-policy-gate.json


## Closeout Blocker

- 当前 `int_main` 分支已有并行任务提交推进到 `ahead 8`，且仍有非本任务脏改动；本任务不执行提交/推送，避免混入并行任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按动态菜单正式名称/搜索数据源修复入口可发现性，并移除与当前复用路线资源模型冲突的过时测试。
- `是否存在临时补丁或绕过`：否。
