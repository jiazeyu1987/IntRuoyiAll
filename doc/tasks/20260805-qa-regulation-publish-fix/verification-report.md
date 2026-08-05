# Verification Report

## Scope

- AC-M09：QA 维护检验规程正式草稿保存、发布、不可变版本、缺首检/巡检/末检发布失败、前端正式保存/发布接入。
- 追加前端范围：QA 页面从一次性直铺展示改为单一 `DCC 项目代码` 下拉选择，选中项目后再展示 Tab、适用范围、检验规则、检验项目和发布检查，并用 `UnifiedListTemplate` 承载规则、项目、发布检查和 PQC 任务预览列表。
- 追加截图黄框范围：路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数、批记录绑定等不再由 QA 页面手工设置；产品绑定工艺路线后由正式工艺路线、路线版本、工序配置、排产配置和批记录配置自动带出。
- 追加手动绑定范围：产品未绑定或需修正工艺路线时，QA 页面支持显式选择工艺路线并通过正式产品-工艺路线绑定 API 保存，保存后重新读取当前产品绑定并带出适用范围。

## Passed

- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS，后端生产代码编译通过。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，前端静态契约确认正式 API 已接入、旧未写入后台提示已移除，QA 页面使用 Tab + `UnifiedListTemplate`，且项目选择区只保留 1 个必填 `DCC 项目代码` 下拉框，不显示旧已配置/待配置列表；同时确认适用范围从正式工艺路线链路自动带出，黄框字段不再提供手工输入，缺正式路线范围时保存/发布被阻断，并支持通过 `saveRouteProductByItem` 手动绑定工艺路线后重新解析范围。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：PASS，系统标准列表模板接入点 88 个、显式隐藏筛选列表 14 个。
- `pnpm ts:check`：PASS，前端 Vue/TypeScript 类型检查通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，frontend feature evidence 有效。
- QA 页面专属排序接线断言：PASS，输出 `PASS QA standard list sort wiring`。
- `docs/frontend-development.md`：UPDATED，已将标准列表系统接入点 88 和显式隐藏筛选 14 的长期门禁证据合并到既有前端规则。
- `docs/backend-development.md` / `docs/experience-index.md`：UPDATED，已将 QA 手动绑定工艺路线并入既有产品侧路线绑定门禁。
- `git diff --check -- <AC-M09 实现文件和经验文档>`：PASS，无 whitespace error。

## Blocked

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BLOCKED，`testCompile` 被共享 `target/classes` 缺失阻断；检查时存在其它非本任务 Maven 进程写入同一 `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\target`。
- 限制 `maven.compiler.testIncludes=**/MesQaInspectionRegulationServiceTest.java` 后复跑：BLOCKED，20 分钟超时；期间主工作区仍出现其它非本任务 Maven 测试进程。
- `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js`：BLOCKED，当前全局契约被大量既有页面排序 helper 历史缺口阻塞；QA 页面聚焦扫描已确认新增四个标准列表均接入排序 helper。

## Result

blocked：QA 页面项目选择区收窄、Tab + 标准列表模板改造、正式工艺路线适用范围自动带出、黄框字段移除和手动绑定工艺路线已完成，并通过聚焦静态契约、标准列表数量契约、`pnpm ts:check`、frontend evidence validator 与 diff check；完整 AC-M09 后端目标 JUnit 仍需共享 Maven target 空闲后复跑，当前不提交、不推送、不标记 completed。
