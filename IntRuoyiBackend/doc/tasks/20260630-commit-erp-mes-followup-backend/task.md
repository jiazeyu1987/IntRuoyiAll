# 任务：提交 ERP/MES 可闭环后端代码补充批次

- Task ID: `20260630-commit-erp-mes-followup-backend`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在后端仓库中仅提交当前仍留在工作区、但已经具备 `completed` 状态与验证证据、且文件边界清晰的 ERP/MES 改动。本批目标只包含：

- `20260630-erp-production-order-material-list-bidirectional-link`
- `20260630-erp-production-material-list-grouped-popup`

`20260630-mes-material-shortage-use-production-material-list`、DCC 全量包、Showroom 奖项生图与其他 `in_progress` / `blocked` 任务继续保留在工作区。

## Previous Task Check

- 上一个后端提交任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-commit-committable-followup-backend\task.md`
- 状态：`completed`
- 处理说明：上一批后端补充提交已完成工作台全量包与 NAS 共享范围收口；本次进入新的 ERP/MES 提交批次。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md` 与 `docs\worktree-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档与执行日志显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 同文件混入未完成 hunk 时不得整文件强提；收口前必须核对 staged 文件与 diff 边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只提交已闭环、已验证、边界清晰的正式后端代码。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 已完成 ERP/MES 后端任务可独立提交 -> Given 后端工作区存在 ERP 生产用料清单分组查询与生产工单关联展示改动 / When 本次补充提交收口 / Then 只提交这些已具备 GREEN 证据的后端文件组。`
- `BDD: 未完成 schedule/DCC/Showroom 后端文件继续留在工作区 -> Given schedule 口径统一、DCC 全量包与 showroom 奖项生图文件仍混有 in_progress 或 blocked hunk / When 评估提交范围 / Then 这些文件整体留在工作区。`

## Milestones

1. M1：建立本轮后端 ERP/MES 提交任务并锁定候选文件组。`completed`
2. M2：补核候选任务 GREEN 证据与共享文件边界。`completed`
3. M3：按任务边界提交后端代码。`completed`
4. M4：记录剩余未提交范围并完成收尾预览。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSchemaTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesKingdeeProductionMaterialListMapperXmlTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --name-only`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check`

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSchemaTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesKingdeeProductionMaterialListMapperXmlTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesKingdeeProductionMaterialListController.java,**/MesProWorkOrderController.java,**/MesProWorkOrderRespVO.java,**/MesKingdeeProductionMaterialListMapper.java,**/MesKingdeeProductionMaterialListQueryService.java,**/MesKingdeeProductionMaterialListQueryServiceImpl.java -Dmaven.compiler.testIncludes=**/MesKingdeeProductionMaterialListSchemaTest.java,**/MesKingdeeProductionMaterialListMapperXmlTest.java,**/MesProWorkOrderControllerTest.java,**/MesKingdeeProductionMaterialListQueryServiceImplTest.java test` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\backend-api-evidence.md` -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro commit -m "任务: 提交ERP生产用料清单与工单关联后端收口"` -> PASS，创建 commit `7545a43ea6`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260630-commit-erp-mes-followup-backend --mode preview` -> PASS

## Current Blockers

- 无新的提交阻塞；剩余改动属于进行中/阻塞任务或边界不清文件，继续保留在工作区。
