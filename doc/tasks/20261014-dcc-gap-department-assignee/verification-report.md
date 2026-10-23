# G05 验证报告

## Root正式M05与本线程只读复核
M05句柄38484已exit0，23reactor BUILD SUCCESS。当前main/testCompile之后四目标类80/80PASS、0fail/error/skip；本线程只读复核Root归档m05-junit四份XML，逐case数量/状态及SHA与m05-verification.json一致。分别为DccWorkflowSignoffAssignmentTest 38、DccPublicMatrixNativeDerivationTest 13、DccExactVersionReferenceUsageTest 10、DccProjectReferenceTransactionTest 19。G05当前Service/Test字节SHA与M05 source-before一致，17交还schema/fixture当前hash也全相符。未在本线程重复执行或编译。

正式命令及来源：`doc/tasks/20261014-dcc-gap-fix-coordination/m05-reactor-receipt.json`、`m05-verification.json`、`m05-source-before.json`、`m05-source-drift.json`、`m05-junit/`。命令为`mvn -pl yudao-module-dcc -am -Dtest=DccWorkflowSignoffAssignmentTest,DccPublicMatrixNativeDerivationTest,DccExactVersionReferenceUsageTest,DccProjectReferenceTransactionTest -Dsurefire.failIfNoSpecifiedTests=false test`。Root本票2285输入有6处并行drift，非整个工程最终freeze；80只覆盖会签/公共派生/精确引用及引用事务，不能替代custom/新确认/附件完整链或14fixture整类JUnit。原M04 testCompile失败保留，不覆盖为PASS。当前无活跃Maven，M04/M05等待已解除，M06另票。

以下私有/隔离schema报告保留各自范围；其中历史“尚无Maven票”的说法只描述该次执行，G05会签38现新增上述正式M05证据，不把80与98或50简单相加。

## A23/A26当前接入
Actual G03 DDL已落盘读取，三custom字段guard/types/null/no-default与actualFileDO对应；依赖两文件存在。A23 `--phase current-red` exit1、正式门禁READY、真实缺列7FAIL；仅加3列后`--phase green` exit0、7/7 H2。A26 `a26-route-contract.py --phase red` H2 1PASS/3FAIL、当时当前public guard11PASS；一处nullability后green/final-public-gate exit0，H2 4/4、fresh3-source javac0、实际PUBLIC canonical/readCanonical11/11。最终版本负向payload只改合法canonical的version，不靠其他raw字段省略提前失败。当前schema`v01-schema-contract.py --phase a23-a26-final-regression` exit0=50/50（原H2 16），全文件逆向保护PASS。未跑PUBLIC完整snapshot保存/全部restore、14类JUnit、原98、reactor或MySQL迁移；最终17输入限定handoff见schema-handoff.json，不能冒工程整体验收。

## A21 当前正式源接入
正式20261015_dcc_checkin_payload_hash.sql、CheckoutDO和Mapper源码出现后实际只读核验，保存3源SHA稳定。`a21-checkout-contract.py --phase current-red` exit1，正式源READY、H2五项真实缺列FAIL；测试schema仅新增nullable CHAR64无default后`--phase green` exit0、5/5PASS。旧NULL保持、64存储向量精确保留、超64真实22001零插入。`v01-schema-contract.py --phase a21-current-regression` exit0、46/46PASS（含原H2 16项），精确差异反向还原PASS。明细见a21-results.json；不是G06服务canonical/replay/事务验收，不是14类JUnit，不执行MySQL迁移。未来3个custom draft列不预造。

## V02 明确14文件fixture
`v02-fixture-contract.py --phase red` exit1=42PASS/14FAIL（结构缺显式来源）；`--phase green` exit0=56/56；`--phase regression --h2` exit0=56/56加真实独立category H2列约束通过。14文件18调用、1125原assert/verify/andExpect AST一致，去18调用原byte/LF都可还原。H2仅提取这18字面值和当前category CREATE TABLE验证，不实例化原DO/原服务，不计14类JUnit。详见v02-review-ready.md与三结果JSON，原G05 98未重复编译。A21接续独立证据见上，不能与V02服务类计数混加。

## V01新增验证准备（2026-10-15）
- 真实RED：`python -X utf8 doc/tasks/20261014-dcc-gap-department-assignee/v01-schema-contract.py --phase red`，exit1；42检查=13PASS/29FAIL（H2 16项=4PASS/12FAIL），证明当前schema缺六确认列/类别source；显式精度与合法fixture为结构RED。旧H2默认精度已为6不冒称丢精度。
- GREEN：同入口`--phase green`，exit0，42/42PASS，真实H2 16/16。
- REGRESSION：同入口`--phase regression`，exit0，45/45PASS；新增3个整文件逆向差异守卫，通过后证明只改授权列/精度及六请求/helper，其他表列/原断言不变。
- 仅java source launcher运行自有JDBC合同、直接执行四张当前CREATE TABLE原文，无schema改写/全局H2改动，所有连接都是随机命名内存库；未编译G05或当前业务依赖，不重复98。
- 明细见schema-current-contract.md及v01-results.json，准确hash/最小diff已归档。没有Maven/TS/构建/服务/E2E/实库/迁移执行，不声称分类服务原负例已运行或签名verify/HMAC通过。额外fixture清单只读，待Root授权。

以下保留原G05已执行证据，不能与V01检查相加。

## 结论
三个授权生产文件修复完成，真实服务/组件RED已记录；原隔离Java回归98/98、前端26/26均0skip，现新增Root正式M05会签38/38通过。M05其他三类仅其明确范围，不代表全部新业务、E2E、运行服务或实库验收，工程未标completed。

## BDD 与证据
| 场景 | 当前执行证据 |
|---|---|
| A义务选择B用户不能保存 | 参数化 upload/revision/obsolete 三种真实服务路径均抛正式 CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED，零签名/快照更新/转派/关系冻结 |
| 本部门成员和本人可指派 | 现有 selfAssignment/otherAssignee 实际执行，保持签名时间、任务和冻结义务；正式会签不自动完成 |
| 本人不在义务部门不可自指派 | frozenLeaderOfAnotherDepartmentCannotAssignThemself 与候选排除，零写 |
| 加载后转部门 | memberMovedAfterCandidateLoadingIsRejectedUsingTheCurrentDirectory，先实际读取候选，再改变目录 DTO，再保存，正式拒绝且零写 |
| 前端无跨部门入口 | 编译并真实渲染 SFC，只有一个人员 selector、无 ALL option；旧 loader 未调用；注入B用户不写 |
| 当前候选/任务独立复核 | Panel 当前候选被移除后不能保存；Detail 拒绝越界人员及不同 request.taskId |
| 资格/签名/事务/重放保持 | 原岗位与 readiness拒绝、签名失败/缺时间、租户/轮次/流程定义、载荷冲突、H2晚失败回滚、真实关系冻结重放测试继续通过 |

## 实际命令及结果
- RED Java：`python doc/tasks/20261014-dcc-gap-department-assignee/run-isolated-tests.py --phase red`，javac exit0，JUnit exit1，38 found / 31 PASS / 7 FAIL / 0 skip。失败确为跨部门新规则，非编译或环境失败。
- RED FE（cwd IntRuoyiFronted）：`node --test tests/unit/dcc-signoff-department-assignee.test.cjs`，exit1，4 found / 1 PASS / 3 FAIL。
- GREEN/REGRESSION Java：`python -X utf8 doc/tasks/20261014-dcc-gap-department-assignee/run-isolated-tests.py --phase regression --regression`，javac exit0、JUnit exit0，98 found / 98 PASS / 0 FAIL / 0 skip。三类包含继承的相邻用例，不能再与38叠加计数；耗时6111ms为 JUnit 执行时间。
- GREEN/REGRESSION FE（cwd IntRuoyiFronted）：`node --test tests/unit/dcc-signoff-department-assignee.test.cjs tests/unit/dcc-workflow-components.test.cjs tests/unit/dcc-signoff-remediation-scope.test.cjs tests/unit/dcc-workflow-ic1.test.cjs`，exit0，26/26 PASS、0 skip。
- 中间隔离编译/运行环境错误及旧 integration fixture 两项不符新需求的失败已完整记录在 execution-log.md，未记为业务 RED，也未隐藏或跳过用例。

## 证据边界
2026-10-14 Root 接续结论：已独立复核三个生产最小 diff 并接受软件实现 review，实际重跑4项前端组件测试 PASS；此为 Root 转交的独立复核结果，本线程本轮未重跑。Maven/reactor 当前源码及依赖尚未验证，原98项 private范围限定保持，等待 Root 正式统一票。生产和测试文件已接管冻结，不重复编译。

相关当前源码实际重新编译，mock 外部组织/BPM/签名端口；H2 验证在任务自有内存库完成。目录查询不是线上真实组织变动验收；签名测试不声称已调用线上签名系统。没有改写依赖以放宽业务规则。

## 收尾
最终7个编译输入SHA稳定、文档结构/UTF-8校验、定向 diff --check 均PASS。ready_for_closeout 后 task_closeout.py preview exit0（keep9/delete67/warnings0）、apply exit0（keep9/delete68/warnings0，多1个preview临时日志），仅清理本任务私有产物，9份交付文档/脚本/指纹保留。Git 收尾未授权，任务最终状态为 blocked，等待 Root review/验证和后续授权。
