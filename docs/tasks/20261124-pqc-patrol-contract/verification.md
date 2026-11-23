# PQC PATROL 前向约束修复

范围：只修复应用仓迁移合同及测试；服务器由维护发布。保留 FIRST/PATROL_AM/PATROL_PM/FINAL 历史身份，新增 PATROL 合法值，不回填、删除或重编号冻结任务，不改历史迁移或账本。

BDD：
1. Given 启用旧四值CHECK及跨租户历史任务；When 插入PATROL；Then 原约束拒绝3819。When 执行前向迁移；Then PATROL可写、非法值仍拒绝、原任务逐字段保持。
2. Given 缺CHECK但字段正确且所有历史值合法；When 执行迁移；Then 建立启用的五值CHECK。
3. Given 已是五值CHECK；When 重复执行；Then 定义和任务均不变。
4. Given 非规范约束、未启用约束、非法/NULL历史数据或字段不符；When 预检或迁移；Then 阻断，不猜测或修复业务数据。

验证：先运行失败测试（RED），新增迁移及只读目标检查后，在本机独占的无端口MySQL8.0.39容器运行原测试（GREEN）；再运行巡检、冻结身份、完成覆盖回归与正式发布目录及三环境输入合同。页面E2E由维护测试部署后执行，此处提供现有入口和前置基线，不将DDL测试冒充业务E2E。

## 实际结果（2026-11-24）

- 独立MySQL 8.0.39容器8项测试全部通过（24.828秒），未连接应用数据库。旧约束插入PATROL实际报3819；迁移后可插入，非法值仍报3819，原跨租户/已删除/已提交记录逐字段不变，重复执行不变。utf8mb4和latin1约束表达式均已实测。非规范、未启用CHECK及非法历史/不符字段均阻断且不修改表或数据。
- 发布源注册单测通过。历史迁移未修改；只新增前向SQL、只读目标预检和目录登记；H2最终结构夹具同步包含PATROL。
- 首次扩展Java回归暴露既有测试基线失配，已在本次范围修正并全部复测通过；没有改Java生产实现、跳过测试或放宽业务校验。具体修复如下。

| 测试类 | 初始失败原因及修复 | 最终结果 |
|---|---|---|
| MesFrontlinePqcPatrolRoundTest | 补齐新增handoffService单元依赖；将“全部订单不读任务”过期断言收紧为“缺来源订单不读任务、健康订单正常验证抽样证据” | 18 PASS |
| MesTeamLeaderActiveOrderServiceTest | 真实交接服务缺pqcTasks；改用实际入单生成任务的仓储夹具，补齐本租户PQC人员/组长/权限，验证生产及每个PQC个人任务均生成；保留无效正式身份拒绝分支 | 113 PASS |
| MesOrderQaStageReleaseCoverageTest | 旧上午/下午定义及缺冻结来源/抽样证据；改为7工序×首检及2轮PATROL=21任务，提供实际抽样计算证据，删除任意整工序仍阻断两项阶段覆盖 | 2 PASS |
| MesFrontlinePqcTaskOverlayTest | 旧阶段排序夹具；改为当前PATROL两轮身份、跨工序/日期/轮次/重复隔离；新增旧AM/PM冻结任务不得冒充新PATROL的断言 | 8 PASS |
| MesQaInspectionStageRulesTest | 反射签名已改为请求对象；传入完整请求及明确末检不适用，原非法/重复/旧上午下午规则拒绝断言保留 | 5 PASS |
| MesActiveOrderHandoffLifecycleTest | 未修改，补充验证个人任务完整生命周期 | 69 PASS |

最终命令：`mvn -pl yudao-module-mes -am '-Dtest=MesFrontlinePqcPatrolRoundTest,MesTeamLeaderActiveOrderServiceTest,MesOrderQaStageReleaseCoverageTest,MesFrontlinePqcTaskOverlayTest,MesQaInspectionStageRulesTest,MesActiveOrderHandoffLifecycleTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`。215项、0失败、0错误、0跳过，BUILD SUCCESS（44.710秒）。真实MySQL迁移测试另计8项。

初始基线可由父提交275c870118f8096328d94bd38700455de3e76fc0中的上述测试和Java实现核对；首轮失败时仅SQL最终结构夹具有改动，未改这些测试或Java生产实现。旧AM/PM任务在数据库保持原身份，不表示当前规则允许将其改解释为PATROL或可直接继续执行；旧规程新入单仍须明确修订发布，相关拒绝测试保留。

## 页面验收入口与前置条件

生产组长进入“活跃订单池→新增活跃订单”，选择本租户真实且状态为NEW的候选生产订单。必须有真实ERP来源、正整数固定订单数量、正式发布路线和工序来源、该组长开工授权、已启用DCC项目及其最新发布产品QA规程；FIRST/PATROL适用工序须有完整检验项目，巡检须配置有效inspectionLevel/AQL。若存在通用包装绑定，也须有效并发布。不得用复用已有订单、模拟或重置替代本次新任务INSERT验收。

真实页面成功入单后，核验组长活跃订单和一线PQC首检/第一轮巡检任务可见，冻结来源版本和抽样证据完整；重复加入不得多建任务。权限不绕过，业务操作全部由维护使用Playwright执行。本轮应用仓任务未部署服务器、未执行应用数据库写入、未完成页面E2E。

链路依据：TeamLeaderWorkbenchPage.vue的submitAddActiveOrder → MesProcessPoolTeamLeaderController的/active-order/add（maintain权限，身份取登录组长）→ MesTeamLeaderActiveOrderServiceImpl.addActiveOrder → requireProductionRouteSourceForAdd/requireLatestQaSource/preparePqcTaskPlan → insertPqcInspectionTasks/buildPqcTask → Mapper.insert。FIRST及第一轮PATROL在入单时冻结生成，FINAL由后续链路创建；本迁移仅解除数据库对合法PATROL的错误拒绝。
