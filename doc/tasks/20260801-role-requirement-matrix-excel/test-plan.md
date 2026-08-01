# 岗位需求分解矩阵全链路测试计划

## Test Strategy

本测试计划用于后续实现任务，不代表当前仓库已经具备计划中的测试类、前端脚本、账号、数据或运行态。每个里程碑必须遵循：

1. 先新增或扩展一个**可编译、可执行**且针对目标行为的测试。
2. 执行 RED，并确认失败原因是目标行为尚未满足。
3. 实现最小正式方案。
4. 执行 GREEN。
5. 执行相邻模块回归、真实用户路径 E2E 和任务数据清理。

缺测试类、缺 package script、No tests、缺数据库、缺账号、缺浏览器或服务未启动只能记录为 blocker，不算 RED。

## BDD Scenarios

| 场景 | Given / When / Then | 覆盖验收 |
|---|---|---|
| BDD-01 ERP 候选进入统一活跃订单 | Given ERP 候选订单和唯一正式路线；When 组长加入活跃订单；Then 生产、PQC、批记录和放行读取同一 activeOrderId | `AC-M01`、`AC-M03`、`AC-M04` |
| BDD-02 多调拨与开工检查 | Given 分批发货、补料、退料和多批次；When 关联并检查；Then 逐物料计算覆盖、展示缺项且不自动创建异常 | `AC-M02`、`AC-M05`、`AC-M06`、`AC-M07`、`AC-M08` |
| BDD-03 班组配置只绑定正式设备 | Given 正式设备台账和员工/原因配置；When 维护班组配置；Then 新业务只使用启用配置且历史快照不变 | `AC-D01` 至 `AC-D08` |
| BDD-04 工序事实优先报工 | Given 尚未确定订单归属；When 员工提交工序、人员、设备、参数、数量、损耗和签名；Then 原始事实成功保存且不可覆盖 | `AC-M10`、`AC-M11` |
| BDD-05 退回修订与系数分配 | Given 原始报工和多个活跃订单；When 退回补正并分配；Then 修订链完整、分配总量一致、目标量使用生产系数 | `AC-M16`、`AC-M17`、`AC-M18`、`AC-D11` |
| BDD-06 确定性批记录回填 | Given 多员工、多设备、多次报工共同完成工序；When 达到目标量；Then 全部事实按正式映射聚合并幂等回填 | `AC-M19` |
| BDD-07 QA 规程版本生命周期 | Given 产品、路线版本和工序；When QA 配置、校验并发布；Then 缺项阻塞、发布不可变、历史保留版本 | `AC-M09`、`AC-D15` 至 `AC-D23` |
| BDD-08 首检与巡检任务计算 | Given 活跃订单和发布规程；When 按日期/班次生成任务；Then 首检固定、巡检向上取整、末检按适用性处理 | `AC-M12` 至 `AC-M15` |
| BDD-09 PQC 逐件检验与签名 | Given PQC 任务和实际人员；When 逐件填写并签名；Then 保存任务上下文、逐件明细、原因和签名且不依赖生产事件 | `AC-D24` 至 `AC-D29` |
| BDD-10 PQC 复核职责隔离与补正 | Given 待确认 PQC 提交；When 组长确认、退回或自我确认；Then 合法确认、退回修订和自我确认阻塞正确 | `AC-M20`、`AC-D30`、`AC-D32` 至 `AC-D35` |
| BDD-11 过程检验和质量异常 | Given 已确认合格/不合格提交；When 汇集过程检验；Then 仅最终修订汇集，不合格形成独立质量异常 | `AC-M21`、`AC-D36`、`AC-D37` |
| BDD-12 批记录完整性和放行 | Given 调拨、生产、PQC、批记录、异常和签名来源；When 预检并审核；Then 缺项阻塞，全部通过后才可签名放行 | `AC-M22`、`AC-M23` |
| BDD-13 日结、范围、只读和快照 | Given 多角色、未闭环事项和配置变更；When 查看范围/日结/历史；Then 后端隔离权限、逐项提示并保持历史快照 | `AC-D09`、`AC-D10`、`AC-D12`、`AC-D13`、`AC-D38`、`AC-D39` |
| BDD-14 缺少正式前置条件 | Given 任一正式订单、路线、系数、规程、人员、签名、调拨或批记录绑定缺失；When 提交；Then fail fast 且无默认补齐 | `AC-D31` 及全部跨切面失败路径 |
| BDD-15 并发与幂等 | Given 同一业务对象的两个并发请求；When 同时提交；Then 最多一个转换成功且无超额、重复回填/汇集/放行 | M1-M6 并发门禁 |
| BDD-16 三类工艺路线配置互不替代 | Given 工序开始、正式批记录绑定和 `formBindings` 同时存在；When 分别展示和执行；Then 三条链路只读取各自来源 | `AC-D14` 及正式批记录跨切面门禁 |

## RED

### RED Rules

- RED 命令执行前，测试类/脚本必须已创建且能被测试运行器发现。
- RED 预期原因必须写成业务断言，例如“PQC 仍读取旧活跃订单来源”，不能写“测试类不存在”。
- 一个 RED 只证明一个最小行为差距；多个失败原因必须拆分。
- 所有 RED 证据记录到后续实现任务的 `execution-log.md`：
  `RED: <command> -> FAIL, <expected reason>`。

### M1 RED

先增加可编译的权威来源、schema 和迁移合同测试，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderAuthorityServiceTest,MesActiveOrderMigrationContractTest,MesActiveOrderSchemaTest,MesFrontlinePqcActiveOrderAuthorityTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

预期行为型失败：

- PQC 仍通过 `mes_pro_process_pool` 判断活跃订单。
- 同一订单被多个组长加入后得到多个跨角色身份。
- 开放订单缺正式路线版本时迁移未阻塞。
- 活跃订单状态转换缺少唯一键/版本冲突保护。

### M2 RED

优先扩展现有测试，使其断言新行为：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineProductionFactSubmitServiceTest,MesTeamLeaderSubmissionRevisionServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordAggregationServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static
pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static
```

预期行为型失败：

- 原始生产报工仍强制订单、任务或工作站。
- 订单数量 `300`、生产系数 `3.0` 时目标量仍为 `300` 而非 `900`。
- 多次报工完成后只回填代表事件，遗漏其他人员、设备或参数。
- 两个并发确认可造成超额或重复回填。

### M3 RED

先新增前端静态测试文件和对应 `package.json` script，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest,MesQaInspectionRegulationPublishServiceTest,MesPqcTaskGenerationServiceTest,MesFrontlinePqcContextServiceTest,MesPqcSubmissionServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-qa-regulation:static
pnpm --dir IntRuoyiFronted test e2e:role-matrix-pqc-dynamic-form:static
```

预期行为型失败：

- PQC 首检在没有最新生产事件时不能提交。
- 页面仍固定四个项目、`PATROL`、数量 `30` 或损耗 `1`。
- 规程发布后仍可原地修改。
- 上午/下午巡检未按日期、班次、轮次区分。
- PQC 组长可确认自己作为实际检验人的记录。

### M4 RED

先确认调拨、异常、返工、报废和库存的正式 source map，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderStartCheckServiceTest,MesQualityAbnormalServiceTest,MesPqcProcessInspectionAggregationTest,MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-transfer-start-check:static
pnpm --dir IntRuoyiFronted test e2e:edhr:release:check
```

预期行为型失败：

- 一个订单不能表达多调拨、分批、补料、退料或多批次。
- 开工检查只判断“有关联单”，未校验物料/数量/批次。
- 未确认 PQC 被错误算作过程检验完成。
- 放行仍只得到“来源未接入”占位项，无法读取真实生产/PQC/物料来源。

### M5 RED

先新增日结、范围、快照和静态合同测试，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderDailyCloseServiceTest,MesPqcDailyCloseServiceTest,MesRoleMatrixReadModelServiceTest,MesRoleMatrixHistorySnapshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-daily-close-scope:static
```

预期行为型失败：

- 班组长仍可创建独立班组设备而不是绑定设备台账。
- 责任范围缺产线、设备或订单。
- 日结未列出退回未补正、未分配、未确认或批记录缺项。
- 修改配置后历史报工/PQC 显示新值。

## GREEN

### GREEN Rules

- 只实现令当前 RED 通过的最小正式方案。
- 不保留双读、默认值、兼容 shim、mock success 或异常吞噬。
- GREEN 后立即运行相邻回归；任何相邻回归失败都保持 milestone 未完成。
- 记录格式：
  `GREEN: <command> -> PASS`。

### Milestone GREEN Commands

M1、M2、M3、M4、M5 分别重跑对应 RED 命令并要求：

- tests run 大于 `0`。
- failures=`0`，errors=`0`。
- 新增 schema/contract 测试和行为测试同时通过。
- 前端静态合同通过后执行：

```powershell
pnpm --dir IntRuoyiFronted ts:check
```

M2 相邻回归至少包含：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

M3/M4 相邻回归必须继续覆盖原生产组长测试、PQC 上下文、过程检验和 eDHR 放行测试。

## E2E

### E2E Preconditions

执行前必须按项目规则确认：

- 前端、后端端口与当前分支 profile 一致并且服务健康。
- 已读取 `docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md` 和 `docs/database-rules.md`。
- 测试租户、六类角色账号、权限、密码、电子签名、浏览器、数据库、Redis 均可用。
- ERP 已存在任务专用真实候选订单、调拨、发货和批次数据，或可通过正式 ERP/同步页面创建并同步。
- 正式路线、生产系数、SOP、正式逐工序批记录绑定和 QA 规程数据可通过真实页面维护。
- 缺任一前置时 E2E 标记 BLOCKED，不得用 API-only、SQL 直写、mock 或静态合同替代。

### Planned E2E Scripts

实施时新增：

- `IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js`
- `e2e:role-requirement-matrix:real:check`
- `e2e:role-requirement-matrix:real`

语法和真实执行：

```powershell
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real
```

### E2E-01 Positive Main Flow

1. 计划/班组长账号通过正式页面确认任务专用 ERP 候选已同步。
2. 生产班组长登录，搜索订单并加入统一活跃订单。
3. 关联两个调拨/发货批次，验证物料覆盖和开工检查。
4. QA 登录，创建规程草稿，配置首检、上午/下午巡检、末检和项目，完成发布。
5. 生产员工登录，在不选择订单/任务的情况下提交工序事实和签名。
6. 生产班组长查看原始报工，退回一次；员工补正后重新签名。
7. 生产班组长确认并按 FIFO 建议/手工调整分配到活跃订单。
8. 验证 ERP 固定数量未变，系数 `1.0` 和 `3.0` 工序分别得到正确目标量。
9. PQC 检验员登录，在尚无依赖生产事件的情况下执行首检，随后执行上午/下午巡检和适用/不适用末检。
10. PQC 组长登录，确认合法提交并退回一笔补正；验证自我确认被阻塞。
11. 验证过程检验汇集、正式批记录多事件聚合、异常状态和日结。
12. 放行负责人登录，查看全部来源并签名放行。
13. API 只读核验 activeOrderId、目标量、批记录、PQC 任务、过程检验和放行状态。

### E2E-02 Missing Prerequisites

分别通过真实页面构造：

- 缺调拨/物料批次。
- 缺 SOP。
- 缺生产系数。
- 缺正式批记录绑定。
- 缺发布 QA 规程。
- 缺实际 PQC 人员或签名。
- 缺必需 PQC 任务确认。

每个场景必须断言具体缺项、来源和阻塞动作；不得只断言通用 toast。

### E2E-03 Role and Scope Isolation

- 生产员工不能维护班组、规程或放行。
- 生产班组长只能只读查看 PQC，不可提交/确认。
- QA 不可确认 PQC 或分配报工。
- PQC 检验员不可发布规程或放行。
- PQC 组长不可确认自己的实际检验提交。
- 放行负责人不可修改原始报工/PQC。
- 调整员工、工序、产线、设备和订单范围后，后端返回结果同步收窄。

### E2E-04 Historical Snapshot and Daily Close

1. 创建并完成一组报工/PQC。
2. 修改员工名称、原因、设备参数上限和 QA 规程版本。
3. 验证历史报工、PQC、签名和批记录仍显示旧快照。
4. 创建未分配、未确认、退回未补正和批记录缺项。
5. 验证班组日结和 PQC 日结按职责分别显示。

### E2E Evidence

证据至少记录：

- 前端/后端 URL。
- 测试租户和角色账号标签，不记录密码。
- 任务数据前缀。
- 订单、路线版本、路线工序、调拨、物料批次、规程版本、报工、PQC 任务、签名、批记录和放行 ID。
- 关键页面断言和最终只读 API 结果。
- 清理命令/页面路径和清理后计数。

## Test Data

所有数据必须属于一个已确认的非生产测试租户，建议前缀：`RRM-20260801-`。

| 数据 | 最小要求 |
|---|---|
| 角色 | 生产员工、生产组长、QA、PQC 检验员、PQC 组长、放行负责人各至少一名；另准备一名可作为实际 PQC 人员的 PQC 组长 |
| ERP 订单 | 固定产品数量 `300`，正式产品、交期、批次和唯一订单 ID |
| 路线 | 正式发布路线版本，至少两个工序；生产系数分别为 `1.0` 和 `3.0` |
| 三类配置 | 同一工序同时准备工序开始配置、正式批记录表单绑定和一个 `formBindings`，用于验证互不替代 |
| 调拨 | 至少两个调拨/发货批次，包含部分发货、补料、退料和多物料批次 |
| 设备 | 正式设备台账中启用、报修、禁用设备各一台 |
| 参数 | 一个正常值、一个超上限值、一个低于下限值 |
| 报工 | 多员工、多设备、多次报工；包含一次退回补正 |
| QA 规程 | 首检固定数量；上午/下午比例 `5%`；一个工序末检适用，一个工序末检不适用 |
| PQC | 首检、上午巡检、下午巡检、末检、退回补正、不合格和自我确认阻塞样本 |
| 签名 | 每个实际操作人可用的正式电子签名；签名人与实际人员不一致的失败样本 |
| 异常 | 生产异常和质量异常各一条，分别包含阻塞和非阻塞状态 |

禁止把账号密码、token、连接密钥或签名凭据写入任务文档或提交。

## Failure Paths

- ERP 候选未同步或正式订单不存在。
- 同一订单存在两条冲突正式路线或两套活跃来源。
- 重复加入、移出后继续分配、终止订单继续生成 PQC。
- 调拨只有单号无正式 ID，物料/数量/批次不覆盖，退料导致净数量不足。
- 生产报工缺工序、实际人员、台账设备、数量、原因或签名。
- 生产报工被强制要求订单/任务/工作站。
- 设备报修/禁用后仍可选；超限值被自动改写。
- 退回覆盖原始报工或补正没有修订链。
- 生产系数缺失、非正数、来源冲突或被默认为 `1`。
- FIFO/手工分配到非活跃订单、工序不匹配、总量不等、超额、重复事件。
- 多次报工批记录只取一个事件；聚合策略缺失仍继续写入。
- 正式批记录绑定缺失时改用 `formBindings`、`MAIN` 或工序开始配置。
- QA 规程缺项目/首检/上午/下午/末检明确配置仍能发布。
- 发布规程被原地修改，历史任务被新版本覆盖。
- PQC 仍依赖最新生产事件或复制生产设备/工作站。
- 页面仍写死项目、类型、数量、损耗或默认合格。
- 巡检数量未向上取整，上午/下午/跨天任务重复或污染旧任务。
- 实际 PQC 人员与签名人不一致。
- PQC 自我确认、退回记录参与完成、旧修订被重复汇集。
- 质量异常进入生产异常流或阻塞条件无责任人/解除条件。
- 班组长能修改 PQC，或权限只在前端隐藏未在后端校验。
- 日结遗漏未分配、未确认、退回未补正、不合格未处理或批记录缺项。
- 放行检查用“来源未接入”占位成功、缺项仍生成待办或重复放行。
- 配置修改改写历史报工/PQC/批记录。
- 一对多 JOIN 导致列表重复、分页总数错误或放行检查重复计数。

## Concurrency and Performance

- 使用数据库锁、唯一键和版本号验证同一业务对象并发转换。
- 报工分配并发必须证明累计量不超过目标量。
- PQC 提交/确认并发必须证明只有一个有效修订/确认。
- 批记录回填并发必须证明同一聚合版本只写一次。
- 放行并发必须证明同一事务只产生一个终态。
- 对活跃订单、组长待办、PQC 待办、日结和放行列表执行合理规模的分页/索引验证。
- 逐件明细加载执行查询计数或性能断言，阻止 N+1。

## Cleanup

- 真实 E2E 使用任务前缀和明确业务 ID。
- 优先通过真实页面撤销/关闭任务数据；API 只用于最终核验和项目规则允许的任务自有清理。
- 不删除非本任务业务记录、系统基线、生产租户数据或 append-only 审计/签名证据。
- 清理完成后记录各任务表/业务对象的剩余计数和无法删除但按治理规则保留的证据。
