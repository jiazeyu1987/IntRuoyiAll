# Execution Log: 修复展厅讲解 live 数据重启后丢失

BDD: 已发布公司讲解在后端重启后仍可读取 -> Given 公司讲解已经发布为 live 版本 / When 后端进程重启并重新从持久层初始化 / Then `/showroom/display/narration` 仍应返回对应 target/audience/language 的 live 讲解文本与音频地址。

BDD: 缺失持久化时必须暴露真实失败 -> Given 当前实现只把讲解 live 状态保存在进程内存 / When 后端重启后访问公司讲解公开接口 / Then 系统必须明确返回 `SHOWROOM_TARGET_NOT_FOUND: live narration not found`，而不是伪造讲解内容。

- M1: In progress. 已创建任务记录，正在固化“重启后 live narration 丢失”的 RED 证据。
- M1: Completed. 重启 `48081` 后，`GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` 重新返回 `SHOWROOM_TARGET_NOT_FOUND: live narration not found`，而 `/showroom/display/home` 与 `/showroom/display/company` 继续正常。
- M2: Completed.
- RED: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs` -> FAIL，只有 `company narration endpoint` 失败，重现“正文持久化但讲解重启丢失”的回归。
- RED: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` after backend restart -> FAIL, `SHOWROOM_TARGET_NOT_FOUND: live narration not found`
- RED: `showroom_narration_version` 持久化层缺失 -> FAIL，模块内只有内存版 `ShowroomNarrationService`，没有对应 DO/Mapper/PersistentService。
- M3: Completed. 新增 `ShowroomNarrationOperations`、`ShowroomNarrationVersionDO`、`ShowroomNarrationVersionMapper`、`ShowroomPersistentNarrationService`，并让 Spring 路径下的 `ShowroomApiRuntime` 使用持久化 narration service。
- M3: Completed. 为 H2 showroom 单测库补齐 `showroom_narration_version` 建表和清理脚本。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentNarrationServiceTest,ShowroomHttpApiIntegrationTest,ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS
- M4: Completed. 新 jar 重启后，先确认旧 runtime 中的内存 live narration 已丢失，再通过正式 API 重新发布公司讲解，使 `showroom_narration_version.id = 1` 成为持久化的 `PUBLISHED` 版本。
- GREEN: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs` -> PASS after re-publish
- GREEN: second backend restart -> PASS；再次执行同一 runtime test 仍然 PASS
- GREEN: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` -> PASS after second restart
- GREEN: Playwright CLI browser smoke on `http://127.0.0.1:8081/showroom/home` -> PASS，`errorCount = 0`、`warningCount = 0`、`audioCount = 1`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-live-persistence\bug-regression-evidence.md` -> PASS
- CLOSEOUT-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-showroom-narration-live-persistence --mode preview` -> READY，默认保留 `task.md` 与 `execution-log.md`，默认清理 `bug-regression-evidence.md`
- M5: Completed. 任务文档已更新为完成状态，当前回归路径无剩余阻塞。
