# 生产组长工作台测试计划

## Purpose and Scope

定义生产组长工作台改造的 BDD 场景、严格 TDD 顺序、真实 E2E 路径、测试数据和阻塞条件。测试计划覆盖班组配置、员工端配置驱动、活跃订单、报工确认分配、订单工序完成和正式批记录回填。

## Evidence Reviewed

- `prd.md`
- `development-plan.md`
- `docs/e2e-rules.md`
- `IntRuoyiFronted/package.json`
- `IntRuoyiBackend/pom.xml`
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`

## BDD Scenarios

### Feature Scenarios

- BDD: 活跃订单池约束 -> Given 生产组长已加入订单 O1 为活跃订单 When 组长异常上报或确认报工 Then 订单选择器只显示 O1 等活跃订单。
- BDD: 班组员工维护 -> Given 生产组长新增临时工 T1 When 绑定 T1 到工序 A Then 员工端可在工序 A 选择 T1 且不要求用户系统账号。
- BDD: 班组设备维护 -> Given 生产组长新增设备 D1 并绑定工序 A When 员工填报工序 A Then 员工端可选择 D1。
- BDD: 设备参数默认值 -> Given D1 参数压力范围 10-20 默认 15 When 员工选择 D1 Then 压力默认显示 15 且提交值必须在 10-20。
- BDD: 工序异常关系 -> Given 工序 A 允许异常 E1 When 员工填报工序 A 不良原因 Then 只能选择 E1 等允许原因。
- BDD: 结构化报工详情 -> Given 员工提交设备参数和不良原因 When 组长查看提交详情 Then 页面展示结构化设备、参数、数量和原因，不只展示原始 JSON。
- BDD: FIFO 自动分配 -> Given 员工提交完成数量 80 且活跃订单 O1 当前工序剩余 50、O2 当前工序剩余 30 When 组长点击 FIFO 自动分配 Then 系统预分配 O1=50、O2=30。
- BDD: 手动分配调整 -> Given FIFO 预分配 O1=50、O2=30 When 组长手动调整为 O1=40、O2=40 Then 系统重新校验并保存两条分配，总数等于 80。
- BDD: 订单工序完成 -> Given 订单 O1 工序 A 目标数量 200 且累计 120 When 新确认 80 Then O1 工序 A 变为完成。
- BDD: 批记录回填 -> Given 工序 A 绑定正式批记录 F1 且字段映射完整 When O1 工序 A 完成 Then 设备参数写入 F1 对应字段。

### Failure Scenarios

- BDD: 非活跃订单拒绝 -> Given 订单 O9 未加入活跃订单 When 组长尝试分配报工到 O9 Then 系统拒绝并提示订单非活跃。
- BDD: 分配总数不等拒绝 -> Given 员工提交完成数量 80 When 组长只分配 70 Then 系统拒绝确认。
- BDD: FIFO 剩余不足阻塞 -> Given 活跃订单当前工序总剩余 60 When 员工提交完成数量 80 并点击 FIFO 自动分配 Then 系统提示活跃订单剩余不足并阻塞确认。
- BDD: 设备参数越界拒绝 -> Given 压力范围 10-20 When 员工提交压力 25 Then 系统拒绝提交并显示越界。
- BDD: 禁用设备不可选 -> Given 设备 D1 已禁用 When 员工进入工序 A Then D1 不出现在可选设备中。
- BDD: 报修设备不可选且恢复后可选 -> Given 设备 D1 已报修 When 员工进入工序 A Then D1 不出现在可选设备中；When 组长恢复 D1 Then 员工再次进入工序 A 可选择 D1。
- BDD: 正式批记录绑定缺失阻塞 -> Given 工序 A 没有正式批记录绑定 When 累计数量达到完成条件 Then 系统阻塞回填并显示缺少正式批记录绑定。
- BDD: 字段映射缺失阻塞 -> Given F1 缺少压力字段映射 When 工序完成 Then 系统阻塞对应字段回填并提示缺少映射。

### Boundary Scenarios

- BDD: 刚好达到目标数量 -> Given 目标 200 已累计 199 When 本次分配 1 Then 工序完成。
- BDD: 未达到目标数量 -> Given 目标 200 已累计 120 When 本次分配 79 Then 工序保持进行中。
- BDD: FIFO 稳定排序 -> Given 两个活跃订单剩余数量相同 When 多次点击 FIFO 自动分配 Then 系统按活跃订单加入时间和订单稳定键生成一致结果。
- BDD: 多员工并发确认同一订单工序 -> Given O1 工序 A 剩余 10 When 两个确认同时尝试各分配 10 Then 最多一个成功，另一个因剩余数量不足失败。
- BDD: 无设备工序填报 -> Given 工序 B 不需要设备 When 员工打开无设备模板 Then 页面不显示设备参数，但仍按配置显示员工和异常原因。

## TDD Sequence

### Backend Unit / Service Tests

- Test Entry Gate: 每个 RED 命令执行前必须先新增或确认目标测试类和测试方法存在；缺测试类、缺测试方法、No tests 或空跑不能作为有效 RED。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少活跃订单服务。
- GREEN: 同命令 -> PASS, 活跃订单加入、移出、查询、非活跃拒绝通过。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少配置运行态服务。
- GREEN: 同命令 -> PASS, 工序-员工、工序-设备、工序-异常、设备报修/恢复、设备参数默认值通过。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderReportConfirmationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少报工分配确认服务。
- GREEN: 同命令 -> PASS, 分配总数、活跃订单、剩余数量、审计记录通过。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderFifoAllocationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少 FIFO 自动分配服务。
- GREEN: 同命令 -> PASS, 按活跃订单加入时间、剩余数量、多订单拆分、剩余不足阻塞、稳定排序通过。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少订单工序完成判断。
- GREEN: 同命令 -> PASS, 未达成、刚好达成、并发幂等通过。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前缺少正式批记录回填服务。
- GREEN: 同命令 -> PASS, 成功回填、缺绑定阻塞、缺映射阻塞通过。

### Controller / API Contract Tests

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前 Controller 缺少活跃订单、配置中心、报工分配接口。
- GREEN: 同命令 -> PASS, 路由、权限、请求字段、禁止客户端传 leaderUserId 通过。

### Frontend Static Contract Tests

- Test Entry Gate: 先新增目标静态合同 spec 和 `package.json` 脚本；缺脚本不能作为有效 RED。
- Worktree Gate: 所有前端命令必须从当前 worktree 根目录执行，并使用 `pnpm --dir IntRuoyiFronted ...`，不得指向 `E:\IntRuoyi`。
- RED: `pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static` -> FAIL, 当前页面缺少配置中心和报工分配表。
- GREEN: 同命令 -> PASS, 页面包含活跃订单、FIFO 自动分配、手动分配调整、结构化提交详情和异常上报选择器。
- RED: `pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static` -> FAIL, 当前员工端仍可能使用固定设备、参数、不良原因。
- GREEN: 同命令 -> PASS, 员工端从运行态配置接口读取选项。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS, 前端类型检查通过。

## E2E Plan

### User Path 1 - 组长配置驱动员工填报

- 登录生产组长账号。
- 进入工序池班组长工作台。
- 在生产组长页签加入活跃订单 O1。
- 新增临时工 T1，不关联用户系统。
- 新增设备 D1，设置参数压力范围 10-20 默认 15。
- 绑定工序 A 与 T1、D1、异常 E1。
- 登录或切换到员工填报路径。
- 打开工序 A 填报页，断言只能看到 T1、D1、E1 和压力默认值 15。
- 员工提交完成数量和设备参数。

### User Path 2 - 组长确认报工并分配订单

- 登录生产组长账号。
- 打开报工确认工作台。
- 选择员工提交记录。
- 查看结构化设备参数、不良原因、数量。
- 点击 FIFO 自动分配，断言系统按 O1 剩余优先生成分配结果。
- 手动调整部分分配数量，断言系统重新校验总数和剩余数量。
- 提交确认。
- API 只读核验分配记录、订单工序累计数量和提交状态。

### User Path 3 - 工序完成并回填正式批记录

- 准备 O1 工序 A 目标数量 200，正式批记录 F1 已绑定且字段映射完整。
- 通过员工提交和组长确认使累计分配达到 200。
- 页面断言 O1 工序 A 状态为完成。
- API 只读核验 F1 对应字段已写入设备参数和数量。
- 断言无 `formBindings` 替代来源痕迹。

### User Path 4 - 缺正式批记录绑定阻塞

- 准备 O2 工序 A 没有正式批记录绑定。
- 员工提交并由组长确认使累计数量达到目标。
- 页面或接口返回明确阻塞：缺少正式批记录绑定。
- 断言没有使用表单槽位、默认 `MAIN` 或空表单回填。

## Browser or Client Steps

- 使用 Playwright 操作真实前端页面。
- 前端 URL 必须与后端 URL 成对确认。
- 登录使用已授权测试租户和测试账号。
- 写入型数据必须带任务标识 `TLW-20260731-`。
- finally 清理任务自有测试数据，或记录无法清理的 blocker。

## API Verification

- API 只能用于最终状态核验或只读辅助检查。
- 核验项包括活跃订单列表、FIFO 预分配结果、手动调整后的分配记录、配置关系、员工提交详情、订单工序累计数量、批记录字段值。
- 不得用 API 直接造提交、直接确认、直接改工序状态或直接写批记录来替代页面路径。

## Console and Log Checks

- 浏览器控制台不得出现未处理异常。
- 网络请求不得出现业务接口 500、401、403、404。
- 后端日志不得吞掉批记录回填失败。
- 回填失败必须在接口响应和页面上可见。

## Required Test Data

- 测试租户：非生产租户，允许写入和清理任务数据。
- 生产组长账号：拥有工序池班组长工作台、维护、异常上报、报工确认权限。
- 员工账号或临时工档案：可用于员工填报路径。
- 生产订单：至少 O1、O2 为可加入活跃订单；O1 目标数量 200。
- 工序：至少 A 为需要设备参数工序，B 为无设备工序。
- 设备：D1，参数压力 10-20 默认 15。
- 异常原因：E1。
- 正式批记录表单：F1，与工序 A 正式绑定且字段映射完整。
- 阻塞样本：O2 或工序 A 的缺正式批记录绑定场景。

## Reset Procedure

- 清理任务标识 `TLW-20260731-` 创建的活跃订单记录、员工档案、设备、参数、关系、提交、分配和异常上报。
- 对批记录回填样本，优先使用任务自有订单和任务自有批记录实例；不得清理或覆盖生产业务数据。
- 若涉及共享配置，先记录原值，finally 恢复并复验。

## Data Ownership

- 所有写入型测试数据必须属于当前任务。
- 禁止修改生产租户、admin 基线租户或无关真实业务记录。
- 禁止使用历史固定订单、历史固定 executionId 或历史截图作为当前通过证据。

## Test Blockers

- 缺少真实前端入口、菜单权限、路由或页面按钮。
- 缺少测试租户、测试账号、签名或权限。
- 缺少正式批记录表单绑定和字段映射。
- 缺少可用生产订单或工序配置。
- 缺少 Playwright 可用浏览器。
- 前端或后端运行态未启动，或前后端 URL 不成对。
- 目标测试类、package script、静态合同 spec 或 E2E spec 文件缺失时，不得把空跑当作 RED/GREEN，也不得声明真实 E2E PASS。

## Evidence Log Template

- BDD: `<scenario>` -> Given `<precondition>` When `<action>` Then `<observable outcome>`
- RED: `<command>` -> FAIL, `<expected missing behavior>`
- GREEN: `<command>` -> PASS
- E2E: `<command>` -> PASS/BLOCKED, frontend=`<url>`, backend=`<url>`, tenant=`<label>`, user=`<label>`, dataPrefix=`TLW-20260731-`
- Cleanup: `<method>` -> PASS/BLOCKED, remaining task-owned records=`<count>`
