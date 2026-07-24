# Task: 六路识别页签增加一键清空电子批记录报表按钮

## Goal

为 `报表管理 -> 报表设计器 -> 六路识别` 提供一个“清空电子批记录报表”操作按钮。点击后，后端一次性删除 `报表设计器` 中 `电子批记录` 文件夹下的全部报表，并同步清理 `MES` 电子批记录报表元数据，返回本次删除数量。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecordreport\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\**`

## Non-Scope

- 不调整其他报表目录。
- 不重构六路识别现有导入逻辑。
- 不改动数据库 schema。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-electronic-batch-record-folder-report-delete\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓库电子批记录报表数据清理任务已完成，不阻塞本次新增批量删除接口。

## Milestones

1. 建立任务文档并确认现有单条删除链路、目录查询方式和测试落点。
2. 先写失败测试，新增批量删除接口契约与 service 行为。
3. 最小实现电子批记录目录批量删除，并返回删除数量。
4. 跑定向测试、记录证据、执行 closeout preview 与任务范围提交。

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-six-route-tab-clear-electronic-batch-record-reports --mode preview`

## Current Status

Completed on 2026-05-22.

## Completed Work

1. 新增 `DELETE /admin-api/mes/pro/batch-record-report/delete-all` 批量删除接口，返回删除报表数与元数据数。
2. 新增目录缺失错误码 `PRO_BATCH_RECORD_REPORT_CATEGORY_NOT_EXISTS`，目录缺失时 fail-fast。
3. 新增 `findElectronicBatchRecordCategoryId()` 与 `deleteReportsByCategoryId()` gateway 能力。
4. 将电子批记录报表元数据删除改为物理删除，避免逻辑删除残留唯一键阻塞后续重新生成。
5. 为 controller/service 增补定向测试与原始表级断言。

## Final Verification

- PASS: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\backend-api-evidence.md`
- PASS: runtime rebuild + restart loaded the new backend code
- PASS: final DB state shows `jimu_report = 0`, active `mes_pro_batch_record_report = 0`, soft-deleted metadata = `0`
