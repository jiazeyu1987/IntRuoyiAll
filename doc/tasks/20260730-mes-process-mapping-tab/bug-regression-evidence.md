# MES 工序菜单不可见回归证据

## Bug Summary

- 现象 1：用户反馈 `芋道源码/admin` 当前前端里没有看到 `MES工序`。
- 现象 2：菜单可见后，访问 `MES工序` 页面出现 `系统异常`。
- 期望：`芋道源码/admin` 登录后，在 `MES 系统 > 生产管理` 下看到 `MES工序`，且打开页面不出现 `系统异常`。

## Expected

- `芋道源码/admin` 登录后的动态菜单响应包含 `MES工序`。
- 前端真实页面可以打开 `/mes/pro/mes-process` 并显示 `MES工序` 页面。
- 资源列表接口遇到旧的孤儿路线产品关联时，不应拖垮整页；只读列表应仅展示可解析到正式路线、产品、工序和资源的数据。

## Reproduction / Isolation

- API 复核：使用本机后端 `http://127.0.0.1:48081` 和 `芋道源码/admin` 只读登录态请求 `system/auth/get-permission-info`。
- 结果：权限响应包含 `MES工序`，菜单 `id=5718`，`path=mes-process`，`component=mes/pro/mes-process/index`，`fullPath=/mes/pro/mes-process`，权限集合包含 `mes:pro-mes-process:query`。
- 页面复核：使用本机 Chrome 新上下文登录 `http://127.0.0.1:8081` 后直达 `/mes/pro/mes-process`。
- 结果：页面可见 `MES工序`，侧边栏路径显示 `MES 系统 / 生产管理 / MES工序`，位置在 `工序设置` 和 `工艺流程` 之间。
- 资源列表复核：`GET /admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` 返回业务码 `500`、消息 `系统异常`；后端日志显示 `IllegalStateException: Missing route: 922138`。
- 数据核对：本机库存在 4 条活跃 `mes_pro_route_product` 指向缺失路线 `922138`。

## Root Cause

- 当前代码、运行库菜单、角色授权、租户套餐和新登录态动态菜单均已生效。
- 用户当前浏览器未显示的最可能原因是旧登录态或前端动态菜单缓存未刷新，而不是菜单 SQL、角色权限或前端组件缺失。
- `MES工序` 页面复用 `route-resource` 全量资源读模型；该读模型把所有 `route_product` 都当成可解析主数据，遇到一条已删除或缺失路线的历史关联时直接 `require(route)` 抛出异常，导致只读列表整页返回 `系统异常`。

## RED / GREEN Evidence

- RED: 用户当前浏览器会话反馈未显示 `MES工序`，属于真实使用路径症状。
- GREEN: `get-permission-info` 返回目标菜单和查询权限 -> PASS。
- GREEN: Chrome 新上下文真实登录并打开 `/mes/pro/mes-process` -> PASS。
- RED: `GET /admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` -> FAIL，业务码 `500`，后端日志 `Missing route: 922138`。
- GREEN: `node tests/e2e/mes-pro-route-resource-orphan-static.spec.js` -> PASS，静态合同确认读模型在行组装前过滤无法解析路线或产品的 `route_product`。
- GREEN: `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js` -> PASS，MES 工序只读页面、权限和资源端点契约仍成立。
- GREEN: 本机后端加载修复 Jar 后，登录态 API 请求 `GET /admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` -> HTTP `200`、业务码 `0`、`total=580`、首屏 `20` 行。
- GREEN: Playwright 真实页面打开 `/mes/pro/mes-process` -> `route-resource` 响应业务码 `0`、可见表格 `20` 行、`系统异常` 出现次数 `0`、console error `0`。

## Verification

- `get-permission-info` 只读核验：目标菜单 `id=5718`、`path=mes-process`、`component=mes/pro/mes-process/index`、权限 `mes:pro-mes-process:query` 均存在。
- Chrome 新上下文真实页面核验：`/mes/pro/mes-process` 可打开，页面文本包含 `MES工序`，侧边栏顺序为 `工序设置 / MES工序 / 工艺流程`。
- 网络核验：唯一 HTTP 502 为外部头像资源，不属于目标菜单或列表接口。
- 资源列表核验：修复前目标接口业务码为 `500`；修复后目标接口业务码恢复为 `0`，真实页面不再出现 `系统异常`。
- 本机运行态核验：`48081` 新后端 PID `33108` 使用独立运行 Jar `output\runtime\int_main\backend-mes-process-route-resource-20260730-1757.jar`，`/actuator/health` 返回 `UP`。

## Risk / Scope

- 本轮修复不做数据删除或直接 SQL 修复；以读模型合法数据集合收敛为主，避免孤儿关联拖垮只读页面。
- 若旧浏览器退出重登后仍不可见，需要继续抓取该浏览器当前 token 对应的 `get-permission-info` 响应，核对是否登录到了非 `tenant_id=1` 的另一个 `admin`。

## Blockers

- 当前没有影响 `MES工序` 页面访问的代码或数据库层 blocker。
- 当前用户浏览器仍可能保留旧登录态或旧动态菜单缓存，需要退出重登或硬刷新后确认。
- 目标 JUnit 精确运行存在本任务外未跟踪测试文件阻塞：`MesProMesProcessCatalogSchemaTest.java` 引用不存在的独立 MES 工序目录包；本次未采用该并行方向，已用静态合同、模块 compile、后端 package、登录态 API 和真实页面验证覆盖用户路径。
