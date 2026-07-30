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
- PASS：最终真实 E2E `node doc\tasks\20260730-standard-template-list-search-alias\standard-template-list-real.e2e.mjs`，通过顶部真实搜索框搜索 `mes工序`，进入 `/mes/pro/mes-process`，资源接口 HTTP `200` / 业务码 `0` / total `580`，页面无“系统异常”，MES 写请求数 `0`，页面错误数 `0`。

## Runtime Evidence

- 前端 `http://127.0.0.1:8081/` 返回 HTTP 200。
- 后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 后端监听进程归属 `E:\IntRuoyi` 的 `output\runtime\int_main` 运行 Jar，端口为 `48081`。
- 前端监听进程归属 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite dev server，端口为 `8081`。
- 真实页面验证身份：`芋道源码/admin`。
- 真实页面已确认唯一搜索结果：`标准模板列表/mes/pro/mes-process`。
- 最终后端运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-standard-template-e2e-20260730-2115.jar`，SHA256 `C79371D6B1DC445B94D7160BAB53827679DCC54E787ABF85C49FC60F8BE2C089`，新 PID `53040`。
- 最终 E2E 证据：`E:\IntRuoyi\output\playwright\20260730-standard-template-list-search-alias\standard-template-list-evidence.json`。
- 最终截图：`E:\IntRuoyi\output\playwright\20260730-standard-template-list-search-alias\standard-template-list-success.png`。
- 运行态存在一条头像资源 502：`http://test.yudao.iocoder.cn/user/avatar/20251220/blob_1766215463801.jpg`，不属于本次搜索链路。

## Remaining Risk

- 当前分支存在并行提交和非本任务脏改动；本任务未执行提交/推送，避免扩大变更范围。
- 当前最终 `git status --short --branch` 显示 `int_main...origin/int_main [ahead 7]`，并存在其它任务文档的已暂存/未暂存改动；这些状态不归属本次 E2E 验证，未执行提交或推送。

## Cleanup Evidence

- PASS：task-closeout-cleanup preview/apply 均通过。
- 删除：`output/playwright/20260730-standard-template-list-search-alias`。
- 保留：`task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`、`migration-policy-gate.json`。
- PASS：bug regression evidence contract validation。
- 最终 E2E 在 cleanup 后重新生成成功证据，并已加入 `task.md` 的 `Cleanup Keep`：`standard-template-list-evidence.json` 与 `standard-template-list-success.png`。

## Closeout Blocker

- 当前共享分支为 `int_main...origin/int_main [ahead 7]`，并存在非本任务文档改动；本任务停在 `ready_for_closeout`，不执行提交/推送。

## Experience Evidence

- PASS：复发经验已合并至 `docs/frontend-development.md#动态菜单页签重命名门禁`。
- PASS：复发经验已合并至 `docs/e2e-rules.md#Element Plus 下拉选择门禁`，覆盖 `input[placeholder]` 不可靠、优先确认 `input.el-select__input[role="combobox"]` 的真实 E2E 定位规则。
- PASS：`docs/experience-index.md` 已加入可检索关键词。
