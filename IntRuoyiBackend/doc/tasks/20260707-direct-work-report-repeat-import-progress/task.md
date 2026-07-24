# 报工导入支持同文件重复测试并累加进度

## Task Goal
按用户要求调整直接报工导入：同一份 `李萍.xlsx` 可多次导入；每次导入都基于可匹配的排产任务创建新的报工单，并让对应排产工单/工序进度按本次报工数量继续累加，而不是按源文件指纹跳过。

## Milestones
- [completed] 建立任务记录，确认现有重复导入限制与测试入口。
- [completed] 编写失败回归测试，证明当前同文件重复导入会被源文件行指纹跳过。
- [completed] 移除直接报工导入的重复指纹跳过限制，保留导入记录追溯。
- [completed] 修复重排后同一排产工序多任务时的匹配规则：优先按 Excel 任务单号匹配，未精确匹配时按任务数量/ID 稳定选择。
- [completed] 运行 targeted 验证并用本机真实接口验证进度累加。
- [completed] 更新任务证据，记录阻塞项和最终结果。

## Expected Verification
- 同一份直接报工 Excel 连续导入多次，后续导入仍会创建新的报工记录。
- 第二次及后续导入不再因为 `sourceFileSha256 + sheetName + rowNo` 已存在而计入重复跳过。
- 导入记录仍保留源文件、sheet、行号和新报工单关联，便于追溯多次测试。
- 对应排产工单/工序进度由现有报工进度同步链路继续累加。

## 经验门禁
- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写必须显式 UTF-8，命令不得使用 `&&`。
- 真实导入/登录/E2E：已读取 `docs/login-access.md`；本次只操作本机 `http://127.0.0.1:48081`，不连接测试服/正式服。
- 高风险数据写入：真实重复导入仅用于本机芋道源码租户当前测试数据，执行前后证据写入 `repeat-import-api-result.json`。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整重复导入业务规则和任务匹配规则，不做清库绕过。
- 是否存在临时补丁或绕过：否。

## Final Verification
- `mvn.cmd -pl yudao-module-mes -DskipTests compile`：PASS。
- `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package`：PASS，并重启本机后端到 `backend-20260707-182253.jar`，`/actuator/health` 为 `UP`。
- 真实接口：`POST /admin-api/mes/pro/feedback/import-direct-work-report-xlsx` 上传 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 返回 `code=0`、`importedCount=18`、`submittedCount=18`、`skippedRows=52`。
- 进度验证：本次导入前后，工序 `2973` 从 `396 / 39.6%` 增至 `528 / 52.8%`；工序 `2975` 从 `318 / 31.8%` 增至 `424 / 42.4%`；工序 `3085` 从 `540 / 54.0%` 增至 `720 / 72.0%`。

## Known External Blocker
- 目标单测受无关未跟踪文件 `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java` 编译错误阻塞：它访问了 `MesProBatchRecordExecutionServiceImpl.EXECUTION_STATUS_APPROVED` 私有常量。本任务未修改该文件；因此记录为外部测试编译阻塞。

## Current Status
completed
## Cleanup Keep
- doc/tasks/20260707-direct-work-report-repeat-import-progress/repeat-import-api-result.json
