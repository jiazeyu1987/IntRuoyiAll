# 执行日志：手动发布展厅带出展柜中英文描述

SETUP: IntRuoyi 上一任务 `20260601-e2e-build-release-yudao-admin` 已记录为 `blocked_on_running_release_operation`；本任务不改动旧任务发布脚本、测试或 runtime 产物。

BDD: 手动发布展厅输出展柜中英文描述 -> Given 8 个展柜已经配置中英文描述 / When 管理端点击手动发布展厅并生成 scoped release / Then release 的 `website-index.showrooms[*].description` 与 `descriptionEn` 必须包含对应展柜描述。

BDD: 缺少展柜描述失败快查 -> Given 发布源中任一展柜缺少中文或英文描述 / When 手动发布展厅 / Then 发布应暴露明确缺失字段错误，不得用空字符串、产品讲解或默认成功掩盖。

RED: 读取旧 TEST release `20260601T053334Z-be276b74dfa8-428f69663d1f` 的 `website-index` -> FAIL, 8 个展柜存在但 `description/descriptionEn` 为 0/8，Website 不能显示展柜描述。

GREEN: `mvn.cmd -pl yudao-module-showroom "-Dtest=ShowroomReleaseWebsiteIndexAssemblyTest,ShowroomReleaseAdminPublishIntegrationTest" test` -> PASS, Tests run: 11, Failures: 0, Errors: 0, Skipped: 0.

GREEN: `python -m pytest script/tests/test_showroom_excel_seed_tooling.py script/tests/test_showroom_sql_scripts.py` -> PASS, 12 passed.

GREEN: 调用与“手动发布展厅”按钮相同的 `POST /admin-api/showroom/release/publish`，payload `{siteKey:"yingtai-showroom",stage:"TEST"}` -> PASS, 生成 release `20260601T081746Z-be276b74dfa8-b111cad3b49c`。

GREEN: 读取新 TEST release `website-index` -> PASS, HALL_COUNT=8，READY_HALL_DESCRIPTIONS=8/8，`hall_01` 到 `hall_08` 的中英文描述长度均大于 0。

GREEN: 使用 Website `fetchShowroomWebsiteConfig` + `createMedicalKioskApp` 读取并渲染新 release -> PASS, CONFIG_READY=8/8，切到 `hall_01` 后 `[data-voice-copy]` 包含展柜中文描述。

CLEANUP: `task_closeout.py --task-id 20260601-showroom-hall-description-publish --mode preview` -> READY, keep `task.md`/`execution-log.md`，delete 临时验证脚本 `verify-manual-publish-hall-descriptions.mjs`。

CLEANUP: `task_closeout.py --task-id 20260601-showroom-hall-description-publish --mode apply` -> APPLIED, 已删除临时验证脚本，保留任务记录与正式测试/源码/SQL。
