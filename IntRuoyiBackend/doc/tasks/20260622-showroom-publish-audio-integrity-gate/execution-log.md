# Execution Log

INFO: user-request -> 用户要求长期方案：手工发布时不临时生成音频，而是在发布阶段先校验音频完整性，缺少音频即停止发布。

INFO: prior-task-check -> 已复核 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-test-server-showroom-source-missing-analysis\task.md`，确认测试服当前根因是 live narration 文件引用与对象存储不同步。

INFO: experience-gates -> 已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 与命中的 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`；本任务必须 fail fast，不引入 fallback、不在发布瞬间补生成音频。

BDD: 公司讲解音频对象缺失时阻断发布 -> Given 当前待发布版本沿用了 live 公司讲解 `audio_file_id` / When 发布入口开始解析 release source snapshot / Then 若当前环境无法读取该音频对象，发布直接失败并返回公司级发布阻断错误。

BDD: 手工发布不临时生成音频 -> Given 当前 live narration 文案存在但音频对象缺失 / When 用户点击手动发布展厅 / Then 系统只做完整性校验并阻断，不进入临时生成音频路径。

RED: logical-contract-check -> FAIL, 变更前公司级资源缺失虽然会最终失败，但没有显式收口为公司级 `SHOWROOM_RELEASE_COMPANY_BLOCKED` 发布门禁，错误语义停留在底层 source read failure。

GREEN: targeted-test -> PASS, `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest#shouldFailFastWhenCompanyNarrationAudioObjectIsMissing test`

GREEN: regression-suite -> PASS, `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest test`

RESULT: implementation -> PASS, `ShowroomReleaseAssembler.resolveSourceSnapshot()` 已把公司封面与公司双语音频解析统一包裹为公司级发布阻断；当 `fileService.getFileContent(...)` 无法读取当前 live narration 音频对象时，错误向上收口为 `SHOWROOM_RELEASE_COMPANY_BLOCKED`，同时保留底层 `SHOWROOM_RELEASE_SOURCE_MISSING` 作为原因链。
