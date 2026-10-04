# G21 17项外部结构/基础前置合同

2026-10-03。由`g20-external-prerequisite-checklist.json`的25项扣除后台Owner的8项BPM/策略/notification，精确17项。NAV-01/02的Root修复及所有前批成果保留，本次没有修改任何FE/Java/正式SQL/Root文件，未连接数据库或运行服务、Maven、type/build、Git。

## 可交Root采集的文件

- `g21-structure-prerequisite-queries.sql`：7条只读SELECT，输出JSONL；查询runtime、表engine/collation、列type/nullable/collation/default/extra/生成表达式、索引unique/顺序/前缀/方向/表达式、6项已有ledger原SHA及两个必要数据标量。没有DML、DDL、CALL或session写入。
- `g21-structure-prerequisites.json`：17逐项合同，41个基础表、562必要列、110索引；额外列忽略，不把19候选将加字段作为前置。每项`migrationMustNotExecute=true`，`executionAuthorized=false`。
- `g21_structure_prerequisites.py`：离线准备及validator；没有DB/network/subprocess接口。JSONL中错误/警告/非JSON、重复事实、缺表/列、错误shape均FAIL。结果逐migration给事实结论，只能是`SATISFIED_READ_ONLY_FACTS_NOT_APPLIED`或FAIL，不能伪造历史APPLIED。
- `test_g21_structure_prerequisites.py`：13项离线正/负验证。
- `g21-prior-source-consistency.json`：protected dump与已有日志一致性核对，仅记录前次证据；不是本次新数据库采集结果。

Root采集时保持明确MySQL目标`ruoyi-vue-pro`、`--batch --raw --skip-column-names --default-character-set=utf8mb4`；stdout保存UTF-8 JSONL，stderr独立保留并检查退出码。不要把SQL与迁移文件拼接，不输出连接凭据。validator只接受规范JSON对象逐行，不能删除错误行后宣称PASS。

离线校验命令（Root填写采集日志/result的实际路径；脚本路径从整合树解析）：

```powershell
python doc/tasks/20261002-dcc-detail-integration/g21_structure_prerequisites.py validate --facts <fresh-select-jsonl.log> --result <g21-facts-result.json>
```

此命令本身不连接数据库。失败返回exit1，result标FAIL。不要把离线positive fixture或protected旧日志当fresh采集传入。

## 来源、精确形态与历史边界

protected schema dump的压缩SHA重新计算精确等于receipt `cf6a94375b95720637d7b75a471e508420ffa9cc6b399c4438851237d4fc66fa`，只读取schema包，不解读含业务/凭据的数据dump。各17正式SQL当前raw SHA与g20清单全部一致。非base纯结构表的声明type/nullable/default/extra和命名索引先逐一与protected CREATE核对，不因同表名就满足。SQL未指定table collation的少量表，冻结当前protected实际collation并在合同basis明确说明，不推测默认为unicode。

base只要求19候选需要的旧基础字段/表：File、Master、Project、signature、obsolete audit、access log、category、directory、route/node。没有要求本次`latest_controlled_file_id`、`controlled_time`、attrs/defaults、`source_original_file_name/c_version_key`、owner、attempt等新增合同提前存在；P1候选会处理Master的3个新身份字段，因此本前置也不要求它们。额外新列或索引出现不触发假失败。

base存在的legacy nullable File identity保持精确`YES`，不是把current CREATE的NOT NULL强压历史；access_log.reason前置为现有255、operator_id前置非nullable，本次forward文件扩大/允许系统actor是后续步骤。完整base初始化/菜单重跑不属于本任务。

历史ledger6条原值均保留：4项当前SHA匹配（taxonomy、notify business key、config menu、FormCenter）及2项精确旧来源（base、catalog）。validator若收到当前不同语义base/catalog SHA替代原SHA则FAIL。11项未登记前置只验证结构，不生成APPLIED或写账本；并不因missing ledger判必须重放旧初始化。

catalog声明DDL与原账本SQL相同；当前seed181→186行是另一次源数据变化，不重播、不验证其“当前种子已导入”。本前置只验证catalog列/unique source-row和查询索引，以及原ledger。

FormCenter全部6表的必要列及索引精确验证。`system_tenant_package.menu_ids`实际已由`20260724_system_codex_test_management`扩为LONGTEXT NOT NULL unicode_ci，冻结更大现有类型，绝不为满足旧FormCenter脚本缩成TEXT或重授菜单。config-menu项证明原SHA及system_menu/role_menu/tenant_package基础表形态；不声称每个菜单授权、业务账号或角色配置均已验证，亦不触发菜单重播。

两个生成表达式：name_claim.active_unique_flag必须STORED `CASE WHEN deleted=0 THEN1 ELSE NULL END`；checkout.active_master_id必须STORED `CASE WHEN status='ACTIVE' THEN master_id ELSE NULL END`。仅规范化MySQL非语义括号、反引号、charset literal introducer；保留字符串大小写及条件/返回列，错误状态/actor返回/virtual类型拒绝。unique列序、前缀长度、方向及BTREE精确匹配。

route `action_type`及新unique三列形态、旧unique不存在必须成立；新增只读标量证明没有NULL/空action_type。notify business_key type255 nullable/no-default + unique(tenant_id,business_key)严格匹配，并查非NULL重复组为0。旧菜单/流程/政策DML不自动执行。

## 17项覆盖映射

| 外部前置 | 合同事实 |
|---|---|
| 20260513_dcc_base_schema | 10个必要旧base表字段/PK，原历史SHA；不要求候选待加列 |
| 20260921_dcc_category_approval_route_action_type | action_type、unique(category_id,action_type,version_no)、旧unique无、空值0 |
| 20260922_dcc_task_assignee_snapshot | 现有snapshot全声明列、2个unique及用户索引；本轮A round字段不提前要求 |
| 20260710_dcc_product_catalog_database | 全声明列/4索引及原SHA，不验证或导入0901新种子 |
| 20260903_dcc_explicit_data_relation | data_relation列/default/collation、2unique与3查询索引 |
| 20260920_dcc_project_product_create_approval | request/relation/claim3表全声明基础列和索引，不要求Battrs/audit/reviewer新列 |
| 20260719_dcc_file_type_taxonomy | taxonomy表、category/File/recognition leaf字段、原SHA；不改menu/role |
| 20260910_dcc_project_file_template | template_item基础列和unique/index |
| 20260917_dcc_controlled_file_name_claim | claim列bin名称、STORED active flag、旧tenant/name唯一和Master索引；C新列后加 |
| 20260906_dcc_new_file_lifecycle_p2 | checkout列/4索引/STORED active Master，以及File正文证据/前驱字段 |
| 20260906_dcc_new_file_lifecycle_p3 | nullable bigint revision_base_active_controlled_file_id |
| 20260815_system_notify_message_business_key | type255/nullable/no-default/tenant unique、重复组0、原SHA |
| 20260903_dcc_controlled_file_related_file | historical snapshot基础列及target/project/unique完整形态 |
| 20260615_system_config_package_menu | 原SHA和3个system基础表schema，禁止菜单/role/package重放 |
| 20260717_bpm_form_center | 6表全声明字段/index、现menu_ids容量、原SHA，不验证策略数据 |
| 20260907_dcc_publication_followup | 7个已存在followup/冻结visibility/relation基础表全声明列与索引 |
| 20260907_dcc_publication_impact_assessment | impact_task/audit列、nullable/collation/default和index |

8项排除清单保存在generator的OMIT，正好包含Root指定的三流程seed/candidate/multiinstance/business-policy/upload-policy/publish-policy/P4/publication-notification；它们由后台Owner提供，不能用本562列schema PASS替代其BPM/策略/notification数据证明。

## 实际验证与仍待Root采集

离线13项PASS：合法fixture不产生APPLIED；同名错误table engine/collation、缺列/错type、nullable/default/column collation、unique顺序/前缀/nonunique、错误generated/virtual、原SHA被替换、外数据库/缺ledger、空route/重复notify、候选新列未强迫、只SELECT及JSONL保留NULL/错误行fail。

已有日志与protected核对：524个列shape/生成表达式和424个default/collation均一致，110索引中已采集范围对应项一致。旧g14没有3个system表的38列/PK元数据；旧g18 defaults没有138列，均不解释成真实缺结构，protected dump已证明，新SELECT会完整补齐。notify duplicate scalar仍待fresh SELECT；route空值在g18为0，但也会重新采集。

**当前仅prepared，不是fresh target PASS。** Root取得本SELECT JSONL并运行validator才可把17项标成当前事实满足；即使PASS也不授权执行19候选，不改old ledger，不重跑旧SQL，不代表应用启动、真实E2E或后台8项完成。
