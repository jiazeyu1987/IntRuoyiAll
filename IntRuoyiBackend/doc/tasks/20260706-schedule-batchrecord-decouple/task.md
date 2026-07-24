# 20260706-schedule-batchrecord-decouple

## Task Goal
- 将生产排产前检查与工艺批记录系统解耦：排产、重排只受排产域自己的路线、工序、日历、产能等规则限制，不再因为批记录路线启用、工单批次号缺失或批记录模板绑定问题阻断。
- 不改接口、不改数据库结构、不改批记录真实业务数据；仅移除排产前检查中的跨系统阻断。

## Milestones
- [x] 建立任务记录、经验门禁、BDD/TDD 约束。
- [x] RED：新增/调整回归测试，证明批记录阻断仍会导致排产前检查失败。
- [x] GREEN：移除排产前检查中的批记录阻断逻辑。
- [x] 验证：运行排产前检查相关后端测试。
- [x] 收尾：清理任务产物并单独提交本任务改动。

## Expected Verification
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test`

## 经验门禁
- PowerShell/Windows 命令：已先阅读 `docs/powershell-memory.md`，命令避免 `&&`，中文读写使用 UTF-8。
- 任务文档：本任务在生产代码修改前创建 `doc/tasks/20260706-schedule-batchrecord-decouple/` 并记录目标、里程碑、验证与状态。
- BDD + 严格 TDD：先记录 Given/When/Then，再修改测试形成 RED，最后最小实现与 GREEN。
- 无 fallback：不新增兜底、降级、吞异常或 mock 成功。
- Git 提交：仅提交本任务直接相关文件，避开当前工作区已有无关脏改。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；移除排产前检查对批记录域配置的跨系统阻断，保留批记录系统自身流程职责。
- 是否存在临时补丁或绕过：否。

## Current Status
completed
- 状态：已完成。
- 已完成：定位并移除排产前检查中的批记录路线、批次号、批记录模板绑定阻断；目标回归测试已通过；清理 apply 无待删除项。
- 剩余：无。
