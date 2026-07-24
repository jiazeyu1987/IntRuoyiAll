# 执行日志：展厅产品批量封面支持模式选择与多 Codex CLI 并发

- BDD: 批量封面接口支持全部重生成与仅未上传两种模式 -> Given 企宣用户以当前筛选条件触发批量封面 / When 请求显式指定 `ALL` 或 `MISSING_ONLY` / Then 后端必须只按所选模式处理已发布产品，并对无封面与已有封面的行为给出可汇总结果。
- BDD: 仅未上传模式必须跳过已有封面的已发布产品 -> Given 当前筛选结果里同时存在已发布有封面产品和已发布无封面产品 / When 企宣用户以 `MISSING_ONLY` 模式执行批量封面 / Then 系统只处理无封面产品，并把已有封面产品记入单独的跳过统计，而不是重生成它们。
- BDD: 批量封面应以有界并发同时运行多个本地 Codex CLI 进程 -> Given 当前筛选命中多条需要生成封面的已发布产品 / When 后端执行批量封面 / Then 系统必须使用有限并发同时编排多个本地 Codex CLI 任务，并继续稳定汇总成功数、失败数和失败明细。
- RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `CodexCli.parallelism`、`resolveBatchParallelism`、批量封面模式参数和 `skippedExistingCount` 契约。
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`ShowroomProductCoverImageServiceTest` 与 `ShowroomApiRuntimeBatchCoverModeTest` 共 `6` 个测试全绿。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\backend-api-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview` -> PASS，预览仅建议清理 `backend-api-evidence.md`。
- FACT: `ShowroomAdminController.java`、`ShowroomApiRuntime.java` 与同仓的公司字段翻译任务共享同一批 controller/runtime 文件，当前无法在不混入并行改动的前提下完成纯后端任务提交。
