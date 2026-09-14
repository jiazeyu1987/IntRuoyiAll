# Execution Log

## User Intent

- 2026-08-10：用户要求“PQC管理下的列表改成倒序排列，即最近提交的排在最前面”。

## BDD

- BDD: PQC管理列表最近提交优先 -> Given PQC组长打开“PQC管理”列表且存在多条不同提交时间的 PQC 提交记录 / When 列表通过正式分页接口加载 / Then 第一页按服务端提交时间倒序返回，提交时间相同按事件 ID 倒序稳定排列，最近提交记录排在最前面。
- BDD: 排序不在前端当前页伪造 -> Given PQC管理列表通过服务端分页加载 / When 用户切换页码或筛选条件 / Then 前端直接使用正式分页返回顺序，后端按提交时间倒序和事件 ID 倒序提供稳定跨页排序。

## Milestone Evidence

- in_progress: 已读取 frontend-feature-delivery 与 backend-api-delivery 技能、前后端触发规则、PowerShell/编码和 E2E 规则；已定位前端 `TeamLeaderWorkbenchPage.vue` 与后端 `MesProProcessPoolTimelineReadMapper.xml` 当前按 `server_submit_time ASC, id ASC` 排序。

## RED / GREEN / REGRESSION

- RED: `node tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs` -> FAIL，PQC管理倒序合同不存在，首个失败为缺少正式管理列表排序证据。
- RED: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> FAIL，mapper 未满足 `server_submit_time DESC, id DESC`。
- RED: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增后端排序测试先失败；初始方案因新增排序字段缺失在 testCompile 暴露，后收敛为后端固定倒序。
- GREEN: `node tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- CHECK: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260810-pqc-management-desc-sort\frontend-feature-evidence.md` -> PASS。
- CHECK: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260810-pqc-management-desc-sort\backend-api-evidence.md` -> PASS。
- REGRESSION: `git diff --check` -> PASS；仅有已有 LF/CRLF 工作区提示，无 whitespace error。

## Blockers

- 当前工作区已有大量非本任务脏改动；本任务仅修改 PQC 排序相关文件和新建任务文档，不会清理、提交或回滚无关改动。
