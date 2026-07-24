# 任务：DCC 并行识别文件级互斥认领

- Task ID: `20260629-dcc-batch-recognition-file-claim`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

支持多个 Codex 或多个批量识别执行器并行处理 DCC 基础信息识别，但同一时刻同一个受控文件只能被一个执行器识别；识别完成后释放文件级认领，后续任务再按既有账本版本/方式跳过规则决定是否重识别。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成并补齐最终验证，可继续当前任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本次命中 PowerShell/中文编码门禁；任务文档、执行日志、请求命令记录与 SQL 契约文件均需显式 UTF-8 读写。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 下禁止用默认重定向写中文文件；命令输出和文件写入前需显式设置 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。文件级认领失败时不降级为 JVM 本地锁，也不静默重复识别；单文件入口暴露真实占用错误，批量入口明确统计为未执行/跳过占用。
- 是否从根因和长期维护角度解决：是。采用数据库正式文件认领表实现跨进程互斥，不依赖单实例内存锁或任务顺序假设。
- 是否存在临时补丁或绕过：否。不通过缩小并发度、串行轮询或仅在批量任务层做 best-effort 判断来规避重复识别。

## Milestones

- M1: 建立任务文档、执行日志、请求命令记录，明确并发互斥 BDD/TDD 与数据模型。状态：completed。
- M2: 补 RED 测试，覆盖文件级 claim、并发批次同文件互斥、完成后释放、单文件占用报错。状态：completed。
- M3: 实现文件识别认领表、Mapper/DO、SQL 契约与识别服务互斥逻辑。状态：completed。
- M4: 实现批量任务对文件占用的处理与结果统计/响应补强。状态：completed。
- M5: 运行定向验证、更新 evidence、收尾任务文档。状态：completed。

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -k recognition`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-batch-recognition-file-claim\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-batch-recognition-file-claim\database-schema-evidence.md`

## Current Blockers

- 暂无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` -> PASS（29 tests）
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_sql_scripts.py -k recognition -q` -> PASS（5 tests）
