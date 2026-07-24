# eDHR 工序辅助表单联动填写

## 任务目标
- 当工艺路线工序配置 `MAIN / LOSS_REPORT / PROCESS_INSPECTION / PARAMETER_RECORD` 表单时，批次执行生成同一 `routeProcessId` 下的全部表单任务。
- 主表完成后，损耗单、过程检验单和参数记录表未完成时，下一工序保持阻塞。
- 配置缺少槽位、记录分类、校验策略、权限范围、必填策略或槽位快照时创建批次失败，不引入 fallback。

## 上一任务检查
- `doc/tasks/20260710-edhr-workorder-route-resolution/task.md` 状态为 `completed`，提交 `4d75a105f6` 已完成，不阻塞本任务。

## 经验门禁
- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`，中文文件显式按 UTF-8 处理，命令不使用 `&&`。
- BDD + 严格 TDD：先记录 Given/When/Then，再取得 RED，最后最小实现和回归验证。
- 后端契约：不新增接口、数据库表或迁移，不改变现有任务响应字段。
- 无 fallback：缺少正式配置直接失败，不补默认表单、默认权限或默认成功。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以路线工序批记录绑定作为唯一任务来源，按工序组统一校验和门禁。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- BDD: 同工序生成全部辅助表单任务 -> Given 工序配置主表、损耗单、过程检验单和参数记录表 / When 创建批次执行 / Then 同一工序生成 4 个独立任务并保留槽位类型。
- BDD: 辅助表单阻止下一工序 -> Given 主表已完成但任一辅助表单未完成 / When 用户打开下一工序 / Then 系统拒绝并提示上一工序批记录未全部填写完成。
- BDD: 同工序全部完成后流转 -> Given 同一工序全部必填表单已完成 / When 用户打开下一工序 / Then 下一工序可正常打开。
- BDD: 并行模式开放同工序表单 -> Given 工序执行模式为 PARALLEL / When 批次执行创建完成 / Then 同工序主表和辅助表单均可打开，但下一工序仍等待全部完成。
- BDD: 不完整槽位配置直接失败 -> Given 辅助表单缺少正式槽位元数据 / When 创建批次执行 / Then 创建失败并明确暴露配置缺失。

## 里程碑
1. [已完成] 建立任务文档、BDD 和后端证据骨架。
2. [已完成] 新增辅助表单生成、门禁和配置失败的 RED 测试。
3. [已完成] 最小实现正式配置校验并锁定既有工序门禁。
4. [已完成] 运行定向测试、服务回归和证据校验。
5. [已完成] 收尾清理、任务完成记录和独立提交准备。

## 预期验证
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260710-edhr-process-companion-forms/backend-api-evidence.md`

## 当前状态
- COMPLETED：同工序辅助表单任务生成、串并行门禁、完整配置校验和显式权限范围要求已实现；74 个服务测试通过。

## Current Status
completed
