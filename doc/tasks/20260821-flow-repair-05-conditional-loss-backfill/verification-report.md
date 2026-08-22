# Verification Report

## Verification Scope

本报告验证流程5 task-owned 实现、主线融合和定向测试证据。未修改数据库、配置或测试数据；未启动服务，未运行数据库命令或写入型 E2E。

## Code Compliance Conclusion

**流程5实现符合专项条件分支目标；全链路仍受跨流程 owner 边界约束。**

- 正数损耗路径已校验正式来源、签名、原因、明细和五字段绑定快照，输出 `REQUIRED`、`hasActualLoss=true`、`lossQuantity>0` 并创建正式损耗单。
- 明确无损耗路径输出 `NO_LOSS`、`hasActualLoss=false`、`lossQuantity=0`、`lossReportStatus=NOT_REQUIRED`，不创建损耗单、零数量报告或 `lossRecordId`。
- 缺失事实、绑定、签名或映射时输出阻塞，不生成成功 receipt；不能从缺少 `lossRecordId` 推断无损耗。
- task-owned commit `24fdf7767ac02c4b6d4a3c4709194e195fea624a` 已由主线集成提交 `16e47106e043ad93b4d43d699d269996703a47e1` 融合；代码/测试验证基线为 `fd7566c3ef3c8fea3adcc0e73cb23d2c86d66cf8`，已确认其祖先关系；其后仅追加流程5文档和复验经验收尾提交，未改变代码验证结果。

## Document Structure Verification

预期五份文件均存在，且覆盖任务目标、目标态、当前事实、根因、修改边界、流程 1 五字段正式来源、接口/数据/状态、订单/工序 `hasActualLoss`、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚和流程修复 4/6/7/8/10/11 的跨线程契约。

## Read-only Checks

- 已用 rg 定位正式来源章节、损耗 writer、source reader、dossier 条件门禁及相关单测。
- 已核对五份文档均使用 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` 作为领料绑定合同，并声明流程 5 只读校验、不创建或猜测关系。
- 已核对五份文档均冻结逐工序/订单级 `hasActualLoss`：正损耗工序决策为 REQUIRED，必须为 true 且 lossQuantity>0、建损耗单并使 receipt 的 `lossReportStatus=SUCCESS`；无损耗工序决策为 NO_LOSS，必须有正式零损耗确认快照、为 false 且 lossQuantity=0，receipt 的 `lossReportStatus=NOT_REQUIRED` 且不生成 lossRecordId；不能从缺少 lossRecordId 推断 false。
- 已核对接口/错误码合同：绑定快照缺失或变化返回 `LOSS_SOURCE_PICK_LIST_BINDING_REQUIRED` / `LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED`，`hasActualLoss` 缺失或矛盾返回 `LOSS_HAS_ACTUAL_LOSS_REQUIRED` / `LOSS_HAS_ACTUAL_LOSS_CONFLICT`，均 fail fast。
- 主线 `mvn -pl yudao-module-mes -DskipTests compile`（Maven 3.9.16，当前 HEAD）-> PASS。
- 主线流程5核心 JUnit（splitter、dynamic-form、writer、source-reader）共21项（当前 HEAD）-> PASS。
- 主线 `git diff --check` -> PASS；runtime v5 guard -> PASS（int_main 8081/48081）。
- 组合测试中的 `MesFrontlineRuntimeConfigProcessScopeTest` 1项失败，属于前线运行时参数校验静态契约，不属于流程5条件损耗 owner；流程5核心21项不受影响。
- UTF-8 文档写入使用 apply_patch；未使用 PowerShell 重定向写入中文文件。

## BDD/TDD Evidence Status

BDD: 正数损耗按工序建单 -> Given 双100、五字段领料绑定快照和正式正损耗来源完整，When 流程 4 完成节点调用流程 5，Then 仅该工序生成损耗单且 `hasActualLoss=true`、lossQuantity>0、工序和订单 receipt 的 `lossReportStatus=SUCCESS`。
BDD: 无损耗不建单 -> Given 正式无损耗事实确认、五字段领料绑定快照完整且数量为0，When 流程 4 完成节点调用流程 5，Then 流程 5 返回 NO_LOSS/`hasActualLoss=false`/`lossQuantity=0`，流程 4 工序和订单 receipt 的 `lossReportStatus=NOT_REQUIRED`，且不创建损耗单或任何无损耗报告。
BDD: 部分工序损耗 -> Given 工序间决策分别为 REQUIRED/NO_LOSS，且各自五字段绑定快照完整，When 流程 4 完成节点调用流程 5，Then 只为 REQUIRED 建单，NO_LOSS 工序显式 false/0 且 `lossReportStatus=NOT_REQUIRED`，订单 receipt 的 `hasActualLoss=true` 且 `lossReportStatus=SUCCESS`。
BDD: 原子失败 -> Given 任一工序阻塞或损耗写入失败，When 流程 4 完成节点执行，Then 三类回填和完成 receipt 整体回滚；流程 6 的建批失败不重跑流程 5。
BDD: 阻塞不建批 -> Given 任一工序缺失正式事实或绑定快照，When 流程 4 统一回填，Then 输出 BLOCKED，不生成成功 receipt，流程 6 不得驱动批次执行。
RED: 实现前零损耗旧合同测试 -> FAIL，返回 ZERO_LOSS_CONFIRMATION_UNSUPPORTED；作为基线根因证据保留。
GREEN: mvn -pl yudao-module-mes -DskipTests compile -> PASS；流程5核心 JUnit 21 项 -> PASS。
REGRESSION: git diff --check -> PASS；branch-runtime-port-guard.ps1 -> PASS；服务、数据库迁移和写入型 E2E 保持 NOT_RUN。

## Unresolved Blockers

1. `MesFrontlineRuntimeConfigProcessScopeTest` 主线组合失败，属于前线运行时参数校验静态契约，不属于流程5 owner。
2. 流程4订单级 receipt 持久化、流程6建批消费、流程7映射、流程8四材料、流程10最终放行和流程11迁移/全链路回归仍需跨线程验证。
3. 历史完成记录缺少正式损耗事实或五字段绑定快照时，仍须由流程11迁移门禁阻塞。

## Final Conclusion

流程5实现切片、主线融合和核心验证已完成，可结束本专项。结论仅覆盖“正数损耗建单、明确无损耗不建单、阻塞不成功”的流程5条件分支；不得将其扩展为流程4/6/7/8/10/11 全链路放行，也不得以 mock、API-only、直接 SQL、缺少 lossRecordId 或任何无损耗报告代替正式行为证据。

## Closeout Evidence

task-closeout-cleanup preview/apply 已通过；五份正式文档均保留，清理删除项为零。流程5代码/测试已融合并在当前 `int_main` 完成 compile、核心21项 JUnit、diff-check 和 runtime guard；数据库迁移、服务和写入型 E2E 仍 NOT_RUN，跨流程 blocker 仍按上文保留。
