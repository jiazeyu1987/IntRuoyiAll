# G05 会签仅指派本部门人员

## Current Status
blocked — Root M05正式四类80PASS（含G05会签38），XML只读复核一致；A23/A26与17授权schema/fixture已交还，限定合同保留。M04/M05等待已解除，当前无活跃Maven。剩余整体联合验证/工程Git收尾未完成，14类/新custom-confirm-附件完整链/E2E/运行库未验收，不标completed。

## 任务目标
负责人只能签名指派本次义务部门的启用且岗位、权限、签名授权有效人员。本人可以被指派，但必须属于该部门。移除会签全员跨部门搜索；保存时独立读取当前目录复核。

## 里程碑
1. 核对 int_qms、规则、当前代码和相邻测试，保存改前 SHA256。
2. 写 BDD 并运行真实服务/组件 RED。
3. 专属文件 GREEN，运行相邻回归，提交 review 资料。
4. ready_for_closeout 后记录未获 Git/Maven/运行验收授权边界，不自动 completed。

## 预期验证
- 当前源码隔离 javac/JUnit：部门候选、直接保存跨部门拒绝、自指派、人员转部门、岗位/授权、准确任务/租户/轮次、签名失败、事务和重放。
- Node/Vue SFC 实际编译执行：无全员入口，只消费正式部门候选；越界选值不能保存；本人/本部门可保存；任务切换及错误保持。
- 相邻现有前端/后端测试按实际执行报告；无 Maven 唯一票不运行 Maven、全类型、大构建。
- 文档结构及当前任务清理 preview。

## BDD
- Given A 部门负责人办理 A 义务，When 提供 B 部门用户，Then 前端无跨部门入口且正式保存拒绝，零签名、快照写入与任务转派。
- Given A 部门启用有岗位且就绪的成员，When 负责人签名指派，Then 持久化准确义务及签名时间并转派，不能完成实际会签。
- Given 负责人本人属于 A，When 指派本人，Then 按相同资格规则允许；本人不属于 A 时拒绝。
- Given 候选加载时属于 A，When 保存前转至 B，Then 以保存时目录拒绝且零签名/推进。
- Given 已签名的相同请求，When 精确重放，Then 读取已提交事实且不重新签名；冲突载荷拒绝。

## 设计约束检查
- 授权仅 DccWorkflowSignoffAssignmentService.java、SignoffAssignmentPanel.vue、DetailSignoffAssignment.vue、专属测试与本目录。
- 不修改核心 Workflow、公共 Controller/VO、共享 fixture、公共 API；不触碰其他线程资产。
- 不启子 Agent、不提交推送/切分支/worktree/E2E/服务/远端/实库写入。
- 不 fallback、不吞异常、不默认原因；保持租户、组织、精确任务、版本、签名、事务和重放规则。
- Detail 中整改负责人全员目录是独立业务，只移除其作为会签候选的用法。

## V01 独立目标与里程碑（2026-10-15）
- 目标：按正式20261014确认迁移/当前DO六列与C05时间精度、20261015类别审批来源迁移，对齐DCC及signature两份test create_tables.sql；只修分类服务测试6个SaveReq和createCategory helper的显式PUBLIC_MATRIX。
- 范围：DCC/src/test/resources/sql/create_tables.sql、signature/src/test/resources/sql/create_tables.sql、DccFileCategoryAdminServiceImplTest.java及本目录合同测试/报告。其他表列、全局H2模式、BaseDb、cleanup与原G05业务源/test不动。
- M1：保存当前改前字节/规范化SHA及精确快照，读取正式DDL/DO与相邻fixture。
- M2：当前schema合同真实RED（执行实际目标CREATE TABLE至隔离H2），再最小修改GREEN；静态核对显式timestamp(6)、六列类型/null、无default、六请求及helper。
- M3：精确反向差异验证确保其他表列/所有原断言不变，交schema-current-contract.md、SHA和最小diff给Root统一验证。
- Given 当前正式DO含六确认字段，When H2按仓库test schema建表并写读，Then 字段可用、nullable无默认、id/round精确且选定E与签名S独立。
- Given C05服务器微秒S，When 写读统一signedAt、DCC signedAt、confirmedAt和controlledTime，Then 各层按timestamp(6)原值保存；旧秒值不改变。
- Given 正式类别来源必选，When 明确PUBLIC_MATRIX/APPLICATION_CUSTOM写读，Then 原值保持；缺失/null不由数据库默认补值。
- Given 原分类服务权限/归属/目录负例，When fixture显式PUBLIC_MATRIX，Then 不被新source guard提前拒绝；原断言/负例一字不改，实际服务验证仍等待Root票。
- 验证边界：隔离H2和结构合同，不Maven/TS/构建、不重复原98编译、不声称签名HMAC或服务整类验收。未知来源负向及其他受影响fixture须先交Root授权。

## V02 独立目标、BDD与验证（2026-10-15）
- 范围仅v01-fixture-candidates.md中14文件的正常有效Category builder/helper；最多每builder新增一个显式PUBLIC_MATRIX调用。未知/malformed来源负例、跨租户、权限、目录及失效场景的原assert/数据身份保持；不扩大其他mock、inline SQL、随机generator、production或cleanup。
- 先核每个builder上下文、保存14文件byte/LF SHA和源码快照及原断言AST；若快照后同文件任何合法drift先报告，禁止覆盖。V01清单仅行锚点未存全文件hash，不声称能证明此前至今字节无漂移；以本轮实际baseline开始比较。
- Given 正常有效Category fixture未显式审批来源，When 当前NOTNULL schema写入或当前来源guard被消费，Then 明确配置PUBLIC_MATRIX且保持所有原权限/归属/目录拒绝断言，不以来源提前失败冒原测试PASS。
- Given 任意未知/malformed审批来源负例，When 更新正常fixture，Then 不补默认、不修改其assert或受测数据。
- Given 14文件改前断言AST与完整字节，When 每builder只加一个调用，Then 断言AST完全一致，去精确增量后原字节和规范LF均可还原。
- 运行限定静态/AST RED/GREEN与独立H2精确schema合同，不执行14类JUnit、不重编98、不Maven/全类型；交v02最小diff/每文件manifest与边界报告。

## A21 checkout hash测试schema追加
- Root追加DCC test create_tables.sql中checkout.checkin_payload_hash CHAR(64) NULL，无默认/历史补值；DO/Mapper/正式DDL/G06tests仍只读。
- 初次正式20261015_dcc_checkin_payload_hash.sql及DO尚未出现字段，已准确记录依赖未就绪；本次接续已实际出现DDL/DO/Mapper合同，保存新来源SHA后继续，不修改其生产源码。
- Given 新checkout完整hash字段为nullable CHAR64，When 旧行省略/显式NULL或新明确64hex写读，Then 旧NULL保持、新64字符精确保留、没有默认；超64长度真实拒绝。数据库不承担servercanonical正确性，G06正式后端验证另待Root票。
- 改schema前单独保存V01后当前schema baseline SHA/快照；基于正式DDL/DO扩当前schema契约，真实RED→GREEN，其他列保护不变。

## A23 正式custom草稿字段测试schema
- Root仅授权FileDO已存在draftApprovalSourceMode/draftApplicationCustomMatrixJson/draftApplicationContextKey三个nullable对应列，类型按G03正式DDL核验；不新增round/其他字段、不写生产DO/DDL/Mapper/服务/G03test。
- 当前DO三字段与正式DDL均已实际读到，历史首次DDL未落盘保留记录。本次已对actualDDL三CALL/types/null/no-default/direct dependencies准确核验，A26后baseline CAS才加3列，真实GREEN已完成。
- Given 历史测试schema与实际新DO三个nullable草稿字段，When 旧行省略保存，Then 三字段保留NULL、无schema默认/历史补值。
- Given 明确mode/完整JSON/opaquecontext存储向量，When 按当前测试schema保存/读回，Then 三字段精确保留，JSON长文本不截断、context128边界保持；两个版本行不串字段，不以存储测试冒称G03申请/轮次业务验收。
- Given 当前schema完整baseline，When 只增正式三列，Then 去3行增量后byte/LF完全还原，无新round列及其他表列/索引改动。
- 验证：实际DDL/DO来源SHA、精准H2 RED/GREEN/长度/null合同、当前schema结构/相邻限定回归、正式checkpoint manifest交Root；不重跑98/14JUnit/Maven/TS/运行库。

## A26 route snapshot版本nullable合同
- Root已明确授权现存route_snapshot.route_version_no INT NOT NULL→INT NULL，无default1，不改其他列或业务Source/RouteDO。G03正式custom迁移由其owner维护；A26此既有列nullability依据当前schema和明确Root合同，不等同A23三个字段可以预造。
- Given custom没有公共route version，When schema存新custom行NULL，Then NULL原样保留，不能填假1；省略版本无默认值。
- Given 已存公共行真实版本7，When 允许其他custom行版本NULL，Then 原公共行列值及摘要不变。
- Given PUBLIC canonical/source缺version或为0，When 当前生产canonicalizer及readCanonical恢复公共baseline，Then 明确拒绝；正数版本精确保留。此仅公共来源合法性守卫，不冒完整snapshot save/restore服务全验收。
- 保存A26独立baseline/hash；真实H2 shape RED→GREEN与小范围fresh javac当前公共Canonicalizer/Source负向合同，不能重跑98或Maven/大构建；精确逆向diff与正式后续schema checkpoint交Root。

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-department-assignee/changed-files.md
- doc/tasks/20261014-dcc-gap-department-assignee/integration-notes.md
- doc/tasks/20261014-dcc-gap-department-assignee/review-ready.md
- doc/tasks/20261014-dcc-gap-department-assignee/baseline-hashes.json
- doc/tasks/20261014-dcc-gap-department-assignee/run-isolated-tests.py
- doc/tasks/20261014-dcc-gap-department-assignee/final-hashes.json
- doc/tasks/20261014-dcc-gap-department-assignee/schema-current-contract.md
- doc/tasks/20261014-dcc-gap-department-assignee/v01-source-hashes.json
- doc/tasks/20261014-dcc-gap-department-assignee/v01-minimal.diff
- doc/tasks/20261014-dcc-gap-department-assignee/v01-schema-contract.py
- doc/tasks/20261014-dcc-gap-department-assignee/V01SchemaContract.java
- doc/tasks/20261014-dcc-gap-department-assignee/v01-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/v01-fixture-candidates.md
- doc/tasks/20261014-dcc-gap-department-assignee/v01-baseline/dcc-schema-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/v01-baseline/signature-schema-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/v01-baseline/category-admin-before.java
- doc/tasks/20261014-dcc-gap-department-assignee/V02AssertionAst.java
- doc/tasks/20261014-dcc-gap-department-assignee/V02CategorySchema.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-fixture-contract.py
- doc/tasks/20261014-dcc-gap-department-assignee/v02-file-manifest.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-assertions-before.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-assertions-after.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-minimal.diff
- doc/tasks/20261014-dcc-gap-department-assignee/v02-red-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-green-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-regression-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-unowned-fixtures.json
- doc/tasks/20261014-dcc-gap-department-assignee/v02-review-ready.md
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccAdminFullConfigPackageServiceTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccCategoryApprovalMatrixAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccCategoryDistributionRuleAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccCategoryPermissionAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccCategoryTrainingRuleAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccCategoryViewMatrixAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccFileTypeTaxonomyAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccObsoleteFormOpenDraftDatabaseTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccPublicFinalizationTransactionTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccUploadNamePreflightDatabaseTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccWorkflowAttributesIntegrationTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccProjectCodeServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccProjectConfigurationHttpPersistenceTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/v02-baseline/DccApprovalRouteAdminServiceImplTest.java
- doc/tasks/20261014-dcc-gap-department-assignee/A21CheckoutSchema.java
- doc/tasks/20261014-dcc-gap-department-assignee/a21-checkout-contract.py
- doc/tasks/20261014-dcc-gap-department-assignee/a21-file-manifest.json
- doc/tasks/20261014-dcc-gap-department-assignee/a21-schema-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/a21-current-contract.md
- doc/tasks/20261014-dcc-gap-department-assignee/a21-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/a21-minimal.diff
- doc/tasks/20261014-dcc-gap-department-assignee/A23DraftSchema.java
- doc/tasks/20261014-dcc-gap-department-assignee/a23-draft-contract.py
- doc/tasks/20261014-dcc-gap-department-assignee/a23-file-manifest.json
- doc/tasks/20261014-dcc-gap-department-assignee/a23-schema-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/a23-current-contract.md
- doc/tasks/20261014-dcc-gap-department-assignee/a23-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/a23-minimal.diff
- doc/tasks/20261014-dcc-gap-department-assignee/schema-handoff.json
- doc/tasks/20261014-dcc-gap-department-assignee/a23-schema-first-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/A26RouteShape.java
- doc/tasks/20261014-dcc-gap-department-assignee/A26PublicSourceGate.java
- doc/tasks/20261014-dcc-gap-department-assignee/a26-route-contract.py
- doc/tasks/20261014-dcc-gap-department-assignee/a26-file-manifest.json
- doc/tasks/20261014-dcc-gap-department-assignee/a26-schema-before.sql
- doc/tasks/20261014-dcc-gap-department-assignee/a26-results.json
- doc/tasks/20261014-dcc-gap-department-assignee/a26-minimal.diff
- doc/tasks/20261014-dcc-gap-department-assignee/a26-current-contract.md
- doc/tasks/20261014-dcc-gap-department-assignee/schema-final-minimal.diff
