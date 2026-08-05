# Verification Report

## Scope

- AC-M09：QA 维护检验规程正式草稿保存、发布、不可变版本、缺首检/巡检/末检发布失败、前端正式保存/发布接入。
- 追加前端范围：QA 页面从一次性直铺展示改为单一 `DCC 项目代码` 下拉选择，选中项目后再展示 Tab、适用范围、检验规则、检验项目和发布检查，并用 `UnifiedListTemplate` 承载规则、项目、发布检查和 PQC 任务预览列表。

## Passed

- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS，后端生产代码编译通过。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，前端静态契约确认正式 API 已接入、旧未写入后台提示已移除，QA 页面使用 Tab + `UnifiedListTemplate`，且项目选择区只保留 1 个必填 `DCC 项目代码` 下拉框，不显示旧已配置/待配置列表。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：PASS，系统标准列表模板接入点 88 个、显式隐藏筛选列表 14 个。
- `pnpm ts:check`：PASS，前端 Vue/TypeScript 类型检查通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，frontend feature evidence 有效。
- QA 页面专属排序接线断言：PASS，输出 `PASS QA standard list sort wiring`。
- `docs/frontend-development.md`：UPDATED，已将标准列表系统接入点 88 和显式隐藏筛选 14 的长期门禁证据合并到既有前端规则。
- `git diff --check -- <AC-M09 实现文件>`：PASS，无 whitespace error。

## Blocked

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BLOCKED，`testCompile` 被共享 `target/classes` 缺失阻断；检查时存在其它非本任务 Maven 进程写入同一 `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\target`。
- 限制 `maven.compiler.testIncludes=**/MesQaInspectionRegulationServiceTest.java` 后复跑：BLOCKED，20 分钟超时；期间主工作区仍出现其它非本任务 Maven 测试进程。
- `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js`：BLOCKED，当前全局契约被大量既有页面排序 helper 历史缺口阻塞；QA 页面聚焦扫描已确认新增四个标准列表均接入排序 helper。

## Result

blocked：QA 页面项目选择区收窄、Tab + 标准列表模板改造已完成并通过聚焦静态契约、标准列表数量契约、`pnpm ts:check`、frontend evidence validator 与 diff check；完整 AC-M09 后端目标 JUnit 仍需共享 Maven target 空闲后复跑，当前不提交、不推送、不标记 completed。
