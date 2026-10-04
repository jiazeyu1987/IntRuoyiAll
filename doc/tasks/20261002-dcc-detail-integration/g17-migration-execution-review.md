# G17 本机迁移执行准备只读审查

2026-10-03。当前18根/44依赖包不能整包按“37项无账本”盲跑。真实日志确认17张新表、51个新增列缺失；另有一个在44闭包外的明确容量缺口：`dcc_controlled_file_access_log.reason`仍为255，已有前向迁移应扩到2000。建议授权方案单列该文件，形成19根/45闭包；这只是待审批计划，没有执行。

数据源仅采用主管理已获取的`int-ruoyi-mysql / ruoyi-vue-pro / MySQL 8.0.40`只读证据。本Agent没有连接数据库、运行服务、Git、Maven、type/build或任何DML/DDL；只读当前整合SQL及日志、写任务报告/manifest。服务429退出不会使已采集schema失效，但实际运行新Jar前仍须Root核对同一数据源及采样后有没有其它写者。

## 证据与判定口径

主管理任务目录`doc/tasks/20261001-dcc-integration-unblock/`：

- `g13-migration-package.json`：18根、44拓扑依赖、policy passed。逐文件重算当前原始SHA，44/44与包一致，未发生本次SQL漂移。
- `g13-runtime-complete-ledger.log`与comparison：5项当前SHA匹配、37项无登记、2项历史SHA不同。状态均须按实际ledger解读，不能把旧`SKIPPED_ALREADY_APPLIED`推导成当前语义已执行。
- `g13-runtime-schema-preflight.log`：运行数据库身份、部分列/索引、目标activation handler查询无结果。
- `g14-schema-complete.log`：3749列（包括生成表达式）、1397索引记录、216个表大小记录。采集范围为DCC/BPM/Flowable/GXP/signature和列出的infra/system表，不是整个MySQL所有库。TABLE_ROWS是估计值，不能作为精确历史COUNT基线。
- `g17-ledger-source-search.json`、`g17-ledger-semantic-diff.patch`：Root找到两个ledger精确历史来源。本Agent反向应用冻结diff到当前文本，重建原SQL并转换CRLF，两项SHA均精确等于原ledger；过程没有调用Git或改SQL。

本任务长期证据：`g13-migration-closure-evidence.json`、`g14-migration-structure-evidence.json`、`g17-ledger-semantic-review-proof.json`、`g14-migration-execution-review-manifest.json`。语法提取JSON是辅助定位，最终表中判定结合SQL和实际日志人工复核；BIT未写宽度按MySQL BIT(1)规范化，未把它当差异。

## 两项历史hash已查明：都不是仅换行

| 迁移 | 原来源与原SHA | 本次结论 |
|---|---|---|
| `20260513_dcc_base_schema` | `03646727bcf91611425da89b730f7b32b16ad0a1`；CRLF `2a57d12b245485017f67c11335ee8b8c4b07eb6a5d9035ae96dad09d7a8203a0` | LF旧`a99df875…`与当前`2186d043…`不同，明确结构语义变化。原历史已应用，原ledger/hash保留；当前base不能当equivalent。 |
| `20260710_dcc_product_catalog_database` | `2f514778cc89d7d78c9f5c7f3a9a1902e8c6d986`；CRLF `2a17bc9ff3845e4ca313c02ca0602471716cc676843a207649c1a0f016f16307` | LF旧`48a76dc2…`与当前`d9be6777…`不同。除来源注释外，INSERT前DDL/租户列守护完全相同；变化是0420→0901产品种子，从181行变186行，行号/字段内容已重排。禁止重导或换ledgerSHA。 |

base语义diff的逐项真实结构核对：

| 后续变化 | 当前库 | 正向文件/处置 |
|---|---|---|
| route `action_type`、unique改为`(category_id,action_type,version_no)` | 列存在、`uk_dcc_category_route_action_version`完整且unique；旧`uk_dcc_category_route_version`不存在 | 对应`20260921_dcc_category_approval_route_action_type`结构已经满足。其历史UPDATE仍要数据核对，不能因无账本重跑。 |
| File `checked_out_by/checked_out_time`与三列checkout索引 | 两列nullable、索引`tenant_id,checked_out_by,checked_out_time`存在 | `20260903_dcc_controlled_file_checkout`的结构已满足；它不在44闭包，也无重跑必要。 |
| assignee snapshot新表 | 原声明列及obligation/obligation_id/user索引存在 | `20260922_dcc_task_assignee_snapshot`已满足；A本次新增round/leader字段另见缺口。 |
| access log reason 255→2000 | 仍`varchar(255) NULL` | **真实未满足**。已有`20260910_dcc_access_log_reason_capacity.sql`做MODIFY 2000，纯扩大容量，无业务DML；新增为本方案显式forward root，不重跑base。SHA `1bc185f1deb8a3f1fc8b0d30d401db8e5cf7e8fabe3210466495bcf488931f1b`。 |
| encrypted download→direct download状态/删除cipher字段 | `download_status varchar(32)`已存在，6个旧encryption/cipher字段不存在；当前nullable，base为NOT NULL | 对应`20260914_dcc_download_record_direct_download_schema`的列改名/删除目标已经满足；不要重跑其DROP。nullable差异不允许猜NULL数据后强改NOT NULL，独立完整性需求另审。 |
| project `batch_record_total_recognition_json` | `LONGTEXT NULL`存在 | 对应`20260902_dcc_project_code_batch_record_total_recognition_json`已满足，无需base重跑。 |

base当前文本还有比库更强/不同的历史声明（如File master/source/name/number/submitter在库可NULL，position_assignment部分字段缺失、project associated_file_count缺失、NAS失败文本等）。CREATE TABLE IF NOT EXISTS不会修这些差异；本轮不能用“重跑base”调整历史架构。44新合同实际依赖的表/列须按本报告限定核对，旧模块其它差异单独评估，不能借新DCC迁移顺便收敛全库。

catalog所有声明列和4个命名索引已满足。当前`ON DUPLICATE KEY UPDATE id=id`会保留同`(data_source,original_row_no)`旧行却只插新增尾行，**不是安全的0901数据刷新**：行号已变化，重跑可混合两套源事实。其任何数据刷新必须另有业务授权、源行身份映射与用户修改保护；本次准备不含产品数据刷新。

## 44项逐文件决定

“已满足”只表示本次查到的结构前置，不冒称该无账本migration历史执行过。“待数据核对”不得由schema日志升级成可执行。

| 文件ID | 归类 | 当前证据与执行准备 |
|---|---|---|
| 20260513_dcc_base_schema | 历史已应用，禁止重放 | 精确历史来源/hash已还原；当前base不同语义。保留ledger，另补reason容量forward。 |
| 20260921_dcc_category_approval_route_action_type | 结构满足，无登记 | 列/新unique存在、旧unique无；UPDATE空action_type需数据证明，先不跑。 |
| 20260922_dcc_three_workflow_bpmn_seed | 待BPM数据核对 | v1固定deployment/model/procdef/info、bytearray UPDATE和作废策略UPDATE；不能盲重放。 |
| 20260922_dcc_task_assignee_snapshot | 结构满足，无登记 | 表/列/索引齐；不重新创建。 |
| 20260930_dcc_a_lifecycle | 新增必需DDL | 2表+10列缺；修改obligation unique加BPM，operator_id改NULL。 |
| 20260923_dcc_three_workflow_candidate_strategy_fix | 待BPM数据核对 | 建v2、暂停v1；既有定义/引用须确认。 |
| 20260926_dcc_three_workflow_matrix_multi_instance_fix | 待BPM数据核对 | 建v3、暂停v2、源XML校验；不当无害seed。 |
| 20260930_dcc_a_workflow_bpmn_v4 | 本轮必需配置，待实际XML证据 | v4 root；要求真实v3 source，需固定target XML/ID/tenant后决定insert或已满足验证。 |
| 20260930_dcc_a_signature_workflow_round | 新增必需DDL | signature `process_instance_id/file_number_snapshot`均缺。 |
| 20260930_dcc_a_future_obsolete_policy | 待策略数据核对 | 两租户ACTIVE作废policy精确前置及future-state目标需查询；只按真正缺目标插。 |
| 20260710_dcc_product_catalog_database | 历史已应用，禁止重放 | 精确来源/hash、DDL已满足，变化只有来源说明/seed；不改ledger/产品。 |
| 20260903_dcc_explicit_data_relation | 结构满足，无登记 | data_relation表/声明列存在。 |
| 20260920_dcc_project_product_create_approval | 结构满足，无登记 | request/relation/identity_claim三表声明列存在。 |
| 20260719_dcc_file_type_taxonomy | 当前hash已登记 | 表和3处leaf列存在；不重放menu/role授权。 |
| 20260910_dcc_project_file_template | 结构满足，无登记 | file_template_item表/声明列存在。 |
| 20260930_dcc_b_project_attributes_folders | 新增必需DDL | 4表+6列缺；历史属性/负责人保持NULL，不猜默认。 |
| 20260917_dcc_controlled_file_name_claim | 结构满足，无登记 | legacy claim表、active_unique生成列/索引存在；不重建旧索引。 |
| 20260906_dcc_new_file_lifecycle_p1 | 部分满足，需索引清理 | 5列/2个新logical identity索引存在，**旧master chain unique仍存在**。核对新索引/列collation后正向drop旧约束，不假称whole迁移执行过。 |
| 20260906_dcc_new_file_lifecycle_p2 | 结构满足，无登记 | checkout全列、ACTIVE生成表达式、4索引及File相关列存在。 |
| 20260906_dcc_new_file_lifecycle_p3 | 结构满足，无登记 | `revision_base_active_controlled_file_id`已nullable bigint。 |
| 20260930_dcc_c_revision_identity | 新增必需DDL | 15列（含3生成列）及source/number/version unique缺；先新约束后撤旧claim-name unique。 |
| 20260815_system_notify_message_business_key | 当前hash已登记 | business_key列存在，不重跑。 |
| 20260903_dcc_controlled_file_related_file | 结构满足，无登记 | historical snapshot全声明列与索引存在；不把历史自动迁成current set。 |
| 20260615_system_config_package_menu | 当前hash已登记 | 不重放menu/package/role更新。 |
| 20260717_bpm_form_center | 当前hash已登记 | 6表声明列存在；menu/package DML不重放。 |
| 20260719_business_approval_policy | 当前hash已登记 | 2表声明列存在；不等于DCC策略值已满足。 |
| 20260719_dcc_upload_form_policy_seed | 待策略数据核对 | 依赖旧`dcc-controlled-file-approval`定义/UPLOAD DRAFT策略，可能已被后续替换；不盲插。 |
| 20260720_dcc_publish_form_policy_seed | 待策略数据核对 | 要求PUBLISHED/BPM_REQUIRED；若后续P4已DIRECT，重跑会冲突。 |
| 20260906_dcc_new_file_lifecycle_p4 | 待策略数据核对 | DISABLE现publish source+插DIRECT，属于真实配置迁移；先确认现行policy。 |
| 20260907_dcc_publication_followup | 结构满足，无登记 | 7个followup/冻结snapshot表齐，无需重建。 |
| 20260907_dcc_publication_impact_assessment | 结构满足，无登记 | task/audit两表齐。 |
| 20260907_dcc_publication_notification | 结构满足，DML暂缓 | 两表齐；脚本UPDATE现template/menu/role/package，不能整文件重跑。 |
| 20260930_dcc_d_notification_template | 待通知模板数据核对 | `dcc_relation_remediation`精确content/params/type/status；存在不同则fail，不能覆盖。 |
| 20260930_dcc_d_relations | 新增必需DDL | 8表全缺；不回填historical relation，不自动引用跟最新。 |
| 20261001_dcc_application_round_link | 新增必需DDL | round_link缺；不生成伪历史BPM binding。 |
| 20261001_dcc_b_manual_project_folder_origin | 新增依赖DDL | 新project_folder创建后，将source template/node允许NULL。 |
| 20261001_dcc_b_project_file_placement | 新增必需DDL | placement表缺；不以NAS ID/目录名猜historical位置。 |
| 20261001_dcc_b_project_product_audit_intent | 新增必需DDL | request四个audit intent列缺。 |
| 20261001_dcc_b_reserved_draft_round | 新增依赖DDL | round_link bpm允许NULL，attributes两source列缺。 |
| 20261002_dcc_c_initial_candidate_identity | 新增依赖DDL | C生成key→288/#INITIAL，只在明确所选INITIAL候选使用，不改version。 |
| 20261003_dcc_approval_file_owner_snapshot | 新增必需DDL | File七个nullable owner冻结列缺；不填历史人员。 |
| 20261003_dcc_controlled_file_activation_job_registration | 待正式配置授权 | handler记录g13查询无匹配；cron/retry/interval/monitor未给，脚本明确拒绝；只插PAUSED，不直接启用Quartz。 |
| 20261003_dcc_project_reviewer_configuration | 新增必需DDL | reviewer config表+request三个nullable冻结字段缺；不seed admin。 |
| 20261003_dcc_revision_failed_attempt_identity | 新增依赖DDL | attempt/predecessor两列、最终key320/#ATTEMPT及predecessor unique缺。 |

合计：7项可证历史已应用（5当前匹配+2精确历史来源），37项未登记。37项中有多项结构已满足，14个本轮root为真实缺结构DDL；其它root是v4 BPM、future作废policy、remediation模板、activation配置4项，必须按实际数据而非账本缺失决定。

## 具体DDL范围与顺序

17缺表：A lifecycle 2张；B attributes/template/history/folder 4张；D reference/current set/change command/arrangement/controlled event/remediation/outbox 8张；`dcc_application_round_link`、`dcc_project_file_placement`、`dcc_project_reviewer_config`各1张。51新增列只计ALTER/column helper新增，不包括新表内部列：A10、signature2、B6、C15、audit4、attributes-source2、owner7、reviewer3、attempt2。全部按正式SQL建，不猜数据。

候选技术结构顺序（**不是授权绕过seed dependsOn的执行包**）：

1. 核对P1新logical identity索引及binary collation，再用正式`20260906_dcc_new_file_lifecycle_p1`处理仍存在的旧`uk_dcc_controlled_file_master_chain`；已存在ADD均应守护no-op。
2. A lifecycle→signature round；B attributes/folders；C revision identity；D relations。在完整依赖方案已冻结的前提下执行，而不是先忽略D的P4策略依赖。
3. application round link→manual folder origin→placement→audit intent→reserved draft round。
4. C initial candidate→failed attempt：`c_version_key`依次256→288→320；先source/number/version unique，旧`uk_dcc_file_name_claim_active`后删除；final `uk_dcc_c_rework_predecessor(tenant_id,master_id,rework_predecessor_controlled_file_id)`。
5. owner snapshot、reviewer config。
6. 新增明确root `20260910_dcc_access_log_reason_capacity`，只扩reason到2000，保留所有已有值。

其中不是纯ADD的动作必须明确列入授权：旧master chain unique撤除、assignee obligation unique替换、claim legacy unique撤除、operator_id nullable、manual folder/BPM nullable、3次生成表达式调整及reason容量扩大。MySQL DDL隐式commit，不能用事务ROLLBACK承诺撤销；备份和恢复方案必须覆盖表结构及已有数据，不能遇部分失败仍标whole成功。

## Root最终申请前应冻结的最小方案

- 使用两个**精确原SHA历史SQL作为已应用依赖证据**，不要把当前不同语义文件登记为acceptedEquivalent。现preflight实现对不同SHA可能生成APPLY，不能直接把现在44 manifest交执行器。应冻结执行包/依赖引用：旧2项保持原ledger、当前forward gap使用各自新ID，旧seed数据不重放；发布/账本处理需Root正式Review。
- 已满足无登记的结构只保留验证证明，不伪造“曾执行”。若执行器不能合法采用结构基线，要先准备单独、可审计的基线验证/采用流程；不得手工UPDATE旧ledger SHA/status绕过。
- 获取受影响表的SHOW CREATE或只读`COLUMN_DEFAULT/COLLATION_NAME/TABLE_COLLATION`：当前日志不含collation/defaults，特别是Master normalized number binary、NameClaim active generated及相关索引。新增unique前做精确重复检查；缺新source/attempt列导致旧键NULL的行为要核对，不回填历史。
- 补真实BPM v1/v2/v3/v4的tenant/key/ID/version/suspension、bytearray XML hash及definition_info；确认v4已有是否精确匹配，版本1/2暂停动作会影响当前定义选择，不改在途历史。补各租户PUBLISHED UPLOAD/PUBLISH/OBSOLETE/future-state policy、两个notification template现值、相关菜单/role/package范围。schema不足以证明它们已满足。
- activation另列批准cron/retry/interval/monitor。SQL注册状态2是暂停；后续用正式job控件及`qrtz_job_details/qrtz_triggers`核对，未获授权不启动/启用。新reviewer/leader/template配置通过正式页面维护，不能数据库写入猜账号。
- 在同库完成带表名精确COUNT和关键历史身份/正文hash/BPM/signature摘要；`g13-runtime-history-counts.log`4个无标签数字不能直接用作执行前后对账，且最后缺source列查询已ERROR，不能写为全项PASS。采样前后无并行writer、有可恢复备份并固定SQLSHA、operation/release标识后才提出数据库修改授权。

## 执行后验证和历史零改写边界

只读schema同合同复跑，17新表/51列及全部最终索引/生成表达式出现，reason2000；新表首次空，旧行新增snapshot/attributes/source/attempt仍NULL；旧Master/current-active/BPM/file.version/name/body/签名/属性不能被自动回填。精确历史COUNT和关键摘要与前值一致。随后分别readback正式BPM/policy/template/config结果，不用“DDL执行完”宣称页面和Quartz可用。

本报告不提供实库已执行结论。可供Root最终提出的数据库scope是：14个真实root schema + P1旧约束清理 + reason容量forward，另4个seed/config root在数据前置及具体值明确后单独审查；旧base/catalog/menu/旧BPMseed/旧policy脚本不盲重放。完整19/45包需Root重新冻结依赖与历史原SHA处理，不能把本候选顺序直接当生产授权。
