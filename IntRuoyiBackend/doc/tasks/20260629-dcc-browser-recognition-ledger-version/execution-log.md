# 执行日志：DCC 受控浏览识别账本与按版本跳过

BDD: 首次识别写入业务字段与账本 -> Given 受控文件首次执行基础信息识别且识别成功 / When 后端完成项目代码匹配与业务字段回写 / Then 同事务写入 `dcc_controlled_file_recognition_record` 成功账本，记录 scope、method、version、结果、识别人和识别时间。

BDD: 同版本同方式成功记录可跳过 -> Given 某文件已经存在 `BASIC_INFO` 范围下同识别方式、同识别版本的成功账本且业务字段仍完整 / When 执行批量识别且未勾选覆盖 / Then 当前文件应被跳过，批量任务累计 `skippedExistingCount`。

BDD: 版本升级后必须重新识别 -> Given 某文件已有成功账本但识别版本与当前配置版本不同 / When 再次执行批量识别 / Then 不得跳过，必须重新执行识别并刷新账本记录。

BDD: 业务字段存在但缺少同版本成功账本不得跳过 -> Given 文件已有 `productName/productCode/dccProjectCodeId` 但没有匹配当前版本与方式的成功账本 / When 执行批量识别 / Then 当前文件不得因旧字段残值被跳过。

BDD: 文件名高置信命中记录快捷方式 -> Given 文件名快捷命中项目代码并直接完成识别 / When 保存识别结果 / Then 账本 `recognitionMethod` 为 `FILE_NAME_SHORTCUT`。

BDD: 内容识别记录 Codex 方式 -> Given 文件名未命中而通过 Codex CLI 内容识别成功 / When 保存识别结果 / Then 账本 `recognitionMethod` 为 `CODEX_CLI_CONTENT`。

BDD: 识别失败写失败账本并暴露真实错误 -> Given 单文件识别失败 / When 后端结束识别流程 / Then 写入失败账本，保留失败消息，并向上抛出真实异常。

BDD: 缺少识别版本配置必须 fail-fast -> Given 运行环境未配置 `yudao.dcc.project-code-recognition.version` / When 单文件或批量识别开始执行 / Then 后端直接失败，不做默认版本回退。

- 2026-06-29：GREEN: task-bootstrap -> PASS，已创建 `task.md` 与 `execution-log.md`，并完成经验门禁摘录。
- 2026-06-29：RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> FAIL，预期原因：账本 DO/Mapper、版本快照字段和新跳过规则实现尚不存在。
- 2026-06-29：RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -q` -> FAIL，预期原因：缺少账本 migration 与 `recognition_version_snapshot` schema 字段。
- 2026-06-29：GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -q` -> PASS，4 项 SQL 契约通过。
- 2026-06-29：GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> PASS，27 项服务测试通过。
