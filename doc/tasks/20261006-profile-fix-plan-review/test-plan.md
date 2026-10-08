# F01–F04 验证计划（仅计划，未执行）

## 2026-10-08 用户批准的 F02 验收变更（当前生效，优先于下方历史 MySQL 门禁）

用户明确同意 F02 只做静态代码评审，不做真实数据验证；保留编译、类型检查和 lint。此变更仅调整 F02 的验收方式，不缩减原 F02-AC1～AC6 功能范围，不更改来源、权限、租户、排序、隐藏、事务、错误和共享 badge epoch 合同；F01 已融合，不重新实施，F03/F04 原定向行为及临时内存 H2 验证保持。

- 保留：主 Agent 与隔离 reviewer 沿页面/角标→wrapper→Controller→标准权限→来源适配器→SQL/Mapper→聚合/事务→返回和导航逐节点评审，逐项记录文件/行号/方法锚点；原六 AC 均需对应静态推导和未验证边界。编译（包含目标模块依赖闭包）、前端类型检查、目标文件 lint、差异/范围检查为放行门槛，既有基线错误须实际对照并明确区分。
- 移出本轮门槛：F02 原 S1 来源真实集合运行对照、MySQL 双连接 RR/快照、H2/HTTP/数据库读写测试、真实容量首中末页与 EXPLAIN/耗时、前端真实数据/E2E 和 F02 行为测试执行。无测试库、无 MySQL 配置不再阻塞 F02 实施及本轮静态放行，不建立任何替代数据验证环境。
- S1 改为来源 SQL / count-chunk 同谓词 / keyset 与比较器同序 / 标准权限和租户 / master 选择与 RR 调用链的静态评审及编译；通过此门槛后允许迁移工作台和角标。S2/S3 以静态链条及编译/类型/lint证据放行 F02，不再引用下方历史运行门槛否决本轮。
- 仍需记录：SQL 实际执行、实际部署表引擎/数据源一致性、RR 快照和并发行为、精确结果全集、大数据量性能、真实页面全部 NOT RUN/UNVERIFIED，不能记录为 PASS 或“已证实容量”。10秒错误预算、100/101缓冲、完整排序/游标和正常容量设计目标保持；不能通过提高 pageSize、取首批、部分成功或算法 fallback 回避缺陷。
- F02 六 AC 静态检查映射：AC1 五源完整 base/投影/过滤及准确计数；AC2 hidden EXISTS 的 tenant/user/taskKey 身份和筛选前顺序、恢复刷新；AC3 SQL ORDER BY/keyset/Java比较器全tuple同序、尾键和有效页钳制；AC4 失败整请求、非法chunk/timeout与页面 generation/badge epoch 的所有写入守卫；AC5 标准动态权限、eDHR OR、展厅显式 tenant/assignee、旧接口合同；AC6 数据源/只读RR bean代理链、同线程/同事务无切库、缓冲上界和深页成本推导（实际快照/性能未验证）。
- 本轮仅授权任务代码和必要本地提交/主干融合；不推送、不发布、不重启主干服务、不连接或写入真实数据库。共享权限审计在生产标准路径保持，不因本轮未连接数据库而删改。

## 当前边界与证据分类

本轮只编写文档，未运行下述命令、未新增测试源码、未进行真实复现。方案放行以同目录 `review-report.md` / `task-state.json` 为准，由主Agent维护；作者不自行判断。AC完整定义见同目录 `fix-plan.md`；本文逐项映射。主Agent方案评审、文档结构验证、实施后代码验证、真实E2E、上线放行是不同证据。根AGENTS的现行规则不默认强制BDD/TDD或E2E；需要时可按以下行为写Given/When/Then，但本文不是已执行RED/GREEN记录。

合成夹具仅用于单元、行为、mapper与契约验证，不称真实E2E。前端拟复用项目实际 `tests/e2e/release-task-notification-recovery-behavior.spec.cjs` 的Vue compiler-sfc + TypeScript transpile + createRenderer + node:test模式，调用真正SFC handler和API wrapper，mock仅明确隔离网络/Form校验/用户上下文；不得复制生产算法到测试然后只测副本。虽然路径含tests/e2e，以下静态/合成行为脚本均不是Playwright真实页面验收。

## AC与场景映射

| 验收 | 正常、边界、失败场景 | 拟测试位置与主要断言 |
| --- | --- | --- |
| F01-AC1 | TODO+OVERDUE均正式创建；DOING/DONE/CANCELED各有记录；分别仅TODO/仅OVERDUE/空结果。 | `ProfileWorkbenchTodoQueryServiceTest` + `MesWorkbenchTodoMapperTest`：身份集合、businessTotal及分页一致，status白名单精确；前端workbench与badge行为测试消费实际page/count wrapper，不再依赖TODO默认。 |
| F01-AC2 | owner/候选各命中、owner也是候选、非本人、跨tenant；批次10/20/30/40/50/60、ARCHIVE特例；仅批次query动态权益/已撤销权益。 | 真实eDHR mapper及permission实现隔离测试，对照 `MesProEdhrWorkTaskServiceImplTest#getMyPage_includesCandidateFillTaskForNonAssignee/getMyPage_excludesTodoTasksFromTerminalBatches`；新来源SQL与现行my-page按TODO、OVERDUE分别取的完整ID集合相等，重复命中只计一次。 |
| F01-AC3 | OVERDUE FILL/REWORK/REVIEW/ARCHIVE/放行任务导航；缺/错正式导航身份拒绝。 | `profile-workbench-query-behavior.spec.cjs`：真正openTodo调用既有navigateToEdhrWorkTask，传入原正式身份；既有导航合同回归。仅单元证明入口载荷，实际办理成功需后续授权真实路径。 |
| F02-AC1 | 分发71、培训两状态各61、eDHR两状态各73、工单67、展厅45，五源ID故意同号；查询旧首批以外特有词；eq/contains、空白检索、字面%/_、英文大小写、中文；taskType + 一个quickFilter组合。 | `ProfileWorkbenchTodoQueryServiceTest`、每模块 `*WorkbenchTodoMapperTest`：遍历所有全局页与夹具精确全集相等、无重复/缺行；比较canonical显示串与当前映射；filtered count精确。展厅不复用封顶20的旧List判尾。 |
| F02-AC2 | 在全局后页隐藏任务、reload后hidden查询/翻页/筛选/恢复；隐藏最后一行；历史已完成/缺source/无权键；他人同key/跨tenant同key。 | `ProfileWorkbenchTodoQueryMapperIntegrationTest`、`profile-workbench-query-behavior.spec.cjs`：SQL EXISTS身份完整，hiddenTotal只含当前base，businessTotal不减少；成功write后query刷新及effectivePageNo回末页；原visibility服务测试不变。 |
| F02-AC3 | due为空/同值、有due不受created抢先、无due同created、created缺失、同ID跨source；四文本列升降序/clear；每页10/20/50/100，精确整页和最后不足页、总0、请求超末页；相同sort tuple跨100块边界。 | `ProfileWorkbenchTodoQueryServiceTest` + `ProfileWorkbenchTodoSortContractTest` +真实各source mapper：全局比较器、SQL ORDER BY、严格keyset谓词三者同序；自选sort不是当前页排序；generation变更重置page1。 |
| F02-AC4 | 首块/中间chunk/count/隐藏EXISTS失败、显式注入10秒超时、缺/负/小数total、重复key、乱序chunk、游标不推进；旧refresh迟到、sort/筛选/tab快速切换；count C开始→page P开始→P成功→C成功/失败迟到；反向P开始→C开始→C成功→P迟到；hide/restore期间旧读、登出/租户/来源变化；workbench与badge来源不同。 | 后端异常负例与前端deferred Promise测试：整请求无成功部分载荷、错误可见；页面generation保护rows，共享badgeUpdateEpoch令牌保护total/loaded/error/loading，C迟到成功和失败均不能覆盖P；新count意图也使旧P失去badge提交资格；pending不跨epoch/身份/来源复用。hide/restore开始就使旧令牌失效，刷新取得新令牌；不同范围page不能覆盖badge自身count。该超时负例仅证明错误处理，不能代替AC6容量通过。 |
| F02-AC5 | 无分发/培训/工单权限、eDHR OR权限；请求伪造enabledSources/用户ID；展厅自身/他人assignee/错tenant、缺上下文；旧接口消费者；相同来源静止夹具与独立事务间业务变化。 | `ProfileWorkbenchTodoControllerContractTest` 与实际permission/source入口组合；未启用不计，启用无权整体403；新请求VO无用户/tenant参数；展厅显式tenant+登录assignee；旧 `/assignment/page`仍List且最多20；旧my-page默认不变。静止夹具的page/count businessTotal相等；业务变化后的两个独立事务允许不同，各自准确，不把readAt当相同snapshot证明。 |
| F02-AC6 | 同事务counts后另连接完成/新增/隐藏行；同请求继续chunk，后续新请求看到改变；来源切库/新事务注入；每源10,000/100,000匹配行规模的首/中/末页、全范围筛选及四文本排序。 | `ProfileWorkbenchTodoMySqlContractTest`：两实际连接、MySQL只读RR、相同事务/连接统计；100行缓冲上界、SQL调用数/执行计划/耗时。正常容量组合必须10秒技术预算内返回精确完整结果，超时即该组合性能FAIL，环境或授权缺失BLOCKED。只在AC4注入超时验证错误行为，不以整请求失败算AC6通过。 |
| F03-AC1 | 原mobile/email各null/undefined/空白，缺其一/两者只改nickname；原非空但payload联系方式缺失/null。 | `profile-basic-info-optional-contact-behavior.spec.cjs`真实submit+wrapper检查JSON省略；`UserProfileUpdateReqVOValidationTest`真实Validator和`AdminUserServiceImplTest`真实Mapper更新后读取数据库，旧值完整保留。 |
| F03-AC2 | 前端有效更新、手机号少/多位/非法首段/字符、邮箱非法/超50、unique冲突；共享API null/省略、mobile空串/少位/多位、mobile11非数字字符/11空格、email空串/空白/非法/超50。 | 前端实际规则执行：非空非法格式零请求。后端真实Validator、HTTP及Mapper锁定既有合同：null/省略不set；mobile空串原Length拒绝，11字符没有新增正则拒绝，blank unique仍跳过；email空串仍允许set空、空白/非法邮箱按原Email拒绝；有效非blank冲突仍拒绝。不能用直接service调用绕过@Valid证明HTTP校验。 |
| F03-AC3 | 有原值后用户清空；恢复原值或换有效值；已有非法非空值；OAuth2包装调用缺字段/null/有效字段/空email/空mobile/11字符mobile/非法邮箱。 | 前端清空明确字段提示与零请求、重试成功；`UserProfileControllerContractTest`覆盖JSON+原@Valid；`OAuth2UserProfileUpdateContractTest`锁定原Email/Size/Length、unique、blank边界及null不误删，不扩展授权或共享输入校验。 |
| F04-AC1 | 校验deferred期间连点、请求deferred期间连点、保存+重置连点、试图改输入值。 | `profile-reset-password-submit-behavior.spec.cjs`真正SFC：validation≤1/request≤1、loading/disabled、本次已验证载荷稳定。 |
| F04-AC2 | required/确认不符/弱密码校验失败；旧密码错、历史重用、网络错误；失败后修正再次提交。 | 前端零请求或一失败请求、字段保留且finally解锁、无成功toast、本地错误一次、再次成功；既有后端AdminUserServiceImplTest密码方法定向回归，不改其策略。 |
| F04-AC3 | 成功后所有三字段空、clearValidate、按钮恢复；重置正常/新一轮重新填；成功后第二次点击因空表单零请求。 | 前端实际model和按钮状态断言，不仅assert resetFields字符串；后端密码/历史/凭据现有测试回归，无自动会话动作。 |

## 来源对照证据表（S0锁定合同，S1执行）

同一租户/用户/权限、静止合成夹具下，调用现行正式service/mapper完整遍历作对照，执行真实新mapper；不能用只返回预设数组的source mock证明权限/集合正确。ID按source命名空间比较，原来源排序与新全局排序不同，先比全集再按新合同比顺序。每行记录夹具版本、身份/权限组合、旧/新完整ID证据文件、canonical逐字段差异、导航差异、count及报告路径；当前全部NOT RUN。

| 来源与现行对照方法 | 完整遍历及必要对照 | 拟执行证据/原AC |
| --- | --- | --- |
| 分发 `DccDistributionTaskServiceImpl#getMyDistributionTaskPage` | 静止数据按返回total逐页收齐recipientId；核对未签收、PUBLIC_FOLDER、file可见状态及缺file/缺distribution过滤；比detail/publishedTime和原导航ID | DccWorkbenchTodoMapperTest与分发既有service回归；F02-AC1/AC3/AC5 |
| 培训 `DccTrainingTaskServiceImpl#getMyTrainingTaskPage` | PENDING_VIEW、READY_TO_ACKNOWLEDGE分别按total完整收齐progressId后合并；比现行秒数状态及显示字段，不套用预览file状态；核对缺file投影过滤 | DccWorkbenchTodoMapperTest与培训既有service回归；F02-AC1/AC3/AC5 |
| eDHR `MesProEdhrWorkTaskServiceImpl#getMyPage` / Mapper#selectMyPage | TODO、OVERDUE各按total完整收齐正式taskId；owner/候选/重叠/无权限/跨租户、终态及ARCHIVE例外；比正式导航字段和大ID | MesWorkbenchTodoMapperTest、既有eDHR方法、导航真实函数行为；F01-AC1～AC3及F02-AC1/AC3/AC5 |
| 工单 `MesProWorkOrderServiceImpl#getWorkOrderPage` / Controller#buildWorkOrderRespVOList | CONFIRMED/SELF/temporaryFrozen=false按total完整遍历；核对共享可见权限及租户，item名称/缺item与quantity格式、原code导航 | MesWorkbenchTodoMapperTest及MesProWorkOrderMapperTest；F02-AC1/AC3/AC5 |
| 展厅 `ShowroomAssignmentService#pageAssignments/toDetail` | 明确有效pageSize20，静止已知夹具逐页至空页，校对预期全集，绝不用len<50判尾；核对本人OPEN、显式tenant、通知关联、字段文字和assignmentId；旧公开接口仍List≤20 | ShowroomWorkbenchTodoMapperTest及ShowroomAssignmentWorkflowTest；F02-AC1/AC3/AC5 |
| 全来源与隐藏/事务 | 上述准确全集与hidden EXISTS规则得出visible/hidden预期集合；canonical逐字段筛选/排序、keyset跨块无漏重；权限入口用真实permission装配；读连接/事务/表引擎证据另记录 | ProfileWorkbenchTodoQueryMapperIntegrationTest / MySqlContractTest；F02-AC1～AC6。同快照/静止夹具数值相等，不要求独立事务业务变化后恒等 |

## 分阶段执行与证据门禁

以下阶段复用后文既定命令，不新增AC或降低规模。S1必须在替换前端前执行，不能以SFC/静态通过代替。前提检查动作见fix-plan，JDK17/pnpm/Maven、Redis及MySQL当前均未实际检查就绪。

| 阶段/原AC | 应执行的生产逻辑与报告 | 条件及停止范围 |
| --- | --- | --- |
| S0；F01-AC1～AC3、F02-AC1～AC6的前提 | 锁定来源对照合同、实际权限入口、表租户/引擎/主库配置、方法/源码指纹；记录环境/授权检查结果 | 来源合同不清不进入S1；没有MySQL专有库授权不写夹具、不声称容量/隔离验证已做 |
| S1；F01-AC1/AC2，F02-AC1～AC3/AC5/AC6，AC4后端错误部分 | 运行各源真实mapper、QueryService/Sort/Controller/MapperIntegration测试及实际MySqlContractTest；完整源集合、RR两连接并发、首中末页/筛选/排序正常容量报告；超时负例独立 | 正常完整结果与端到端预算均通过，方可S2。FAIL/BLOCKED停止F01/F02迁移和整体验收；分析EXPLAIN/count/chunk成本、修订后复审。前端仍未验证的AC不得提前标PASS |
| S2；F01-AC3，F02-AC1～AC5前端部分 | 真实SFC handler/API wrapper、原导航函数、隐藏恢复及epoch迟到行为测试；同步静态合同并跑定向ESLint/ts:check | 实际函数行为报告为主，静态为辅助。失败停止F01/F02交付，不以原型通过宣称UI修复 |
| 独立F03；F03-AC1～AC3 | BasicInfo真实校验与序列化、Validator/HTTP/OAuth2/真实Mapper更新readback、既有user service回归 | 不依赖F02 MySQL预验证；缺本项JDK/依赖/测试Redis仍阻塞本项。profile.ts按单一整合负责人修改 |
| 独立F04；F04-AC1～AC3 | ResetPwd真实SFC deferred校验/请求、重复点击、失败重试/成功清空及既有密码策略回归 | 可独立推进；本项失败不写PASS，不能用静态loading字符串证明锁生效；与F03共享wrapper整合后两组重跑 |
| S3；全部原15AC | 最终源码指纹下执行后文全套定向命令，逐AC关联实际报告；S1后SQL/索引/事务变化则重跑对应MySQL预验证 | 15AC实际有报告且零未决阻塞，由主Agent判定；F03/F04部分通过而F01/F02阻塞时整体BLOCKED，文档审核不算修复完成 |

证据分类：静态脚本只证明所扫描的结构/约束；合成行为测试执行真实SFC/service/mapper/HTTP/导航生产逻辑，证明相应夹具和边界；MySQL报告证明实际SQL、快照及该规模容量；真实页面只在后续明确授权E2E后验证。四者互不替代，原15AC必须按上表及场景表汇总，不能仅凭空报告模板或静态PASS完成验收。

## 拟新增测试文件与运行命令

以下文件名已选定用于未来实施，当前尚不存在；若实施后缺文件，命令必须明确失败，不能被Surefire无匹配而伪装PASS。PowerShell命令按行运行，不使用&&。

前端目录 `IntRuoyiFronted`：

```powershell
pnpm exec node --test tests/e2e/profile-workbench-query-behavior.spec.cjs tests/e2e/profile-workbench-badge-query-behavior.spec.cjs tests/e2e/profile-basic-info-optional-contact-behavior.spec.cjs tests/e2e/profile-reset-password-submit-behavior.spec.cjs
pnpm exec eslint src/views/Profile/components/ProfileWorkbench.vue src/views/Profile/components/BasicInfo.vue src/views/Profile/components/ResetPwd.vue src/store/modules/profileWorkbenchTodoBadge.ts src/api/system/profileWorkbenchTodo/index.ts src/api/system/user/profile.ts
pnpm ts:check
```

Vue行为测试的form验证替身用于F04异步锁时序；联系方式合法/非法规则另用Element Plus实际校验链/其依赖validator执行，不能让固定返回true替身证明规则正确。依赖缺失时阻塞；无需引入新测试框架。`pnpm ts:check` 是package现有脚本，若发现无关失败应记录具体出处，不改无关代码；不得把未通过全量检查写作PASS。修改SFC必须定向ESLint。

后端新增文件均在相应 `src/test/java/cn/iocoder/yudao/` 现有模块/包路径中：

- server `server/profileworkbench/`：`ProfileWorkbenchTodoQueryServiceTest`、`ProfileWorkbenchTodoSortContractTest`、`ProfileWorkbenchTodoControllerContractTest`、`ProfileWorkbenchTodoQueryMapperIntegrationTest`、`ProfileWorkbenchTodoMySqlContractTest`。
- dcc `module/dcc/service/file/DccWorkbenchTodoMapperTest`；mes `module/mes/service/pro/batchrecord/MesWorkbenchTodoMapperTest`；showroom `module/showroom/workflow/ShowroomWorkbenchTodoMapperTest`：新来源真实SQL、同现行来源ID集合对比、投影显示、count/keyset/隐藏权限合同。
- system `module/system/service/user/UserProfileUpdateReqVOValidationTest`、`module/system/controller/admin/user/UserProfileControllerContractTest`、`module/system/controller/admin/oauth2/OAuth2UserProfileUpdateContractTest`；修改原 `module/system/service/user/AdminUserServiceImplTest` 联系方式null/省略更新断言。
- 对应H2 schema/合成fixture放各模块 `src/test/resources`；对照实际DDL/DO补当前任务所需列和tenant，不在生产库建表。MySQL合同在任务专有数据库中建夹具，不能写int_main或共享E2E业务数据。

后端工作目录 `IntRuoyiBackend`：

```powershell
mvn -pl yudao-module-system -am '-Dtest=UserProfileUpdateReqVOValidationTest,UserProfileControllerContractTest,OAuth2UserProfileUpdateContractTest,AdminUserServiceImplTest,ProfileWorkbenchTaskVisibilityServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl yudao-module-dcc -am '-Dtest=DccWorkbenchTodoMapperTest,DccDistributionTaskServiceImplTest,DccTrainingTaskServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl yudao-module-mes -am '-Dtest=MesWorkbenchTodoMapperTest,MesProEdhrWorkTaskServiceImplTest,MesProWorkOrderMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl yudao-module-showroom -am '-Dtest=ShowroomWorkbenchTodoMapperTest,ShowroomAssignmentWorkflowTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl yudao-server -am '-Dtest=ProfileWorkbenchTodoQueryServiceTest,ProfileWorkbenchTodoSortContractTest,ProfileWorkbenchTodoControllerContractTest,ProfileWorkbenchTodoQueryMapperIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`-am` 编译依赖闭包，`failIfNoSpecifiedTests=false`仅允许上游模块无这些指定类；报告必须实际有每个目标类、非零用例、无失败/错误/跳过才能计PASS。本轮没有验证JDK17/Maven/pnpm安装或测试Redis16379运行；现行BaseDbUnitTest配置为H2和独立测试Redis，缺前提准确阻塞，不改数据源或模拟成功。

MySQL合同命令也已选定，**仅在后续当轮授权任务专有测试数据库写入并提供必要连接配置后运行**：

```powershell
mvn -pl yudao-server -am '-Dtest=ProfileWorkbenchTodoMySqlContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

该拟新增测试从进程环境 `PROFILE_WORKBENCH_TEST_MYSQL_URL`、`PROFILE_WORKBENCH_TEST_MYSQL_USER`、`PROFILE_WORKBENCH_TEST_MYSQL_PASSWORD`读取，不把值写命令/文档/报告。三者缺失或连接非任务专有数据库时明确失败，禁止自动切H2/自动skip；执行方需先确认归属并记录脱敏目标。数据库DDL、MySQL版、目标事务管理器与实际部署一致性需要记录；使用两真实连接并统计事务号/连接/查询时序，注入counts后并发更新，验证单响应一致。没有数据库写入授权时保持NOT RUN/BLOCKED，不为测试自行授权。

正常容量计划：每源10,000及100,000匹配行，额外布置同量不匹配/隐藏/跨tenant行；pageSize100，取page1、ceil(准确total/100的一半)、ceil(total/100)末页。每种规模分别测默认排序、四文本列升/降排序、detail contains/eq（高/低命中率及旧首批以后命中），覆盖visible/hidden查询。各组合以独立夹具预期集合核验精确count及应返回页身份/顺序，记录SQL调用数、各count/chunk耗时、EXPLAIN、内存峰值和总耗时。

正常组合均须在拟定10秒技术预算内返回完整正确结果；超时/错误/遗漏/乱序为该组合FAIL，环境/授权缺失为BLOCKED，不能改判错误处理PASS、静默减少规模或省略末页。每源缓存≤100（limit+1临时101）、HTTP行≤100；归并跳过offset需约O(5+(offset+P)/100) chunk SQL，每块联表/筛选/排序/扫描及准确count成本独立记录。没有现有benchmark，不声称已达到业务SLO。若大规模不达标，修订查询/索引或方案并交主Agent复审后重跑；不自动fallback或换算法，DDL另需当轮授权。

超时错误负例另设独立用例：注入超预算执行或控制deferred调用，验证整请求失败、无成功list/total和前端局部错误。该用例的PASS只映射F02-AC4，不证明上述正常容量或完整可达性通过。

### 容量/快照预验证结果模板（S1填写，当前NOT RUN）

每个正常组合独立填写一份，报告保存于实施任务证据目录并由主Agent登记；模板空白不是证据。端到端计时从完整服务page请求进入至完成响应，含COUNT、全部chunk、归并/导航及响应处理，不是每条SQL各10秒。并发快照场景另记录第二连接的变化及提交时点，来源mapper不得切库/新事务，所有关联表须支持同主库快照。

| 记录项 | 实施时应填内容（当前均NOT RUN） |
| --- | --- |
| 归属/基线 | 测试类/方法、命令、日期、源码/夹具指纹、脱敏专有库归属及授权记录、MySQL版/引擎/配置与部署差异 |
| 规模与查询 | 五源各匹配/不匹配/隐藏/跨tenant行数，pageNo/pageSize/effectivePageNo，sort及方向，筛选字段/算子/值，visibility和enabledSources |
| 预期 | 完整预期ID集合证据文件及本页有序ID、预期total/businessTotal/hiddenTotal；由独立夹具预期而非复制新算法求得 |
| 实际 | 本页有序ID及各total，漏/重/排序差异；实际连接标识、事务起止/RR隔离证据、表引擎、是否同连接；并发另一连接提交时点与后续请求结果 |
| SQL与时间 | COUNT/chunk各次数、每类累计/最大耗时、总SQL数、预读情况、完整服务端到端耗时、EXPLAIN报告；区分DB扫描/排序与归并CPU |
| 内存与报告 | 来源缓存/临时limit+1行峰值、整体内存峰值及测量方式，原始报告/脱敏SQL统计路径，映射原AC |
| 判定 | 正常组合完整准确且≤10秒为容量PASS；超时/错误/漏重/乱序为FAIL；缺环境/授权为BLOCKED；NOT RUN如实保留。记录原因、停止F01/F02迁移及复审动作 |

五源各100,000、total500,000、pageSize100末页5000的推导（约500,000消费行、约5,000块、10秒约2ms/块且未计COUNT/CPU）用于提前提示成本，不是实测结果。不得改算法/预算/规模规避失败；成本不达标先停F01/F02迁移及整体验收，F03/F04可独立但整体仍BLOCKED。

错误负例单独记录注入点、请求/令牌次序、预期整失败、实际错误/页面状态及报告位置，映射F02-AC4；其PASS不出现在正常容量结果栏，也不能填成“末页可达”。

## 已实读的现有定向回归命令

前端 `package.json` 的 `test` 只是 `scripts/run-named-test.mjs`，其中已知target白名单**没有**个人中心target，不能编造 `pnpm test ProfileWorkbench`。下列脚本已读取，使用直接node命令；其目录名含e2e但内容是fs/assert静态合同，不启动浏览器：

```powershell
pnpm exec node tests/e2e/profile-unified-todo-list-static.spec.js
pnpm exec node tests/e2e/profile-workbench-todo-badge-static.spec.js
pnpm exec node tests/e2e/profile-workbench-task-hide-restore-static.spec.js
pnpm exec node tests/e2e/profile-workbench-system-error-static.spec.cjs
```

前三个当前分别写死旧五API imports/requirePageList、角标normalizeAssignmentPage与20条fail-fast、前端hidden Set和hidden-keys调用。实施替换聚合API后必须同步为新page/count合同及真实行为：保留单表/标准模板/全部合法类型、动态batch权限、源身份、非零角标、hidden/restore入口、业务总数不随隐藏减少，删除过时的旧实现token要求。不能直接删测试，也不能仅将正则改成新函数名而不补行为断言。

`profile-workbench-system-error-static.spec.cjs` 当前要求workbench至少6个ignoreErrorMessage和badge至少5个。新请求数减少后，改为读取真实新wrapper并验证page/count关闭全局toast、当前页面/调用方捕获错误、成功write后refresh失败准确呈现；保留Profile Index站内信异常本地处理断言。错误可见/整请求失败由新行为测试承担，不能只用字符串次数替代。

后端既有 `AdminUserServiceImplTest`（BaseDbUnitTest/H2）、`ProfileWorkbenchTaskVisibilityServiceImplTest`、`MesProEdhrWorkTaskServiceImplTest`、`MesProWorkOrderMapperTest`、`DccDistributionTaskServiceImplTest`、`DccTrainingTaskServiceTest`（部分BaseMockito）、`ShowroomAssignmentWorkflowTest`（BaseDbUnitTest）均已定位/实读关联方法。上述模块命令已包含这些真实类；mock领域规则测试只能证明其边界调用，不等于MySQL/真实页面证明。现有 `GxpAuditPolicyStartupMapperIntegrationTest` 展示了server可装配真实MyBatis+租户插件+独立H2测试，采用其隔离装配方式时不启动全应用/业务服务。

已定位并阅读现有定向导航脚本。`edhr-work-task-notify-workbench-fill-navigation-static.spec.js` 中工作台旧`edhrWorkTask:item`/VO类型断言应同步为新来源区分的正式navigation载荷；保留原navigateToEdhrWorkTask、通知身份及workTaskId约束，不放宽业务导航。`edhr-manager-task-route-behavior.spec.cjs` 直接编译实际导航函数，覆盖正式业务范围、大ID和冲突身份，是实际可运行的定向回归。以下命令在`IntRuoyiFronted`执行，仅计划，未运行：

```powershell
pnpm exec node tests/e2e/edhr-work-task-notify-workbench-fill-navigation-static.spec.js
pnpm exec node --test tests/e2e/edhr-manager-task-route-behavior.spec.cjs
```

## 后续明确授权E2E时的真实前端计划

本轮不执行，也不设默认完成门禁。后续收到当轮明确E2E授权后，先读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`；用Playwright真实页面、真实账号及任务自有数据，不在本文写账号凭据。所有被验收动作均由页面完成，API/DB只读终态核验，不借fetch/apiGet或后台写入创建/推进被验收业务。

1. 由正式业务页面生成文控分发/培训、eDHR任务、待排产工单、展厅本人补充指派，达到跨首批数量；个人中心→工作台筛选→翻后页→进入正式任务页面。OVERDUE必须通过正式允许的逾期推进路径形成，不手工改数据库；若缺可达创建/逾期入口，释放/验收任务按规则报告准确阻塞，不扩大范围补入口。
2. 对跨页任务从工作台隐藏→刷新→已隐藏筛选/翻页→恢复→回待办；观察业务总数角标、后页查找和正确原来源身份。完成/无权旧hidden只验证不造行，不直接删业务记录。
3. 由合法管理前端准备任务自有缺联系方式账号；以该账号个人中心基本信息只改昵称；另一个已有联系方式账号清空应显示明确提示，再输入有效值保存。页面readback及只读核验现有联系方式，不能用直接profile/update承担改昵称动作。
4. 任务自有账号个人中心修改密码，真实提交期间连续点击、失败后修正和成功清空；仍从前端检验现行密码策略/登录行为。不要对共享E2E账号改密码，不把真实密码记录到截图/日志。

真实E2E需新的当轮数据/服务/入口检查，本文不承诺可用。合成100,000行性能夹具不属于真实E2E数据，不用API生成数万任务冒充前端动作。

## 执行证据与最终判定格式

实施后每个AC记录：实际测试类/脚本、命令、工作目录、源码基线或改动指纹、用例数与失败/错误/跳过数、时间、PASS/FAIL/BLOCKED/NOT RUN。将schema/依赖错误与业务断言失败分开；静态检索不是RED，文档结构验证不是GREEN。只有原15AC均有真实执行报告、无失败及未决阻塞，才能交主Agent确认四项修复完成；部分通过不能报告整体完成。E2E不是默认门禁，未授权/未运行真实页面须如实列NOT RUN；方案、实施、上线授权各自保持原边界。仅主Agent可以决定方案或实施放行，所有上面的命令目前统一为 **NOT RUN**。
