# 任务：MES 缺料判定固定使用生产用料清单

- Task ID: `20260630-mes-material-shortage-use-production-material-list`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `blocked`
- User Request: `需要改成通过生产用料清单来判定缺少什么物料,这个要定死`
- Follow-up Request: `检查是不是全部改成了生产用料清单来判定缺少什么物料` / `继续`

## Task Goal

把 MES 排产相关缺料判定逻辑统一改为固定使用 ERP 生产用料清单 `mes_kingdee_production_material_list` 作为物料需求来源，不再使用本地工单 BOM 展开结果作为缺料判定依据；若生产用料清单缺失或未映射，必须 fail-fast 暴露问题。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-approval-center-tenant1-visibility-fix\task.md`
- 状态：`blocked`
- 处理说明：已按用户切换优先级阻塞，当前转入 MES 自动排产缺料判定口径改造。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs\powershell-memory.md` 与 `docs\integrations\kingdee-erp-official-docs.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、执行日志、测试输出统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - ERP 生产用料清单口径必须以当前已落地同步表和本地映射代码为准，不把未证实推断写成事实。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。生产用料清单缺失时直接暴露阻塞，不回退到本地 BOM 口径。
- `是否从根因和长期维护角度解决`：是。统一自动排产缺料判定口径，固定以 ERP 生产用料清单为准。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 缺料判定固定使用生产用料清单 -> Given 自动排产计算某批生产工单缺料 / When 生成物料需求与短缺问题 / Then 需求量必须来自 mes_kingdee_production_material_list.requiredQuantity，而不是本地工单 BOM 展开。`
- `BDD: 生产用料清单缺失时 fail-fast -> Given 某生产工单存在有效排产范围但没有已映射的生产用料清单 / When 执行自动排产预览或应用 / Then 系统必须生成明确问题，不允许静默回退到本地 BOM 口径。`
- `BDD: 生产用料清单重复物料按物料汇总 -> Given 同一生产工单的生产用料清单存在多个相同 childMaterialId 分录 / When 计算缺料 / Then 系统按 childMaterialId 汇总 requiredQuantity 后与库存台账比较，并输出 requiredQty/availableQty/shortageQty。`
- `BDD: 排程日历缺料汇总也固定使用生产用料清单 -> Given 排程日历按天展示物料占用与缺料汇总 / When 构建每日物料需求行与剩余库存 / Then 需求量必须来自 mes_kingdee_production_material_list.requiredQuantity，而不是本地工单 BOM 展开。`

## Milestones

1. M1：阻塞上一后端任务并建立本次任务文档。`completed`
2. M2：补 RED 测试锁定“当前仍使用本地 BOM 口径”的问题。`completed`
3. M3：实现自动排产缺料判定改为生产用料清单口径。`completed`
4. M4：完成自动排产定向测试、证据文档与任务收口。`blocked`
5. M5：复核“是否全部切换为生产用料清单口径”。`completed`
6. M6：把排程日历缺料汇总链路统一切换到生产用料清单。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-mes-material-shortage-use-production-material-list\backend-api-evidence.md`

## Current Blockers

- 当前 `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` 被工作区内既有审批中心编译错误阻塞：`MesProEdhrApprovalTaskAdapter`、`MesProFeedbackApprovalTaskAdapter` 引用的 `ApprovalTaskQueryContext/ApprovalTaskTimelineQueryContext.isGlobalView()` 在当前源码中不存在，导致测试尚未执行到本任务用例。
- 已完成生产代码切换与定向编译验证；待不相关审批中心编译阻塞解除后补跑更完整的目标单测与证据校验。
- 定向真实测试中，`MesProAutoScheduleContractTest.preview_shouldIgnoreMissingRouteScheduleOrderAndKeepAutoSchedulingReadyOrders` 仍按旧预期要求“忽略不可自动排产的请求工单”；而当前服务 `validateRequestedScheduleOrderScope(...)` 会 fail-fast 拒绝这类请求。该冲突属于当前主干既有 contract 行为与测试预期不一致，不是本次生产用料清单口径切换引入的新问题。
- 当前线程用户优先级已切换到“排程日历正式排程为空空态回归修复”；本任务保留当前证据并显式阻塞，待用户恢复该主题后再继续补完整收尾验证。

## Final Verification Result

- 局部编译验证通过：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.includes=**/MesProAutoScheduleServiceImpl.java,**/MesKingdeeProductionMaterialListMapper.java,**/ErrorCodeConstants.java -DskipTests compile`
- 统一后局部生产编译验证通过：`mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java,**/MesProAutoScheduleServiceImpl.java,**/ErrorCodeConstants.java -DskipTests compile`
- 统一后定向测试编译验证通过：`mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java,**/MesProAutoScheduleServiceImpl.java,**/ErrorCodeConstants.java -Dmaven.compiler.testIncludes=**/MesProScheduleCalendarServiceImplTest.java,**/MesProAutoScheduleContractTest.java,**/MesProAutoScheduleAlgorithmContractTest.java -DskipTests test-compile`
- 排程日历目标单测通过：`mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 完整目标单测待补：`MesProAutoScheduleServiceImplTest` 全量执行仍受当前分支不相关审批中心编译错误阻塞。

## Review Conclusion

- 当前排产相关缺料判定链路已全部切换为生产用料清单：
  - `preview(...)`、`replanPreview(...)`、`apply(...)`、`replanApply(...)` 最终都进入 `computeSchedule(...)`。
  - `collectMasterData(...)` 已改为调用 `buildProductionMaterialDemandMap(...)`，需求来源是 `MesKingdeeProductionMaterialListMapper.selectListByWorkOrderIds(...)`。
  - 缺少生产用料清单、子项未映射、应发数量缺失都会生成 `MATERIAL_DEMAND` 阻塞问题，并映射到 `PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED`。
- `MesProScheduleCalendarServiceImpl` 已改为通过 `MesKingdeeProductionMaterialListMapper.selectListByWorkOrderIds(...)` 构建每日物料需求与缺料汇总，不再读取本地工单 BOM 展开。
- `MesProScheduleCalendarServiceImplTest`、`MesProAutoScheduleContractTest`、`MesProAutoScheduleAlgorithmContractTest` 中与缺料口径相关的 BOM 依赖桩已同步替换为生产用料清单依赖桩。
- 本次复核检索 `schedule` 相关源码与测试后，已无 `getWorkOrderMaterialDemandMapByWorkOrderIds(...)` / `workOrderBomService` 在排产缺料判定链路中的残留引用。
