# 20260729-route-workbook-full-export-import

## Task Goal

让工艺路线列表当前“导入 / 导出”Excel 数据包覆盖现有全部工艺路线及其正式路线相关数据：基础信息、当前投影工序、流转关系图、布局、关联产品、工序 BOM、排产配置、工艺流程用途配置、批记录表单绑定与表单槽位绑定。

## Milestones

1. 建立任务记录、BDD 场景和现有导入导出边界。
2. 先补失败测试，锁定“导出全部路线 + 全量数据 Sheet + 导入回放”契约。
3. 扩展后端工作簿导出与导入服务，保持失败显式暴露。
4. 调整前端导出按钮语义为全量导出，保留现有导入入口。
5. 运行定向验证并记录 RED/GREEN/回归结果。

## Expected Verification

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteWorkbookExportServiceTest,MesProRouteWorkbookImportServiceTest,MesProRouteControllerWorkbookExcelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，扩展现有可导入工作簿契约，而不是新增旁路导出。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Git 基线：当前任务开始前工作区已有非本任务脏改，已按项目规则提交独立基线 `5bdaee38`。
- 工艺路线三类配置：批记录表单、表单槽位 `formBindings`、工序开始配置不得混用；导出导入必须分别保留正式字段来源。
- No fallback：缺少正式引用、Sheet、必填字段或主数据时失败，不写默认成功值。

## Verification Evidence

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteWorkbookExportServiceTest,MesProRouteWorkbookImportServiceTest,MesProRouteControllerWorkbookExcelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，11 tests。
- `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-route-workbook-full-export-import --mode preview` -> PASS，无删除项、无阻塞。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-route-workbook-full-export-import --mode apply` -> PASS，无删除项。

## Cleanup Keep

- doc/tasks/20260729-route-workbook-full-export-import/backend-api-evidence.md
- doc/tasks/20260729-route-workbook-full-export-import/frontend-feature-evidence.md
