# Task: 修复展厅讲解 live 数据重启后丢失

## Goal

修复数字展厅公司讲解 live 数据只存在后端进程内存、重启后丢失的问题，使 `showroom_narration_version` 真正承担持久化职责，重启后 `/showroom/display/narration` 仍能返回已发布讲解。

## Scope

- 先检查上一个后端任务状态并创建本任务记录。
- 复现“公司讲解重启后丢失”的真实失败链路。
- 先补 RED 回归测试，证明当前实现没有从 `showroom_narration_version` 读取 live 数据。
- 按最小范围补齐 narration 的持久化读写实现。
- 通过重启后的真实接口回归确认前台讲解不再丢失。

## Non-Scope

- 不重构公司正文、产品正文或 hall/product 前台接口。
- 不引入 fallback、mock、静默降级或“无讲解时默认成功”逻辑。
- 不顺带清理无关模块的 tmp、DCC、MES、AI 脏改动。

## Previous Task Check

- Previous backend task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-local-company-live-publish\task.md`
- Status before this task: completed.
- Impact: 前序任务已补齐公司现行正文与当前进程内公司讲解 live 数据；本任务继续收口其“重启后丢失”的残余风险。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Unrelated untracked tmp/artifact directories already exist.
- Impact: this task must stage only narration persistence code and its task records.

## Milestones

- [ ] M1: 创建任务记录并确认讲解 live 数据重启后丢失的失败链路。
- [ ] M2: 补 RED 回归测试，证明当前 narration service 未持久化。
- [ ] M3: 实现 narration 持久化读写与 live 读取。
- [ ] M4: 跑 GREEN 测试并完成重启后真实接口回归。
- [ ] M5: 更新任务记录、执行收尾预览并提交本任务改动。

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest,ShowroomNarration*Test "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs`
- restart backend and verify authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH`

## Current Status

Completed. narration 已切到数据库持久化实现，重启后公司讲解 live 数据仍可读取，前台展厅主页讲解面板不再因为后端重启而丢失。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentNarrationServiceTest,ShowroomHttpApiIntegrationTest,ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: restart backend after the new jar was built
- PASS: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs` after re-publishing company narration
- PASS: restart backend again and rerun the same runtime test; all three checks stayed green
- PASS: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` returned `code=0` after the second restart
- PASS: Playwright CLI browser smoke on `http://127.0.0.1:8081/showroom/home` after the second restart reported `errorCount = 0`, `warningCount = 0`, `narrationTextLength = 49`, `audioCount = 1`

## Residual Risk

- None for this regression path. Company narration live data is now stored in `showroom_narration_version`, and a restarted `48081` backend can read it back.
