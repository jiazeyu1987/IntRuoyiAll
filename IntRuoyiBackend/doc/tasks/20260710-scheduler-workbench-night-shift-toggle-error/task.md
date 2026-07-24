# 排产员工作台夜班切换系统异常

## 任务目标
- 修复排产员工作台「工序列表」点击夜班开关提示系统异常的问题。
- 保持夜班设置只更新用户操作的目标工序，不引入降级或静默失败。

## 经验门禁
- 已按 UTF-8 读取 PowerShell 经验，命令显式设置输入/输出编码。
- 本任务是用户可见缺陷修复，按 BDD + 严格 TDD 执行。
- 不操作服务器、不修改真实业务数据；优先通过日志、源码和单元测试定位根因。
- 不引入 fallback、降级、吞异常或 mock 成功。

## 里程碑
1. 定位点击夜班触发的接口、请求体与后端异常根因。
2. 补充失败回归测试复现该异常。
3. 最小化修复并通过定向测试。
4. 完成证据校验、收尾预览与提交。

## 期望验证
- RED：旧逻辑下夜班切换回归测试失败。
- GREEN：修复后夜班切换回归测试通过。
- REGRESSION：排产员工作台 WIP 设置相关测试通过。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，优先使用排产工序快照配置 ID 精准定位排产配置。
- 是否存在临时补丁或绕过：否。

## 当前状态
- 已完成：已定位并修复重复路线排产配置导致的夜班切换系统异常，待重启运行态验证与提交。


## 验证结果
- 日志复现：`process-wip-settings` 抛出 `TooManyResultsException`。
- 只读数据回查：`route_version_id=4 AND route_process_id=922499` 存在 8 条排产配置。
- GREEN：手动定向编译后目标 surefire 测试通过。
- 阻塞：标准 Maven 测试被非本任务脏改 `MesProEdhrBatchExecutionServiceImpl.java` 编译错误阻断。


## 最终验证结果
- 根因：夜班切换保存时按 routeVersionId + routeProcessId 查询工艺排程配置，真实数据存在多条配置记录，触发 selectOne TooManyResultsException。
- 修复：优先使用排产工序快照上的 routeScheduleConfigId 精确定位配置，再保存夜班设置。
- 验证：定向回归测试通过；运行态 jar 已热补丁重启，健康检查 UP；运行态 class hash 与本地修复 class 一致。
- 已知阻塞：标准 Maven 全量编译被无关文件 MesProEdhrBatchExecutionServiceImpl.java 中 routeFlowConfigMapper 缺失阻塞，本任务未修改该文件。

Status: completed
