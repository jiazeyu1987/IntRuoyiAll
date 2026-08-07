# Execution Log

## User Intent

- 用户反馈：点击“一线生产”页签时报错 `工艺路线工序缺少正式工作站绑定，routeId=922119, processId=922985`。
- 目标解释：恢复该真实页面路径，同时保留正式工作站绑定的 fail-fast 约束，不做默认值或静默降级。

## BDD Scenarios

- BDD: 一线生产加载正式路线工序 -> Given 当前登录用户命中路线 `922119` 且工序 `922985` 属于该路线的可切换工序，When 用户点击“一线生产”页签加载设备账号工序列表，Then 后端必须从正式路线工序绑定返回存在且启用的工作站，并以业务码 `0` 返回工序候选。
- BDD: 正式工作站来源缺失时阻塞 -> Given 路线工序没有可追溯的正式工作站绑定，When 一线生产加载该工序，Then 后端继续明确失败，不过滤工序、不猜测默认工作站，也不使用工序开始、批记录表单或 `formBindings` 替代。
- BDD: 流程图展示工作站不得冒充正式绑定 -> Given 候选流程图节点同时包含正式绑定字段 `routeProcessWorkstationId` 和仅供展示的可用工作站字段 `workstationId`，When 候选版本被读取、保存并发布，Then 只能按 `routeProcessWorkstationId` 形成正式路线工序绑定，展示字段不得替代或补齐正式字段。

## Preflight Evidence

- Skill: 已读取 `bug-regression-fix-loop` 及 `bug-contract.md`，本任务必须先复现并记录 RED，再实施修复。
- Rules: 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/server-access.md`、`docs/release-backup-restore.md`。
- Experience: 已读取 `docs/experience-index.md`；匹配 `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`。
- Root code: 报错常量为 `PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED`；抛出位置为 `MesFrontlineDeviceAccountContextServiceImpl.requireRouteProcessWorkstation`，当 `MesProRouteProcessDO.workstationId == null` 时触发。
- API distinction: 一线生产使用 `/mes/pro/feedback/frontline/device-account/processes`，该接口必须执行正式工作站绑定门禁；`/mes/pro/process-pool/team-leader/process-config/list` 是生产组长配置数据源，不能替代本路径。
- Git preflight: 根仓库分支为 `int_main`，remote 为 `origin`。任务开始时既有并发改动先后由提交 `6ebb603c4` 和任务创建前基线提交 `9c7507e1d` 保存；`9c7507e1d` 包含并发任务文件 `doc/tasks/20260807-active-order-without-schedule-order/{backend-api-evidence.md,execution-log.md,task.md}` 与 `doc/tasks/20260807-production-leader-process-loss-reasons-random/execution-log.md`，本任务未修改其内容。

## Milestone Updates

- M1 complete: 已确认前端生产模式通过 `loadFrontlineDeviceProcesses` 调用正式设备账号工序接口，错误由后端路线工序 `workstationId` 空值门禁抛出。
- M2 complete: 路线 `922119` 的 14 条当前 `mes_pro_route_process` 记录均在 V24 发布时间 `2026-08-06 23:42:17` 创建且 `workstation_id` 全为空；其紧邻上一组 14 条逻辑删除记录按相同 process/sort 唯一对应工作站 `980010, 980008, 980009, 980011..980021`。这些工作站均存在、启用且属于对应工序；任务 `981940` 也将工序 `922985` 固定到 `980010`。
- M2 root cause: `MesProRouteVersionPublishProjectionServiceImpl.projectProcesses` 重建正式路线工序时未投影冻结快照中的 `workstationId`。V24 发布正好删除旧行并创建 14 条空工作站新行，时间和数据形态与缺陷一致。
- M3 RED complete: 新增 `projectCandidate_shouldPreserveFrozenRouteProcessWorkstationBinding`，证明候选快照中的 `workstationId=980010` 投影后为 `null`。
- M3 implementation complete: 在正式发布投影构造 `MesProRouteProcessDO` 时加入 `workstationId`，未改变一线生产缺失绑定时的 fail-fast 门禁。
- M3 follow-up RED: V25 发布前置校验发现候选流程图把展示用 `workstationId` 解析成了正式 `routeProcessWorkstationId`，脚本在写入前阻断，未发布、未修改当前 V24。新增正式字段与展示字段取不同值的回归断言，并覆盖候选保存必须写入正式字段。
- M3 formal-field correction complete: 已统一候选流程图读取、候选保存快照、流程配置解析和版本发布投影，仅使用 `routeProcessWorkstationId` 作为正式绑定来源；`workstationId` 保持展示字段，不参与正式绑定投影，也未加入任何旧字段兼容或默认值分支。
- M3 targeted GREEN complete: 使用任务隔离编译产物运行 JUnit Platform，`MesProRouteVersionPublishProjectionServiceTest` 与 `MesProRouteProcessFlowServiceImplTest` 共 4 个目标用例全部通过，覆盖候选读取、候选保存和发布投影。
- M4 runtime complete: 已将 3 个正式字段修复类写入本地运行包，运行包 SHA-256 为 `650703E8CEDAFCF6DDBF7122E4FC7F9BFFD384B5DE20024F2102B017845C56D0`；嵌套 MES Jar 保持 STORED，3 个运行态 class SHA-256 与隔离编译产物逐一一致。48081 已由新 PID `67752` 接管且 health=`UP`。
- M4 repair retry blocked before publish: 临时发布脚本首次保存时因 PowerShell `OrderedDictionary` 使用整数键取值返回空，再被 `[long]` 转换为 `0`，导致任务自建候选 V25 的 14 个正式绑定被写成 `0`；随后候选读取门禁立即阻断，V25 未提交、未发布，V24 仍为唯一 ACTIVE。只读数据库核对确认候选 V25 恰好 14 个值均为 `0`，无其它正式值；将通过正式候选取消/重建 API 清理该任务自有候选，不直接改数据库。

## Verification Evidence

- RED: `mvn --% -pl yudao-module-mes -am -Dtest=MesProRouteVersionPublishProjectionServiceTest#projectCandidate_shouldPreserveFrozenRouteProcessWorkstationBinding -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `expected: <980010> but was: <null>`，证明版本发布投影丢失正式工作站绑定。
- RED: `publish-workstation-repair.ps1`（V25 发布前置校验）-> FAIL, `Candidate already contains an unexpected formal workstation binding`；候选快照的正式字段为空但展示字段有值，证明读取路径错误地把展示字段提升为正式绑定。脚本在保存和发布前终止，V24 仍为唯一生效版本。
- RED environment note: 组合回归命令曾在 4 个并发 Maven 编译期间因共享 `target/classes` 被其它构建清理而失败，报错为 DCC/MES 依赖类缺失；该结果不是行为断言证据，待共享编译结束后重新执行同一标准命令。
- GREEN: pending。
- GREEN: `java @.../junit-selected.args` -> PASS, 4 tests found / 4 tests successful / 0 failed。
- REGRESSION: pending。

## Blockers

- 初次 RED 命令曾被并发任务中的 `MesTeamLeaderActiveOrderServiceImpl` 未完成编译阻塞；该并发文件补齐后，同一标准命令已获得预期断言失败，不再构成本任务阻塞。
