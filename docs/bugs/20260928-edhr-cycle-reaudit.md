# eDHR 连续运行完整复审报告

## 结论与证据边界

本轮发现 6 项新增缺陷（EDHR-FLOW-16～21），并把既有 EDHR-FLOW-07 缩小到一个有源码依据的条件性阻塞场景，共 7 项需要处理。不是每项都会阻止所有订单；其中资料节点完成、更正属于局部路径阻塞。报告为静态代码分析，没有实际运行复现、数据库修改、E2E、部署或新一轮测试。此前 08～15 的修复与 607 项回归通过属于上一轮证据，不代表本轮问题已修复。

审查基于当前工作区文件（包含未提交改动），参考 HEAD：0ece51c1cdb000986c23c0ef72ebe5c31db4d8e7。下文路径相对仓库根目录，行号仅定位；换电脑或代码变化后应按方法锚点重新核对。

## 业务链与覆盖

ERP 工单同步 → 工艺路线及 QA 正式配置 → 生产/PQC 组长配置人员 → 加入活跃工单并冻结执行配置 → 一线生产/PQC 提交 → 组长复核与双 100% → 完工生成正式回执/批记录 → PQC 生产放行 → 多人上传保存资料 → 上市放行 → 系统历史追溯。

同时检查移除再加入、重建、报工更正、拒绝返工、重试及 ERP 再同步。输入缺失、无合法负责人、签名失败、进度未达标属于业务前提，不冒充代码 BUG。上市放行后已有系统追溯，不增加人工归档负责人或 PDF 归档门槛。

代码路径简写：B = `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/`；F = `IntRuoyiFronted/src/views/mes/pro/`。

## 完整问题列表

| 编号 | 优先级 | 触发条件 | 卡点及恢复边界 |
|---|---|---|---|
| EDHR-FLOW-16 | P1 | PQC 放行前在批次详情运行放行预检，预检失败 | 预检占用了放行事务，随后正式 PQC 放行拒绝创建事务；刷新重试无效，失败预检也不能直接撤回 |
| EDHR-FLOW-17 | P2，局部流程阻塞 | 按先 PQC 放行、后上传资料的顺序操作 | PQC 放行立即进入上市审批锁，资料节点无法完成；宽松上市入口仍可能直接放行，不是所有上市放行必失败 |
| EDHR-FLOW-18 | P1，多人协作条件 | 不同负责人各上传了自己节点的待保存资料 | 保存接口检查整批所有节点权限，互相卡住；上市负责人也不能替所有人保存 |
| EDHR-FLOW-19 | P1，特定操作组合 | 已单独生成完工回执，尚无放行申请时重建或移除再加入 | 源记录被清掉，旧回执仍占用同一活跃订单；重新做满进度后仍不能正常完工/申请放行 |
| EDHR-FLOW-20 | P2，必须更正时阻塞 | 一条多物料报工中，两物料用了同一损耗原因 | 初次提交合法，更正却报重复原因；只改参数也失败；原记录可接受时仍可继续复核 |
| EDHR-FLOW-21 | P1，当前上传主入口 | 非本单生产组长的资料人员从批记录列表点“其他上传” | 页面丢失申请上下文，资料列表/上传被错误阻止；同根因使 PQC 无法预览组长上传的文件 |
| EDHR-FLOW-07（补充） | P1，条件性 | 单独完工生成回执后、申请放行前，ERP 改了同一工单的批号 | 再申请放行重算来源与旧回执不符；不应泛化为每次 ERP 同步都会失败 |

入口范围必须区分：16～18 位于仍可从批记录列表的批次编号、工单号、路线名称进入的旧批次详情（F`edhr-batch/BatchExecutionListPage.vue:101/112/135` → `openDetail:1737`）。当前“其他上传”按钮则进入新的活跃订单详情，使用独立 dossier-files 服务，不走 16～18 的附件保存链；21 直接影响这个当前主入口。不能用旧详情问题概括所有上传方式。

### EDHR-FLOW-16：提前预检与正式 PQC 放行争用事务

- 入口：F`edhr-batch/BatchExecutionDetailPage.vue:2675` 的 `canRunReleasePrecheck` 允许进行中批次预检；`4951` 起调用真实预检接口。
- 服务：B`service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:404` 的 `precheck` 未先验证活跃订单 PQC 已完成，无当前事务时创建事务；资料不足写成 `PRECHECK_FAILED` 并正常保存。
- 冲突：B`service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java:94` 发现任何 current 事务就拒绝；该服务由 B`service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:185` 的正式批准调用。B`dal/mysql/pro/batchrecord/MesProEdhrReleaseTransactionMapper.java:24` 的 current 查询只排除 REJECTED/WITHDRAWN。
- 恢复：ReleaseService 的 `finalizeReject:312` 不接受 PRECHECK_FAILED，`withdraw:365` 只接受 PENDING_APPROVAL；普通重试不恢复。先补全资料、重新预检通过再退回可能形成迂回路径，因此不称永久死锁。
- 修改方向：活跃订单的放行事务由正式链路唯一创建；提前预检只读评估，或制定可验证的事务接管规则，不能忽略/删除竞争事务。
- 验收：Given 已完工且未 PQC 放行，When 提前预检失败后再由 PQC 正常批准，Then 正式链继续且只存在一个有效放行事务。

### EDHR-FLOW-17：上市审批锁提前锁住资料完成

- PQC 正式批准直接初始化管理者审批；ManagerStageInitializerImpl `208` 创建 PENDING_APPROVAL，PqcProductionReleaseServiceImpl `158–198` 不再建立旧四报告待办。
- B`service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:2984` 的普通资料节点起始 WAITING；前端 F`edhr-batch/BatchExecutionDetailPage.vue:5660` 完成调用真实 `completeEdhrBatchSpecialNode`。
- 完成服务 `2045` → `validateSpecialNodeTaskBeforeRelease:2947` → `requireBatchActionUnlocked:6922` → `requireReleaseActionUnlocked:6915`，普通员工在 PENDING_APPROVAL 下被拒绝。特殊绕过权限不算正常恢复路径。
- 上传/保存附件（同服务 `2225`、`2365`）只保存附件及 payload，不会把资料节点变为 APPROVED（`2403`）。因此上传成功不等于节点可完成。
- 边界：ReleaseService `572` 明确 `materialGateRequired(false)`，仍保留宽松放行；本项不是要求加严资料门槛，与既有 01 的放行后补传问题分开。
- 修改方向：在当前宽松策略下，为上市审批期间的资料节点保留明确的授权完成路径；不解除整批所有修改锁。
- 验收：Given PQC 已通过且待上市放行，When 各负责人上传并完成自己的资料节点，Then 操作成功、正式记录可追溯，原宽松上市策略不被额外收紧。

### EDHR-FLOW-18：整批保存错误要求单人拥有全部节点权限

- F`edhr-batch/BatchExecutionDetailPage.vue:3976` 回填整批 pending 附件；`3924` 起上市操作前尝试整批保存。
- B`service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:2365` 的 `savePendingSpecialNodeAttachments` 在同一事务遍历所有节点（`2381`），对每个有 pending 附件的节点校验当前用户是负责人（`2392`；负责人校验在 `2772`）。
- 触发例：来料节点只有 A，灭菌节点只有 B；两人分别上传后，A 保存会被 B 节点拒绝，B 保存会被 A 节点拒绝，事务回滚。C 上市负责人也不能自动代存。
- 恢复：协调串行上传保存可能避开；已经交错上传后，没有按当前人/指定节点保存的正常接口。不得建议删除他人文件作为标准解决方法。17 又阻断了 PQC 后逐节点完成。
- 修改方向：保存请求明确指定本人有权操作的节点/附件范围；上市页面提示哪些负责人仍需保存，不尝试替所有人写入。
- 验收：Given 两个互不重叠的负责人均有 pending 附件，When 各自保存，Then 各自成功且不修改他人附件；上市操作不跨权限代存。

### EDHR-FLOW-19：重建源数据后仍保留旧完工回执

- 可达条件是“已单独完工、无放行申请”，不是普通一键完工全部用户必然遇到。F`processpool/TeamLeaderWorkbenchPage.vue:9976` 的 P2 生成调用正式完工流程；B`service/pro/simulation/stage2_5/MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java:216` 调用真实 completion。正式 Controller 也有独立完工入口。
- 完工只把 businessStatus 置 COMPLETED，activeStatus 仍 ACTIVE：B`dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:122`。
- F`processpool/TeamLeaderWorkbenchPage.vue:1763` 的重建按钮未按已完工禁用；B`service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:1569` 重建只检查有效活跃身份、放行申请及删除确认。`1825` 移除也只防已有放行申请。
- `cleanupActiveOrderRuntimeHistory:1642` 删除分配、完工进度、PQC 任务、快照及报工，但不处理 completion receipt；`rebuildRecoveredActiveOrder:2345` 移除再加入也复用同一 ID 并清理来源。
- B`service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java:103` 按 activeOrderId 查已有回执。新请求键直接冲突；旧键走 `matchesReceiptSources`，而原来源已被清掉或重建。`completeForRelease:59` 同样复用旧回执校验，不能开始新周期。
- 恢复：继续重报或重复重建不解决残留回执。未发现当前入口中的受控回执作废/新周期处理；不建议直接删数据库回执。
- 修改方向：已有完工回执时禁止破坏性重建/复用原 ID；如果业务确需重新生产，建立独立周期并保留旧证据。此为完整性保护，不是增加前期配置门槛。
- 验收：Given 有回执但无申请，When 重建或移除再加入，Then 要么无副作用明确拒绝，要么创建新周期且新周期可完工，旧回执与追溯完整。

### EDHR-FLOW-20：合法跨物料损耗在更正时被视为重复

- B`service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java:43` 对每物料独立规范化；`MesProFrontlineFeedbackSubmitServiceImpl.java:313` 拼接各物料损耗，`MesProFrontlineFeedbackPayloadSplitter.java:140` 落入正式事件 payload。两个物料同原因合法可保存。
- F`processpool/TeamLeaderWorkbenchPage.vue:9145` 的 `buildProductionCorrectionRequest` 同样 flatMap 物料损耗（`9158`）。
- B`service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java:218` 无条件解析旧损耗；`originalLossReasons:603` 发现同一 reasonId 出现两次即抛异常，新损耗列表 `220–226` 也要求全局唯一。因此只改其他参数仍失败。
- 边界：无法更正该正式报工；如果原始事实可接受仍可继续复核。拒绝后重新报工属于重新录入，不等于更正功能恢复。
- 修改方向：总层按原因合计，物料层保留独立事实，读取历史合法多物料记录时使用同一正式口径。
- 验收：Given 两物料使用同一损耗原因，When 只改参数或更正数量，Then 更正成功且每物料独立数量、总层损耗与审核证据一致。

### EDHR-FLOW-21：当前“其他上传”丢失业务上下文

- F`edhr-batch/BatchExecutionListPage.vue:1900` 的“其他上传”进入 `BatchExecutionActiveOrderDetailPage.vue`，该页 `17–24` 没有向 `ActiveOrderSubmissionDetailPanel` 传 `pqc-release-application-id`。
- F`processpool/components/ActiveOrderSubmissionDetailPanel.vue:3947` 的列表与 `3975` 的上传只从此 prop 取 applicationId。该入口最终请求没有 applicationId。
- B`service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:224` 的 `resolveContext` 在 applicationId 为空时只准当前工单生产组长。即使资料员工拥有批记录上传权限，仍被报“当前用户不是该活跃订单生产组长”。刷新不恢复。
- 同根因第二表现：组长从组长页上传，`135` 将附件 applicationId 存为空；PQC 页带申请 ID 能列出文件，但预览 `requireReadableFile:98` 又使用附件自身的空 applicationId 做权限校验，导致看得见而打不开。合并计一项，不重复计数。
- 恢复边界：组长代上传只能人为绕行；PQC 专页正确传 ID，但不能要求只有资料上传职责的员工改用 PQC 审核页面。
- 修改方向：从正式关联提供完整申请/订单上下文，并以业务候选人和明确上传授权校验；不能仅通过带任意 applicationId 绕过权限。预览须按文件正式归属及当前读取人的合法业务身份授权。
- 验收：Given 组长、PQC、独立资料人员分工，When 从列表其他上传、PQC 页、组长页分别上传或读取，Then 合法人员成功；组长上传的文件 PQC 可预览，无关用户仍被拒绝。

### EDHR-FLOW-07：ERP 改批号使已生成回执无法再用于申请

- 狭窄前提：P2/独立完工已成功，尚未创建放行申请。ERP 同一来源再次同步更新为另一非空批号。
- B`service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java:165` 的 `syncExistingWorkOrder` 直接更新既有工单；`buildUpdatedWorkOrder:413` 更新 batchCode，`resolveBatchCode` 接受新非空 ERP 批号，未检查正式完工回执。
- B`service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java:700` 的 `canonicalSourceSeed` 把实时 workOrder.productId/batchCode 放入来源摘要；`matchesReceiptSources:105` 重新 prepare，与旧回执比对。
- B`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java:114` 无既有申请时调用 `completeForRelease`；CompletionService `129` 起的来源比对会失败。同步本身完成，用户到申请放行才遇到冲突。
- 边界：同值重复同步不会因此失败；只有数量变化不能由这段证据断言相同冲突；已有申请走 replay 分支也不属于这里的触发条件。恢复旧批号是否足够取决于其他来源是否变化，不作为保证。
- 修改方向：定义已完成执行的身份冻结边界，ERP 更新不得悄然改变既有周期正式身份；通过明确的新周期/变更处理承接更新，不禁止所有 ERP 更新，也不跳过证据校验。
- 验收：Given 已完工未申请，When ERP 改批号，Then 同步阶段明确处理冲突或生成新周期；原周期仍能按冻结身份申请放行。

## 历史清单去重与修复建议

- 08～15 及上一轮三个剩余修复不重复登记；本轮没有撤销上一轮定向测试结论。
- 01、02、04、05、06 仍是既有完善项，详见同目录 `20260928-edhr-flow-improvement-backlog.md`；本轮未把它们扩大为新的全链阻塞。07 使用原编号补充，不另造编号。03 维持撤回。
- 建议先处理当前主入口 21，再处理 16、18、19 和 07 的边界，随后处理 17、20；17/18 应一起核对旧详情多人资料流程。每项修复必须补对应真实服务组合回归，不能仅 mock 相邻服务成功。
- 本报告交付不代表修复完成。静态审计不能证明全系统不存在其他 BUG；真实账号权限、数据库迁移、运行数据和并发时序未在本轮实测。
