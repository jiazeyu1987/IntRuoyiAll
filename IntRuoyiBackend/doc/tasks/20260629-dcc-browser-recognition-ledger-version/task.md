# 任务：DCC 受控浏览识别账本与按版本跳过

- Task ID: `20260629-dcc-browser-recognition-ledger-version`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

在现有 DCC 受控浏览“基础信息识别”能力上新增正式文件识别账本，记录每个文件按识别范围、识别方式、识别版本的最近识别结果；将批量识别跳过规则从“业务字段已有值”升级为“同文件在同 scope、同 method、同 version 下已成功识别且业务字段仍完整”才跳过，并在识别响应与批量任务响应中暴露版本信息。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-subtab-four-char-rename\task.md`
- 状态：`COMPLETED`
- 处理说明：无未完成阻塞项，可继续当前任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：任务开始前先命中相关经验并摘录；本次只命中 PowerShell/中文编码门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 下读取/写入中文文档与日志必须显式 UTF-8，不使用默认重定向写文件。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。识别版本配置缺失、账本写入失败、识别失败时直接暴露真实错误，不做默认版本或默认成功。
- 是否从根因和长期维护角度解决：是。通过独立账本表和显式版本配置统一识别状态来源，避免继续依赖业务字段残值判断。
- 是否存在临时补丁或绕过：否。不用文本文件账本、不用提交号或哈希自动推导版本、不保留兼容旧跳过逻辑。

## Milestones

- M1: 建立任务文档、执行日志、请求命令记录并锁定 BDD/TDD 范围。状态：completed。
- M2: 补数据库/服务 RED 测试，覆盖账本写入、失败记录、版本配置和批量跳过规则。状态：completed。
- M3: 实现账本表、Mapper/DO、版本配置与单文件识别联动。状态：completed。
- M4: 实现批量任务跳过规则、任务版本快照与响应字段扩展。状态：completed。
- M5: 运行定向验证、回填 evidence、更新任务状态并准备收尾。状态：completed。

## Expected Verification

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest test`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -k recognition`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-browser-recognition-ledger-version\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-browser-recognition-ledger-version\database-schema-evidence.md`

## Current Blockers

- 暂无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> PASS（27 tests）
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -q` -> PASS（4 tests）
