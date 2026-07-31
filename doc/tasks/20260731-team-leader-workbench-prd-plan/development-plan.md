# 生产组长工作台开发计划

## Purpose and Scope

按 BDD + 严格 TDD 实施生产组长工作台改造。开发策略为保留现有页面入口、路由、权限和接口模块，重构生产组长页签内容，补齐后端模型、接口、员工端配置读取、报工确认分配、订单工序完成和正式批记录回填链路。

## Evidence Reviewed

- `prd.md`
- `test-plan.md`
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`
- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../processpool/team/`
- `docs/e2e-rules.md`
- `docs/powershell-memory.md`

## Architecture Direction

- Frontend: Vue 3 + TypeScript + Element Plus；生产组长页签保留当前组件名和路由，内部拆分为工作台子组件。
- Backend: Java 17 + Maven + Spring Boot；MES 模块新增或扩展 process pool team leader 领域服务。
- Data: 按正式业务实体建模，不使用前端缓存或 payload JSON 作为唯一事实来源。
- Batch Record: 正式批记录表单绑定来自工序设置；缺绑定或映射失败必须 fail fast。

## Milestone P0 - 文档与契约锁定

### Goal

锁定 PRD、开发计划、测试计划、任务门禁和禁止 fallback 约束。

### Work Items

- 确认 `prd.md`、`development-plan.md`、`test-plan.md` 已覆盖用户需求。
- 明确第一版支持 FIFO 自动分配，同时保留手动分配和手动调整。
- 明确 `formBindings` 不可替代正式批记录表单。

### Verification

- UTF-8 读取文档通过。
- 结构检查通过。

## Milestone P1 - 班组配置与活跃订单后端模型

### BDD

BDD: 生产组长维护活跃订单和班组配置 -> Given 当前用户为生产组长 When 维护活跃订单、员工、设备、设备参数和工序关系 Then 员工端和组长端读取到同一套正式配置。

### RED

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少活跃订单服务。
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少工序-员工、工序-设备、工序-异常、设备参数默认值配置服务。

### Implementation

- 新增活跃订单模型和服务：加入、移出、分页、按组长查询。
- 新增或扩展班组员工档案模型，支持临时工不关联用户系统。
- 新增设备档案与设备状态，支持启用、报修、禁用、恢复。
- 新增设备参数默认值、上下限、单位。
- 新增工序-员工、工序-设备、工序-异常关系。
- 后端校验当前组长只能维护自己班组范围。

### GREEN

- 活跃订单服务单测 PASS。
- 班组配置服务单测 PASS。
- Controller 映射和权限静态测试 PASS。

## Milestone P2 - 员工填报配置驱动

### BDD

BDD: 员工填报页选项来自组长配置 -> Given 组长配置工序 A 可用设备和异常原因 When 员工打开工序 A 填报页 Then 页面只展示当前配置允许项。

### RED

- `pnpm --dir E:\IntRuoyi\IntRuoyiFronted test e2e:frontline-team-config:static` -> FAIL，当前员工端仍存在固定设备、参数或不良原因展示风险。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少员工端运行态配置接口。

### Implementation

- 后端提供员工填报运行态配置接口，按工序返回允许员工、设备、设备参数、不良原因。
- `FrontlineFixedTemplatePanel.vue` 改为从运行态配置读取设备、参数、不良原因。
- 无设备模板仍读取工序员工和异常原因配置，不显示设备参数区域。
- 缺配置时明确提示，不使用硬编码默认列表。

### GREEN

- 员工端运行态配置接口测试 PASS。
- 员工端静态合同 PASS。
- 前端 `pnpm --dir E:\IntRuoyi\IntRuoyiFronted ts:check` PASS。

## Milestone P3 - 报工确认与活跃订单分配

### BDD

BDD: 组长确认报工并自动 FIFO 分配到活跃订单 -> Given 员工提交完成数量 80 且 O1 剩余 50、O2 剩余 30 When 组长点击 FIFO 自动分配 Then 系统生成 O1=50、O2=30 的预分配结果且总数等于 80。

BDD: 组长手动调整 FIFO 分配结果 -> Given FIFO 预分配结果为 O1=50、O2=30 When 组长手动调整为 O1=40、O2=40 Then 系统重新校验活跃订单、剩余数量和总数后保存。

### RED

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderReportConfirmationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前复核接口不能表达订单分配。
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderFifoAllocationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前缺少活跃订单 FIFO 自动分配算法。
- `pnpm --dir E:\IntRuoyi\IntRuoyiFronted test e2e:team-leader-report-allocation:static` -> FAIL，当前页面没有活跃订单分配表。

### Implementation

- 新增报工确认分配模型，记录源提交、员工、工序、订单、数量、确认人、确认时间。
- 新增报工确认接口，替代单纯通过 / 退回的复核语义。
- 新增 FIFO 自动分配服务，按活跃订单加入时间升序和当前工序剩余数量生成预分配结果。
- 前端提供“FIFO 自动分配”动作，自动结果先展示为可确认的预分配行。
- 前端允许生产组长手动新增、删除或调整分配行。
- 校验分配订单必须为活跃订单。
- 校验分配总数等于确认数量。
- 校验不能超过订单工序剩余数量。
- 前端提交详情改为结构化展示，确认弹窗增加活跃订单分配表和 FIFO 自动分配按钮。

### GREEN

- 报工确认服务单测 PASS。
- FIFO 自动分配服务单测 PASS。
- Controller 测试 PASS。
- 前端静态合同 PASS。

## Milestone P4 - 订单工序完成与批记录回填

### BDD

BDD: 累计分配完成订单工序并回填正式批记录 -> Given 订单 O1 工序 A 目标数量 200 且已分配 120 When 本次确认再分配 80 Then 工序 A 完成并回填正式批记录表单。

### RED

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少订单工序累计完成判断。
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少正式批记录回填服务。

### Implementation

- 新增订单工序累计分配读取逻辑。
- 在报工确认事务内判断订单工序是否达到目标数量。
- 达到目标数量后推进订单工序状态为完成。
- 读取工序设置中的正式批记录表单绑定。
- 按字段映射将设备参数、数量、不良原因写入批记录。
- 缺绑定、缺映射或目标不可写时阻塞并返回明确错误。
- 确保重复确认、并发确认不会重复回填。

### GREEN

- 订单工序完成服务测试 PASS。
- 批记录回填服务测试 PASS。
- 缺正式批记录绑定的失败场景测试 PASS。

## Milestone P5 - 生产组长前端工作台重构

### BDD

BDD: 生产组长在一个工作台完成配置、异常上报和报工确认 -> Given 组长进入生产组长页签 When 维护配置、选择活跃订单、确认报工、上报异常 Then 页面按业务链路展示而不是手输 ID 表单。

### RED

- `pnpm --dir E:\IntRuoyi\IntRuoyiFronted test e2e:team-leader-workbench:static` -> FAIL，当前页面缺少配置中心和报工分配业务结构。

### Implementation

- 将生产组长页签改为两大业务区：报工确认工作台、班组配置中心。
- 报工确认工作台：列表 + 详情 + FIFO 自动分配 + 手动分配调整 + 确认 / 退回 / 标记异常。
- 班组配置中心：活跃订单、员工、设备、设备参数、工序-员工、工序-设备、工序-异常。
- 异常上报：订单选择器来自活跃订单，异常原因来自工序配置。
- 删除或隐藏纯 ID 手输入口，改为选择器和结构化表格。

### GREEN

- 前端静态合同 PASS。
- `pnpm --dir E:\IntRuoyi\IntRuoyiFronted ts:check` PASS。

## Milestone P6 - 真实 E2E 与回归

### BDD

BDD: 生产组长配置驱动员工填报并完成订单工序 -> Given 测试租户有组长、员工、订单、工序、设备和正式批记录绑定 When 组长配置、员工填报、组长确认分配 Then 订单工序完成且批记录回填。

### RED

- `pnpm --dir E:\IntRuoyi\IntRuoyiFronted test:e2e -- tests/e2e/team-leader-workbench-real-flow.e2e.ts` -> FAIL，真实路径尚未实现或测试数据缺失。

### Implementation

- 新增真实 E2E 脚本，按页面路径操作。
- 准备任务自有测试数据，避免修改生产租户或 admin 基线数据。
- E2E finally 清理任务自有数据或只读确认终态。

### GREEN

- 真实 E2E PASS。
- 后端目标 JUnit PASS。
- 前端静态合同和类型检查 PASS。
- 验证报告记录前后端 URL、租户/账号标签、测试数据标识、批记录回填证据和清理结果。

## Refactor Checks

- 不引入 fallback、默认成功、mock 成功或吞异常。
- 不以 `formBindings` 替代正式批记录表单。
- 不在员工端保留固定设备、固定参数、固定不良原因作为生产数据来源。
- 不把 FIFO 自动分配失败降级为默认成功；活跃订单剩余数量不足时必须阻塞确认。
- 不让 API-only 验证冒充真实 E2E。
- 不把组长 ID 从客户端传入作为权限事实来源；应以后端当前登录用户和权限为准。

## Delivery Gate

- 每个 milestone 必须先 RED，再实现，再 GREEN。
- 每个 milestone 完成后更新 `execution-log.md` 和验证报告。
- 实施阶段若发现缺少测试租户、账号、正式批记录绑定、字段映射、运行态或脚本入口，必须记录 BLOCKED，不得扩大 scope 或静默降级。
