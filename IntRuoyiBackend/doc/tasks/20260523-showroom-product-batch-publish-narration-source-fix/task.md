# 任务：修复产品发布时报 latest ZH narration not found

## Goal

修复 showroom 产品发布链路中大量出现的
`SHOWROOM_TARGET_NOT_FOUND: latest ZH narration not found for current published product revision`
报错，确保：

- 当产品最新 revision 只是字段/详情草稿、讲解稿仍挂在当前已发布 revision 上时，正式发布链路能复用当前可用的双语讲解稿 source revision；
- 批量发布与相关后端发布路径不再把“最新字段 revision”误当成“讲解稿 source revision”；
- 缺稿、空稿或中英文讲解稿 source 不一致时仍显式失败，不引入 fallback、静默跳过或假成功。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-product-batch-publish-narration-source-fix\**`

## Non-Scope

- 不混入 `infra runtime control` 或公司页面 revision mismatch follow-up 的在途改动
- 不放宽 narration live/source revision 一致性校验
- 不改 Website 展示端
- 不用 mock 成功、默认通过或静默回退掩盖真实缺稿数据

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-page-revision-mismatch-followup\task.md`
- Status before this task: `Blocked on 2026-05-23`
- Impact on this task:
  - 已按仓库要求先将上一同仓 showroom follow-up 显式标记为被当前更高优先级产品发布回归打断；
  - 本次任务只处理产品发布缺陷，不继续混改公司页面 follow-up。

## Milestones

- [x] M1：创建任务文档并完成上一同仓任务状态处置。
- [x] M2：补后端 RED，锁定“最新字段 revision 无讲解稿时，发布需复用当前可用讲解稿 source”。
- [x] M3：最小修复批量/后端发布链路的 narration source 解析。
- [x] M4：运行定向 GREEN 验证并记录证据。
- [x] M5：更新执行日志、收尾预览，并评估本任务提交边界。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 或至少本次定向：
  `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#batchPublishProductsShouldReuseCurrentPublishedNarrationWhenLatestRevisionOnlyChangesFields" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Current Status

Completed on 2026-05-23.

## Notes

- 当前仓库存在与本任务无关的 `infra` 在途改动，以及 `ShowroomHttpApiIntegrationTest.java` 上一轮公司页面 follow-up 的未提交测试补丁；本次修复只能在理解现状后做增量修改，不得回滚无关内容。

## Closeout

- PASS: `validate_bug_regression.py --evidence ...\\bug-regression-evidence.md`
- PASS: `task_closeout.py --mode preview`
- PASS: `task_closeout.py --mode apply`
- 已按 preview 名单删除临时 `bug-regression-evidence.md`，仅保留 `task.md / execution-log.md`
