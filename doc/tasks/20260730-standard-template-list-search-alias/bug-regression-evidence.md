# Bug Regression Evidence

## Bug Summary

- “MES工序”入口按用户要求改名为“标准模板列表”后，顶部菜单搜索继续只匹配路由标题和路径，用户输入旧关键词 `mes工序` 时找不到该入口。
- 上轮遗留的独立 MES 工序目录测试引用不存在的 DO 包，导致 `yudao-module-mes` test-compile 失败并阻塞本地后端重启。

## Expected Behavior

- 页面和正式入口继续以“标准模板列表”为主标题。
- 用户仍可用旧关键词 `mes工序` 搜索到同一路由 `标准模板列表/mes/pro/mes-process`。
- 后端构建不应被与当前“复用工艺路线资源模型”方向冲突的过时独立目录测试阻塞。

## Reproduction

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`：新增别名契约后先失败，提示缺少 `ROUTER_SEARCH_ALIASES`。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile`：先失败于 `cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess` 包不存在。

## Root Cause

- 菜单搜索组件只按 `route.meta.title` 与 `route.path` 匹配；标题从“MES工序”改为“标准模板列表”后，旧关键词没有任何搜索索引。
- `MesProMesProcessCatalogSchemaTest` 属于早先“新建独立 MES 工序目录表”的方向，当前实现已改为复用已有工艺路线资源读模型，对应 DO 和 SQL 不存在。

## Fix

- 在 `IntRuoyiFronted\src\components\RouterSearch\index.vue` 增加路由搜索别名表：`/mes/pro/mes-process -> MES工序`，并统一搜索文本大小写，使 `mes工序` 可匹配。
- 在 `IntRuoyiFronted\tests\e2e\mes-pro-mes-process-readonly-static.spec.js` 增加别名静态契约。
- 删除过时的 `MesProMesProcessCatalogSchemaTest.java`，避免它继续编译不存在的独立目录模型。

## RED

- RED: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> FAIL，缺少搜索别名。
- RED: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile` -> FAIL，缺少 `mesprocess` DO 包。

## GREEN

- GREEN: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-resource-orphan-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 真实页面只读验证，`芋道源码/admin` 登录后搜索 `mes工序`，结果包含 `标准模板列表/mes/pro/mes-process`。

## Verification

- 静态契约、后端 test-compile、相邻只读资源契约、迁移策略门禁、TypeScript 检查和真实页面只读搜索路径均已通过。

## Risk And Regression Scope

- 风险低：只给一个已重命名路由补搜索别名，不改变页面标题、菜单 SQL、权限、数据接口或写入行为。
- 回归范围覆盖：菜单搜索、只读标准模板列表、工艺路线 MES 工序页签、资源池孤儿关系只读过滤、MES 后端 test-compile。

## Blockers And Follow-up

- 当前分支有并行提交和非本任务脏改动，本任务不做提交/推送，避免混入其它任务范围。

## Reopened Regression 2026-07-30

- 用户反馈：`芋道源码/admin` 中仍然无法通过 `mes工序` 搜索到标准模板列表。
- 新增预期：搜索组件不能缓存登录前的静态路由列表，必须在过滤、历史解析和跳转时读取当前 Vue Router 的最新动态路由。

## Reopened Root Cause

- `RouterSearch` 在 setup 初始化阶段执行 `const routers = router.getRoutes()`，该快照可能早于 admin 登录后的动态菜单路由注册。
- 搜索别名已经存在，但搜索过滤和历史路径解析仍基于旧路由快照，所以真实运行态可能搜不到后注册的 `/mes/pro/mes-process`。

## Reopened RED

- RED: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> FAIL，预期原因：静态契约禁止缓存 `router.getRoutes()`，当前代码仍存在 `const routers = router.getRoutes()`。

## Reopened GREEN

- GREEN: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 真实页面只读验证，`芋道源码/admin` 登录后搜索 `mes工序`，结果包含 `标准模板列表/mes/pro/mes-process`，MES 写请求数为 `0`。
