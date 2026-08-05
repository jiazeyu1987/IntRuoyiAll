# Verification Report

## Scope

- AC-M09：QA 维护检验规程正式草稿保存、发布、不可变版本、缺首检/巡检/末检发布失败、前端正式保存/发布接入。
- 追加前端范围：QA 页面从一次性直铺展示改为单一 `DCC 项目代码` 下拉选择，选中项目后再展示 Tab、适用范围、检验规则、检验项目和发布检查，并用 `UnifiedListTemplate` 承载规则、项目、发布检查和 PQC 任务预览列表。
- 追加截图黄框范围：路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数、批记录绑定等不再由 QA 页面手工设置；产品绑定工艺路线后由正式工艺路线、路线版本、工序配置、排产配置和批记录配置自动带出。
- 追加手动绑定范围：产品未绑定或需修正工艺路线时，QA 页面支持显式选择已发布/已启用工艺路线，并通过 QA 专用产品-工艺路线绑定 API 保存；后端校验路线存在且有 ACTIVE 版本但不调用产品维护页的启用路线守卫，保存后重新读取当前产品绑定并带出适用范围。
- 本轮截图回归范围：顶部黄框内显示 `DCC 项目代码` 选择内容；不再把项目选择框拆到黄框外的独立卡片。

## Passed

- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS，后端生产代码编译通过。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，前端静态契约确认正式 API 已接入、旧未写入后台提示已移除，QA 页面使用 Tab + `UnifiedListTemplate`，且项目选择区只保留 1 个必填 `DCC 项目代码` 下拉框，不显示旧已配置/待配置列表；同时确认适用范围从正式工艺路线链路自动带出，黄框字段不再提供手工输入，缺正式路线范围时保存/发布被阻断，并支持通过 `saveQaRegulationRouteProductByItem` 手动绑定已发布/已启用工艺路线后重新解析范围。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，本轮新增黄框显示合同已通过，顶部 `ContentWrap data-qa-regulation-dcc-project` 同时包含标题、正式接口提示、`DCC 项目代码` 选择框和加载失败重试区。
- `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs`：PASS，截图“不能选择”回归已由聚焦静态契约覆盖；QA 手动绑定路线选项显式 `:disabled="false"`，不复用产品维护页的启用路线置灰逻辑，保留“可绑定”语义和 QA 专用绑定 API。
- `node tests\e2e\qa-regulation-final-applicability-static.spec.cjs`：PASS，末检不适用依据合同未受黄框布局调整影响。
- `mvn -rf :yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，20 个目标 JUnit 通过，覆盖 QA 新建绑定、修正既有绑定、缺 ACTIVE 版本失败、Controller QA endpoint 和不调用产品维护页 `validateRouteNotEnable`。
- `pnpm ts:check`：PASS，前端 Vue/TypeScript 类型检查通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，frontend feature evidence 有效。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/bug-regression-evidence.md`：PASS，bug regression evidence 有效。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/backend-api-evidence.md`：PASS，backend API evidence 有效。
- 48081 运行态 QA endpoint 探针：PASS，旧运行 Jar 缺少 `save-qa-regulation-route-by-item` 并在截图请求时返回 `NoResourceFoundException`；当前 48081 运行 Jar 已包含该 endpoint，登录态调用同一路径并使用无效 routeId 返回 `1040501000 / 工艺路线不存在`，证明路由已注册且不再是 `请求地址不存在`。
- QA 页面专属排序接线断言：PASS，输出 `PASS QA standard list sort wiring`。
- `docs/frontend-development.md`：UPDATED，已将标准列表系统接入点 88 和显式隐藏筛选 14 的长期门禁证据合并到既有前端规则。
- `docs/backend-development.md` / `docs/experience-index.md`：UPDATED，已将产品维护页已启用路线不可维护和 QA 规程手动绑定允许已发布路线拆成两条门禁。
- `git diff --check -- <AC-M09 实现文件和经验文档>`：PASS，无 whitespace error。

## Blocked

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BLOCKED，`testCompile` 被共享 `target/classes` 缺失阻断；检查时存在其它非本任务 Maven 进程写入同一 `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\target`。
- 限制 `maven.compiler.testIncludes=**/MesQaInspectionRegulationServiceTest.java` 后复跑：BLOCKED，20 分钟超时；期间主工作区仍出现其它非本任务 Maven 测试进程。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：标准 `-am` 修正后复跑两次超时且无新 Surefire 结果；已按任务边界只停止本任务 Maven PID 47976 和 50448，随后用 Maven 建议的 `-rf :yudao-module-mes` 恢复执行并取得 PASS。
- `restart-int-ruoyi-local.ps1 -Component backend`：BLOCKED，Maven package 超时；线程栈命中项目已记录的 Windows `IncrementalBuildHelper.beforeRebuildExecution -> WinNTFileSystem.delete0` target 删除卡住问题。已仅停止本次重启脚本派生 Maven/restart PIDs；当前 48081 由包含 QA endpoint 的后续 hotpatch Jar 正常提供服务。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：BLOCKED，当前系统标准列表模板接入点为 89，而既有合同锁定 88；这是并行接入点计数漂移，不由本轮 QA 顶部黄框布局变更产生。
- `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js`：BLOCKED，当前全局契约被大量既有页面排序 helper 历史缺口阻塞；QA 页面聚焦扫描已确认新增四个标准列表均接入排序 helper。

## Result

blocked：本轮顶部黄框显示回归已修复，`DCC 项目代码` 选择框和加载失败重试区已并回顶部 QA 标题黄框；QA 手动绑定路线候选恢复为正式 `getRouteItemBindingList`，聚焦 QA 静态合同、末检适用性合同和 `pnpm ts:check` 均通过。完整 AC-M09 后端目标 JUnit 仍需共享 Maven target 空闲后复跑；标准列表系统合同当前被并行 89/88 接入点计数漂移阻塞；当前不提交、不推送、不标记 completed。
