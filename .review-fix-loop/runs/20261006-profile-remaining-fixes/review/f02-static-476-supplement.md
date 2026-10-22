# F02 476 eDHR 共享边界补充静态审核

2026-10-08。已通过只读 `git rev-parse HEAD` 确认当前源码基点为 `476ec5322ab2b8d76ad67308a50eb6c08f980533`。本报告保留 round3 全链路审核与 round4 认证/会话边界审核，仅补充 033→476 的实际 eDHR 共享上下游。读取了精确 upstream476-changes.txt 和相关生产代码 diff，随后追踪正式调用；没有用零文件交集代替业务审查。当前验收依 acceptance-amendment.md 顶端修订：全部六 AC 保留，验证为静态 + compile/types/lint。

logic_status: pass（本次共享边界静态范围）

usability_status: pass（本次共享边界静态范围）

ui_status: pass（static only）；rendered_UI/runtime: NOT RUN

## blocking_issues

未发现 476 共享 eDHR 改动引入的 F02/F01 合同阻断问题。最新 476 的 compile/types/lint 最终证据由 root 集中提供，目前 PENDING；033 被停止的编译明确不是 PASS，不能用作最新基点编译证据。这属于验收证据待补，不记录为产品缺陷。

## 实际节点与代码依据

下列路径相对仓库根；Java 服务简称均位于 `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/`，XML 位于该模块 `src/main/resources/`，前端位于 `IntRuoyiFronted/`。

| 业务节点 | 实际代码和结论 |
| --- | --- |
| 表单填写人配置入口 → 正式规则保存 | `controller/admin/pro/batchrecord/MesProEdhrProcessFormPermissionRuleController.java:53–57` 的 `/save-by-report` 保留标准 update 权限并调用正式 service。`service/pro/batchrecord/MesProEdhrProcessFormPermissionRuleServiceImpl.java:242–287` 是有回滚事务的正式保存入口，校验填写规则/分配互斥、候选规则和责任行，保存规则并绑定权限后调用 `reconcileProcessFormFillTaskOwnership`。476 的 `:294–311` 会删除同 report/version 的工序级填写规则并保存表单级规则；mapper `dal/mysql/pro/batchrecord/MesProEdhrProcessFormPermissionRuleMapper.java:143–153` 只删除指定 report/version、非表单层的 FILL/EQUIPMENT_FILL/QUALITY_FILL，并不删除任务或改变任务状态。 |
| 规则读取/权限绑定 → 下游填报权限 | 476 新增规则读取 `MesProEdhrProcessFormPermissionRuleServiceImpl.java:126–140`：当前工序规则不存在填写类别时，从同 report/version 的表单级正式规则取得 fillRule，签名规则仍从原 rules 取得。`bindPermissionScope:579–617` 保留既有非 VIEW/FILL ability、父 scope 和 expectedVersion，再保存新的 VIEW/FILL 权限；不是把审核权限随填写人一起覆盖。F02 不根据规则接口虚构待办，仍读取正式任务 owner/candidate 快照。此处改变了配置来源，不能描述为行为完全未变；它不改变 F01/F02 的个人待办谓词。 |
| 正式规则 → 任务所有权迁移和落库 | `MesProEdhrWorkTaskServiceImpl.java:1467–1527` 的 reconciliation 仍为回滚事务；先从 mapper `:278–285` 取正式 FILL/REWORK 且 TODO/DOING/OVERDUE 任务，校验同 report/version/route 责任目标（service `:2581–2600`），空责任来源保持既有拒绝/跳过规则，ownershipLocked 拒绝。476 `:1494–1496,2608–2623` 新增的允许迁移必须是表单级 FILL 规则、FILLER 来源、任务和 batchTask 同 routeProcess，且旧 `ROUTE|routeProcess|report|version` 与新 `FORM|report|version` 完整匹配。不会凭表单名或部分 key 转移其他任务。 |
| 迁移落库 → 正式候选、动态权限、导航 | reconciliation `:1502–1526` 写入负责人、正式候选来源/快照、责任版本/digest、更新时间、正式 actionUrl 和可选 dueTime；不写 task status、task id、batchTaskId、batchExecutionId 或 executionId。`MesProEdhrCandidateResolver.java:74–82,235–247` 从正式候选来源解析启用用户、去重排序，以无空白的逗号 ID 序列形成快照，空池/非法源抛错。落库后 service `:1522–1525,2626–2642` 重新读取并同步运行期 entitlement，负责人变化时发送正式通知。因此迁移后旧用户可能失去待办、新正式用户获得待办，这是共享数据更新的预期结果，不是 F02 静态集合保持不变。 |
| F01 my-page → 同一 owner/status/batch 合同 | `MesProEdhrWorkTaskController.java:36–40` 保留 work-task-query OR batch-execution-query。`MesProEdhrWorkTaskServiceImpl.java:163–178` 从当前登录身份查询，includeOverdue 与单独 status 互斥，状态仅 TODO/OVERDUE。mapper `MesProEdhrWorkTaskMapper.java:31–46,358–367,387–391` 仍调用共享 owner/batch 谓词。`MesOpenWorkTaskVisibility.java:7–28` 仍是本人 OR 逗号完整 token 候选；排除批次40/50/60，批次30仅 ARCHIVE 保留，非ARCHIVE终态排除。任务所有权迁移不改变这些谓词，也没有把 DOING 纳入个人待办。 |
| F02 读取 → SQL/count/chunk/正式行 | `service/pro/batchrecord/MesEdhrWorkbenchSource.java:9–20` 直接调用 mapper.countEdhr/chunkEdhr，不调用规则保存、batch.openOrCreate、syncIfActive 或业务写入；chunk仍100/fetch101。`mapper/profileworkbench/MesWorkbenchTodoMapper.xml:4–16` 从正式任务读取 task id/状态/日期/完整导航身份，显式 tenant/deleted、TODO/OVERDUE，并绑定同一 MesOpenWorkTaskVisibility OWNER_SQL/OPEN_BATCH_SQL；count/chunk包含同一base和共享where。迁移后的 owner/candidate/dueTime/actionUrl 由下一次完整查询读到，未引入独立候选缓存或基于规则推断任务。 |
| 页面行 → 正式导航 → 处理权限 | `src/views/Profile/components/ProfileWorkbench.vue:436–440` 将 source 返回的导航交给既有 `src/utils/edhrWorkTaskNavigation.ts:244–263`；无执行记录的填写/返工通过正式 openEdhrBatchTask，已有执行记录使用正式执行表单路由。API `src/api/mes/pro/edhr/batchExecution.ts:948–952` POST `/task/open`；`MesProEdhrBatchExecutionController.java:254–258` 保留 batch-execution-update 标准权限。service `openTask:3374–3423` 复核批次允许状态、批次任务归属和前置节点，再取得当前正式 openWorkTask。`:4030–4073` 要求 workTaskId 匹配当前任务、正式本人/候选和 FILL scope ability；`MesProEdhrWorkTaskAuthorization.java:20–32,51–69` 以 Long token 集合精确判断候选，非法 ID 抛错。配置迁移没有绕过正式处理权限。 |
| 迁移 actionUrl → 原下游对象保持 | `MesProEdhrWorkTaskServiceImpl.java:1516,2923–2959` 用原任务身份重建 actionUrl；填写仍绑定同一个 workTask/batchTask/batchExecution/execution，归档仍使用批次详情和 workTaskId，审批仍使用正式执行审批入口。`MesEdhrWorkbenchSource.java:22–35` 只保留这些正式字段给前端 navigator。没有将“工序开始”“批记录表单”“表单槽位”相互推断或补齐的新读取链。 |
| 批次复用入口 → 状态同步 → 审计事务变化 | `MesProEdhrBatchExecutionServiceImpl.java:728–790` 的正式 openOrCreate 仍在回滚事务内，经当前 tenant 的 authoritativeContextResolver + entryContract、既有上下文校验后调用 openExistingBatch。`:1385–1398` 先核对既有 provisioning，再 syncIfActive、重新读正式批次、返回正式响应。476 唯一该服务生产差异是 OPEN audit 传入 callerTransaction=true。`:9044–9077` 经独立注入 operationAuditService 转至 `MesProEdhrOperationAuditServiceImpl.java:41–60` 的 REQUIRED/rollbackFor 方法，审计写失败抛错，参与原调用事务；不再调用 REQUIRES_NEW `:35–38`。这是业务写链审计原子性改变，不是 F02只读RR事务改变。 |
| 批次同步 → 任务生产和 ARCHIVE 终态 | `syncIfActive:6999–7015` 仍只对符合既有 active 条件且非 stage4 marker 批次执行 ensureRouteFormTasksPresent/syncBatchStatus；`:7028–7065` 在实际补入节点后通过 createInitialFillTask 产生正式任务。这些既有方法本次未改变，F02 adapter 不调用它们。关闭批次 `:4806` 仍生成 ARCHIVE，质量拒收 `:4848` 仍取消活跃任务，归档处理 `:4903,4942` 仍校验/完成正式归档任务；常量 `:229–232` 的30/40/50/60与共享查询谓词一致。OPEN审计事务改变未改写这些终态语义。 |

## 六 AC 边界结论

- AC1：此次正式 owner/candidate/dueTime 更新由 eDHR 完整base/count/chunk直接消费；其余四来源不在此次边界变更内。完整来源合同保留。
- AC2：任务 id/namespace 保持，hidden tenant/user/taskKey 仍绑定原正式任务；不产生历史隐藏行或将隐藏记录作为新的任务来源。
- AC3：变更可能更新 dueTime，从而影响下一次正式排序；它仍是原 canonical due_at 输入，不改变 SQL/Java tuple、after 游标或有效页规则。
- AC4：正式配置和审计失败继续抛错；F02 adapter 未改走写服务，未增加 partial-success/异步写入。round4 的页面 generation、shared badge epoch/会话 teardown 守卫仍适用。
- AC5：查询 owner OR候选、标准 OR权限、正式处理权限及导航上下游已重新核对；F01 TODO/OVERDUE与批次30的ARCHIVE例外保持。
- AC6：共享写服务 REQUIRED审计变更未进入 F02 source→mapper 读链，故不改变已审查的 master外层→不同bean readOnly RR、同步100/101缓冲和深页前缀设计。

## required_changes

源码：无。

验收证据：root 补齐最新476基点及当前F02代码的后端compile依赖闭包、types实际基线对照与目标lint最终结果。该证据不由本 reviewer 执行或认证。

## non_blocking_suggestions

无新增建议，避免扩围。已知运行边界继续明示：SQL实际执行、部署引擎/数据源一致性、真实RR/并发、容量/性能、真实行为/页面/E2E全部 NOT RUN/UNVERIFIED，本轮不把这些未要求项作为 blocker。

## final_decision

pass（限定本次共享eDHR边界静态审核；与round3/round4合并使用）。没有发现476使原六AC或F01共享口径失效。最终总体放行的compile/types/lint证据仍 PENDING，不声称运行或编译已通过。本 reviewer 未改源码、未运行测试/构建/服务/网络/DB/SQL、未做Git mutation；仅写本补充报告，保留原round4。
