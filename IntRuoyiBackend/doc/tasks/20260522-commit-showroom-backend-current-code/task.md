# 任务：提交 Showroom 后端当前代码

## 目标

在 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中提交当前已通过验证的 showroom 后端源码、测试与 SQL 变更，不混入 MES 在途改动、图片产物或无关 task 文档残留。

## 范围

- `sql\showroom\**`
- `sql\mysql\20260522_showroom_product_cover_batch_task.sql`
- `yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\config\YudaoAiProperties.java`
- `yudao-module-showroom\src\main\java\**`
- `yudao-module-showroom\src\test\java\**`
- `yudao-module-showroom\src\test\resources\**`
- `doc\tasks\20260522-commit-showroom-backend-current-code\**`

## 非范围

- 不提交 `yudao-module-mes\**` 当前在途电子批记录视觉保真改动。
- 不提交 `yudao-module-showroom\output\imagegen\**`、生成图片、无关历史 task 目录与临时文件。
- 不回退用户或其他并行任务的未提交修改。

## 前置检查

- 根目录 `D:\ProjectPackage\Int\IntRuoyi` 不是 Git 仓库，后端真实仓库为 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`。
- 同仓上一任务 `doc\tasks\20260522-electronic-batch-record-report-visual-fidelity-optimization\task.md` 原状态为 `In Progress`；因用户当前明确切换为“继续提交前后端代码”，该任务需先标记为 scope switch blocker。
- showroom 相关当前代码已具备独立验证证据，可按 showroom 线单独提交。

## 里程碑

- [x] M1：确认上一同仓任务已显式阻塞，并锁定本次 showroom 后端提交范围。
- [x] M2：复核 showroom 后端相关验证命令通过。
- [ ] M3：精确暂存 showroom 后端当前代码与本任务记录。
- [ ] M4：完成后端 Git 提交并复核剩余工作区状态。
- [x] M5：执行 closeout preview 并记录结果。

## 预期验证

- `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomPersistentContentServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomProductCoverImageServiceTest,ShowroomFoundationContractTest,ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest,ShowroomProductExcelImportExportIntegrationTest,ShowroomProductNarrationRegressionTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-commit-showroom-backend-current-code --mode preview`

## 当前状态

Verified, pending commit.

## 当前结果

- 已将同仓 MES 视觉优化任务显式标记为 `scope switch blocker`，不纳入本次提交范围。
- showroom 后端相关验证已通过：
  - `mvn -pl yudao-module-showroom,yudao-module-ai -am "-Dtest=ShowroomPersistentContentServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomProductCoverImageServiceTest,ShowroomFoundationContractTest,ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest,ShowroomProductExcelImportExportIntegrationTest,ShowroomProductNarrationRegressionTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- closeout preview 已通过：
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-commit-showroom-backend-current-code --mode preview`
