# Task: DCC 上传历史文件名称与版本联动

## Goal

为 DCC 受控文件上传页补充“按文件类型带出历史文件名称”的后端能力：上传人先选择文件类型后，可选或不选历史同名文件名称；如果前端选择了某个历史文件名称，页面能够读取该名称在当前类别下的当前版本号并用于联动展示。

## Scope

- 先明确阻塞并暂停当前最新未完成后端任务，避免跨任务混改。
- 在本任务包中记录 BDD、RED/GREEN 证据和最终验证结果。
- 新增一个受控文件上传页专用的后端查询接口，返回指定文件类别下可选历史文件名称及当前版本号。
- 复用现有受控文件主表/当前有效版本关系，不修改数据库 schema。
- 补充后端接口测试或服务测试，覆盖有历史版本和无历史版本两类行为。
- 不修改审批路由、版本递增规则或现有提交流程的 fail-fast 约束。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-distribution-medium-model/task.md`
- Status before this task: blocked by explicit user reprioritization.
- Impact: the previous task is intentionally paused and does not block this
  upload-linkage delivery.

## Milestones

- [x] M1: Block the previous unfinished backend task and create this task
  package before production-code edits.
- [x] M2: Record BDD scenarios and RED evidence for the missing upload-name
  option API.
- [x] M3: Implement the minimal backend query endpoint and mapping.
- [x] M4: Run targeted backend verification and update evidence.
- [x] M5: Commit only backend files produced by this task if verification fully
  passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260516-dcc-upload-name-version-linkage\backend-api-evidence.md`

## Current Status

Completed and committed. The backend upload-name option endpoint, targeted
tests, runtime packaging, and live backend consumer proof are complete, and the
verified DCC backend changes have been committed in the service repository.

## Blocker And Impact

- No remaining blocker for this feature slice.

## Final Verification Result

- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionApiTest,DccControlledFileUploadNameOptionQueryServiceTest" test` -> PASS
- `mvn -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
- Live backend consumer proof through the upload-page path -> PASS
