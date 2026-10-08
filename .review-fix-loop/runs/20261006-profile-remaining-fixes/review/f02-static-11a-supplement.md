# F02 11a owner reconciliation 边界补充审核

2026-10-08。只读审核 `476ec5322ab2b8d76ad67308a50eb6c08f980533` → `11a701bb573ad46c49cac57122522499217e0324` 的正式所有权 reconciliation 改动；保留 round4 和 476 supplement。主干读取时 HEAD 已为 b05da02a520d5d0bf146cfa69b8ac74eaea23b7b，因此共享服务用 `git show 11a:path` 精确读取，未将更晚主干误称11a。任务worktree当时处于 rebase start、F02新增源码暂未重放，F02消费链用该任务稳定实施提交 `d16ff384a68eb69f5b8691e202b617e8b7cd8349` 精确读取，结合11a正式生产者审核，不用临时checkout缺文件判断产品缺陷。最终重放/融合与最新compile由root提供证据。

logic_status: pass（限定本次共享边界静态审核）

usability_status: pass（限定本次共享边界静态审核）

ui_status: pass（static only）；rendered_UI/runtime: NOT RUN

## blocking_issues

无本次共享所有权变更引入的F02/F01阻断问题。最新融合基点compile/types/lint证据仍 PENDING，不把待编译作为产品缺陷，也不把SQL/RR/容量/真实页面未运行作为本轮门槛。

## 实际节点与依据

Java相对路径以下均位于 `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/`；XML位于该模块 `src/main/resources/`。行号来自上述明确提交快照。

| 节点 | 代码与判断 |
| --- | --- |
| 正式配置保存 → reconciliation | 既有 `service/pro/batchrecord/MesProEdhrProcessFormPermissionRuleServiceImpl.java:242–287` 正式saveRuleByReport在回滚事务中保存/绑定规则，再调用工作任务service；标准update权限仍在 `controller/admin/pro/batchrecord/MesProEdhrProcessFormPermissionRuleController.java:53–57`。11a修改位于工作任务reconciliation内部，不新增F02读链调用或从前端传owner的入口。 |
| 任务目标与旧来源匹配 | `MesProEdhrWorkTaskServiceImpl.java:1467–1500` 仍为@Transactional rollbackFor Exception；先验证输入report/version/候选合法性，再遍历正式活跃FILL/REWORK。任务batch/form责任目标、来源key匹配、仅允许正式ROUTE→FORM迁移和ownershipLocked拒绝均保持。入口对传入fillRule候选的检查仅作为合法性验证，不再直接作为落库候选。 |
| 启用正式规则集合 → 禁用skip | `MesProEdhrWorkTaskServiceImpl.java:1501–1512` 按fillRule的routeProcess/report/version读取 `selectEnabledFillRules`。mapper `dal/mysql/pro/batchrecord/MesProEdhrProcessFormPermissionRuleMapper.java:57–60,169–178,192–198` 实际限制同routeProcess/report/version、正式填写类别与enabled=true，并稳定排序。skip要求集合为空、传入rule确实enabled=false且有id，并在同作用域持久化规则集合中找到同id且禁用；不能仅凭传入disabled标记跳过正式责任。该分支保持原任务快照，不将禁用配置提交的候选转为任务所有者。 |
| 正式规则集合 → 完整责任snapshot | `MesProEdhrWorkTaskServiceImpl.java:1513–1518` 调用正式 `buildProcessFormResponsibilitySnapshot`，并要求生成sourceKey与请求责任key严格相等，不符抛错。builder `:2452–2498` 空集合失败，每条正式规则调用candidateResolver，解析正式候选；TreeSet聚合去重排序用户，scope逐条保存candidate来源/解析用户/fillableScope；单规则保留正式来源，多规则用ASSIST_ROWS和用户并集；sourceVersion来自正式规则版本，digest由完整scope snapshot JSON计算。sourceKey `:2411–2429` 由正式rule层级、batchTask bindingKey和version构成；无sourceKey匹配的配置不能转移任务。 |
| scope/候选异常 → 失败 | `MesProEdhrWorkTaskServiceImpl.java:2501–2520` 对动态表单使用正式batchTask fillableScope，对其他正式规则使用rule scope；按既有ALL规则构造正式报告scope，缺少/非法scope报错。`MesProEdhrCandidateResolver.java:74–82,235–247` 仍解析正式候选来源并生成启用、排序、无空白完整ID快照，空候选池失败。11a没有异常吞掉或默认成功分支。 |
| 完整snapshot → 正式任务落库/权限同步 | reconciliation `:1519–1544` 从responsibility.candidate写负责人和候选快照，同时写responsibility sourceKey/version/digest/scopeJson。可选dueTime依然按正式fillRule.dueMinutes更新；actionUrl依然按原正式task重建。未写status、task id、batchTaskId、batchExecutionId、executionId。落库后重新读取正式任务，执行原syncRuntimeTaskEntitlement，所有者变化走原通知。11a修正了多填写规则候选和责任范围的一致性，确实可能改变用户待办归属；不应称集合完全没变化。 |
| F02 source → SQL/count/chunk | 任务稳定实施提交中的 `service/pro/batchrecord/MesEdhrWorkbenchSource.java:9–20` 只调用正式mapper.countEdhr/chunkEdhr，未调用reconciliation、规则保存或批次sync。`mapper/profileworkbench/MesWorkbenchTodoMapper.xml:4–16` 直接读取正式task owner/candidate/dueTime和navigation身份，显式tenant/deleted/TODO/OVERDUE，count和chunk同base/where。新责任scopeJson不被当作F02任务源，F02不自行解析配置来扩充待办；更新后的正式用户集合和dueTime自然由后续完整查询消费。 |
| F01 my-page → 共享owner/status/batch谓词 | 任务实施提交的 `MesProEdhrWorkTaskMapper.java:31–46,358–367,387–391` 将my-page本人/候选及批次过滤委托给 `MesOpenWorkTaskVisibility.java:7–28`，F02 SQL绑定同一常量。本人OR逗号完整token候选、TODO/OVERDUE、批次40/50/60排除、批次30只允许ARCHIVE保持。11a规则聚合产出的排序逗号ID与这一精确token格式一致，不把DOING扩为个人待办，也不改变ARCHIVE终态例外。 |
| 正式navigation → 实际处理权限 | `IntRuoyiFronted/src/views/Profile/components/ProfileWorkbench.vue:436–440` 仍将源navigation交给既有navigateToEdhrWorkTask；任务source `MesEdhrWorkbenchSource.java:22–35`保留正式id/type/action/batch/task/execution/scope身份。正式 `/task/open` 的标准batch-execution-update权限和正式批次/task归属、状态、前置节点校验保持（`MesProEdhrBatchExecutionController.java:254–258`、service `:3374–3423`）。11a快照中service `:4038–4073` 仍要求workTaskId匹配当前open task、正式本人/候选与FILL ability；既有assist和显式管理bypass条件未变。新的candidate/scope进入正式处理权限链，没有让F02的展示替代业务授权。 |

## 六AC影响

AC1完整五源中的eDHR正式集合由同一任务快照读到；AC2隐藏仍按原task身份，不产生隐藏记录任务；AC3新dueTime仍是原canonical排序输入，未改比较器/游标/有效页；AC4生产者异常抛出、F02 adapter未改写服务，原页面generation/shared badge epoch守卫保持；AC5标准权限、正式身份导航、F01owner/status/batch共享谓词已核对；AC6新写服务snapshot构造不进入F02的master→独立readOnly RR bean→同步mapper链，原100/101缓冲和深页成本合同未改变。此处是边界影响推导，完整静态结论仍与round3/round4/476 supplement合并使用。

## required_changes

源码：无。

证据：root确认任务源码重放后的最终基点和范围，并补齐最新compile/types/lint最终结果；本审核不认证正在重放的checkout已完成，也不以被停止的旧编译充当PASS。

## non_blocking_suggestions

无新增建议；仅维持已有未验证边界记录。

## final_decision

pass（本次11a共享owner reconciliation静态边界）。未发现原F02六AC或F01共享谓词失效；本报告不是最新融合产物compile通过的声明。SQL实际执行、真实RR/并发、容量、真实行为/页面/E2E全部 NOT RUN/UNVERIFIED，未新增门槛。未修改main/源码，未运行测试/build/服务/网络/SQL/DB或Git mutation；仅写本任务review补充报告。
