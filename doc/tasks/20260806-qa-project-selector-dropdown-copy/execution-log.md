# Execution Log

## 2026-08-06

- User intent: “项目代码输入框还要支持选择下拉”。
- Scope: QA 规程配置页头部 DCC 项目代码控件；保留上次选择恢复、复制按钮、发布区和三个 QA 内容页签。
- Skill: `frontend-feature-delivery`。
- BDD: 项目代码仍可下拉选择 -> Given QA 页头部项目代码字段显示上次选择内容 When 用户点击或聚焦字段 Then 字段仍打开正式 DCC 项目下拉候选并支持选择。
- BDD: 项目代码仍可远程搜索 -> Given 候选很多 When 用户输入项目代码或项目名称关键字 Then 控件继续调用正式远程候选加载方法。
- BDD: 复制不替代选择 -> Given 用户需要复制当前项目文本 When 复制按钮存在 Then 复制按钮不把项目代码控件改成纯输入框，用户仍可用下拉改变项目。
- RED: `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs` -> FAIL，首个失败为缺少 `data-qa-regulation-project-dropdown`，符合预期。
- Implemented: `QaRegulationPage.vue` 项目代码 `el-select` 增加 `automatic-dropdown`、`default-first-option`、`remote-show-suffix` 和 `data-qa-regulation-project-dropdown`，保留 `filterable`、`remote`、`:remote-method="loadDccProjectCodeOptions"` 与正式候选 `el-option`。
- GREEN: `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-last-copy-static.spec.cjs doc/tasks/20260806-qa-project-selector-dropdown-copy` -> PASS。
- BLOCKED: `pnpm ts:check` -> FAIL，失败在非本任务文件 `TeamLeaderWorkbenchPage.vue`，缺少 `searchActiveOrderCandidates`、`activeOrderCandidateLoading`、`activeOrderCandidateOptions`、`activeOrderCandidateError` 等模板绑定。
- BLOCKED: `git log --oneline origin/int_main..HEAD` 显示当前分支已有 6 个未推送非本任务提交；为避免推送无关提交，本任务暂不提交/推送。
- RECHECK: `node tests\e2e\qa-regulation-project-last-copy-static.spec.cjs`、`node tests\e2e\qa-regulation-version-publish-header-static.spec.cjs`、`node tests\e2e\qa-regulation-publish-tab-hidden-static.spec.cjs`、`node tests\e2e\qa-regulation-display-fields-titlebar-static.spec.js`、`node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，确认项目代码控件仍支持下拉选择且相邻 QA 页头行为未回归。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-project-selector-dropdown-copy/frontend-feature-evidence.md` -> PASS。
- RECHECK BLOCKED: `pnpm ts:check` -> FAIL，当前失败位于非本任务文件 `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue(3787,7)`，`TeamLeaderActiveOrderAddReqVO` 不接受 `routeId` 属性。
- EXPERIENCE: 按 `project-experience-consolidation` 合并长期经验到 `docs/frontend-development.md#复合输入控件交互保留门禁`，并在 `docs/experience-index.md` 增加关键词路由。
