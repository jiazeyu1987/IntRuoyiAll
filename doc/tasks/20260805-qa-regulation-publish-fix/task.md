# AC-M09 QA 检验规程发布闭环修复

## Task Goal

修复 `AC-M09 | QA | 维护检验规程` 当前只读/预览状态，补齐正式 QA 检验规程维护、草稿保存、发布、不可变版本与发布失败校验链路。

## Milestones

- [x] 建立后端 QA 规程保存草稿、发布、读取不可变版本的 API 与服务契约。
- [x] 增加后端发布完整性、冲突、已发布版本不可修改的 fail-fast 校验。
- [x] 接入前端 QA 规程页面正式保存草稿和发布调用，移除“未写入后台”的阻断提示。
- [x] 将 QA 页面改为 Tab 分区，并用 `UnifiedListTemplate` 承载规则、项目、检查和 PQC 预览列表。
- [x] 收窄 QA 项目选择区，只保留 DCC 项目代码下拉框，选中后再展示适用范围、检验规则、检验项目和发布检查。
- [x] 将路线版本、路线工序、SOP、正式批记录绑定等适用范围字段改为从产品绑定的正式工艺路线自动带出，禁止 QA 页面手工设置黄框字段。
- [x] 支持 QA 页面在产品未绑定或需修正时显式选择已发布工艺路线，并通过 QA 专用产品-工艺路线绑定 API 写入后重新带出适用范围。
- [x] 补齐 QA 手动绑定后端 JUnit 与前端静态契约 RED/GREEN 验证。
- [x] 修复顶部黄框不显示 DCC 项目选择内容的布局回归。
- [x] 隐藏截图红框内的副标题、绿色提示和空白间隔。
- [x] 隐藏适用范围黄色说明块，重排基础字段间距，并让手动工艺路线选择框回填上次正式绑定关系。
- [x] 隐藏顶部“发布检查”页签，不改现有发布校验、草稿保存和发布接口逻辑。
- [ ] 复跑完整 AC-M09 后端目标 JUnit。
- [x] 记录验证、收尾和剩余阻塞。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`
- `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js`
- 登录态 API 探针：`POST /admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` 使用无效 routeId 返回业务校验错误而非 `请求地址不存在`。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`
- `pnpm ts:check` 如前端类型链路改动需要全量类型验证。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs IntRuoyiFronted/tests/e2e/unified-list-template-empty-tabs-system-static.spec.js doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md docs/backend-development.md docs/experience-index.md`

## Current Status

blocked：最新截图反馈已完成，顶部 QA 页签只显示“总览 / 检验规则 / 检验项目”，不再显示“发布检查”；现有发布校验、草稿保存和发布接口代码未修改。目标静态合同、两个相邻 QA 合同、`pnpm ts:check` 和本机真实只读 Playwright 均通过；完整 AC-M09 后端目标 JUnit仍待共享 Maven target 空闲后复跑，标准列表系统合同当前为 91/88 并行计数漂移，因此总任务不标记 completed。

## Baseline Commits

- `5486d9ba9`：保存进入本任务前的既有前后端与任务文档改动。
- `fc5e98ffe`：保存进入本任务前的残余岗位矩阵分析文档更新。
- `515798d74`：保存并发 AC 任务文档更新。

## Applicable Gates

- 后端修改已读取 `docs/backend-development.md`，适用“QA 规程配置状态必须来自产品级规程记录”“PQC 检验项目事实必须来自发布规程和结构化 itemResults”和“QA 规程手动绑定必须允许已发布路线”。
- 前端修改已读取 `docs/frontend-development.md`，必须使用正式 API 错误展示，不得吞异常或默认成功。
- 数据库相关代码已读取 `docs/database-rules.md`，本次优先复用现有 QA 规程表，不新增运行 SQL。
- Git/PowerShell/收尾已读取 `docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式后端状态机与前端写入链路。
- `是否存在临时补丁或绕过`：否。
