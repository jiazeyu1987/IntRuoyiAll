# 20260729 排产员工作台手动重排全量数据包

## Task Goal

将排产员工作台截图中的“导出全部数据包 / 导入全部数据包”扩展为可承载手动重排所需正式数据的数据包，使目标环境导入后具备执行当前手动重排预览与应用重排的必要数据前提；保留“导出排产工艺路线 / 导入排产工艺路线”只处理路线排产配置的独立边界。

## Milestones

1. 现状审计：确认手动重排读取的数据源与现有按钮导入导出覆盖范围。
2. RED 契约：新增或更新后端测试，证明现有全部数据包缺少手动重排业务数据。
3. 后端实现：扩展全部数据包导出/导入字段和导入结果计数，保持缺引用 fail-fast。
4. 前端对齐：更新导入成功提示和类型，使按钮反馈包含重排数据导入结果。
5. 验证收尾：运行目标测试与必要静态检查，记录结果与剩余风险。

## Expected Verification

- 后端目标测试：`mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端静态目标检查：`node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js`
- 结构验证：`git diff --check`

## Current Status

in_progress

## Applicable Experience Gates

- PowerShell 命令不得使用 `&&`；Maven `-D` 参数必须整体加双引号。
- 手动重排必须基于正式排产工单、路线配置、日历产能、任务保护、用料和库存数据，不得用 mock、默认成功或配置包空字段伪装可重排。
- 当前工作区已有并行脏改动和 ahead 提交，本任务只修改本任务文件，提交前需要选择性暂存并避免混入无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少必要引用或数据包字段时应 fail-fast。
- `是否从根因和长期维护角度解决`：是。扩展正式数据包契约，而不是在手动重排接口中补默认数据。
- `是否存在临时补丁或绕过`：否。
