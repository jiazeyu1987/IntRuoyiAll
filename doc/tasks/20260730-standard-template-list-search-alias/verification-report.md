# Verification Report

## Scope

- 修复“MES工序”旧关键词搜索不到已改名入口“标准模板列表”的回归。
- 修复上轮本地后端构建被过时 `MesProMesProcessCatalogSchemaTest` 阻塞的问题。

## Results

- PASS：前端静态契约 `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`。
- PASS：相邻资源池只读契约 `node tests\e2e\mes-pro-route-resource-orphan-static.spec.js`。
- PASS：工艺路线 MES 工序页签契约 `node tests\e2e\mes-route-mes-process-tab-static.spec.js`。
- PASS：后端 test-compile `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile`。
- PASS：迁移策略门禁，输出 `doc\tasks\20260730-standard-template-list-search-alias\migration-policy-gate.json`。
- PASS：前端类型检查 `pnpm ts:check`。
- PASS：真实页面只读验证，顶部菜单搜索 `mes工序` 返回 `标准模板列表/mes/pro/mes-process`。
- PASS：复发静态契约覆盖动态路由新鲜度，禁止 `RouterSearch` 缓存 `router.getRoutes()`。
- PASS：复发真实页面验证，`芋道源码/admin` 登录后顶部搜索 `mes工序` 返回 `标准模板列表/mes/pro/mes-process`，MES 写请求数为 `0`。

## Runtime Evidence

- 前端 `http://127.0.0.1:8081/` 返回 HTTP 200。
- 后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 后端监听进程归属 `E:\IntRuoyi` 的 `output\runtime\int_main` 运行 Jar，端口为 `48081`。
- 前端监听进程归属 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite dev server，端口为 `8081`。
- 真实页面验证身份：`芋道源码/admin`。
- 真实页面已确认唯一搜索结果：`标准模板列表/mes/pro/mes-process`。
- 运行态存在一条头像资源 502：`http://test.yudao.iocoder.cn/user/avatar/20251220/blob_1766215463801.jpg`，不属于本次搜索链路。

## Remaining Risk

- 当前分支存在并行提交和非本任务脏改动；本任务未执行提交/推送，避免扩大变更范围。
