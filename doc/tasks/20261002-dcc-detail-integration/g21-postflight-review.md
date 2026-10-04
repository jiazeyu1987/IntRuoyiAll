# G21 19候选最终schema postflight准备

2026-10-03；当前只准备工具。主任务的17项fresh prerequisite validator已PASS，成果保留。本次没有连接数据库、执行迁移、运行服务/Maven/type/build/Git，未修改任何生产/正式SQL/Root文件。`executionAuthorized=false`不因offline test转为授权。

## 给演练driver的固定入口

- `g21-postflight-environment-query.sql`：**迁移前**执行1条只读SELECT并冻结JSONL，包含database/serverUuid/databaseCharset/databaseCollation/utf8mb4DefaultCollation/defaultStorageEngine。迁移连接使用同样的database charset/defaults；不猜MySQL默认值。
- `g21-postflight-schema-queries.sql`：首跑完成及重跑完成各执行同样5条只读SELECT，按`--batch --raw --skip-column-names --default-character-set=utf8mb4`保存UTF-8 JSONL。schema采集是完整26受影响表，不是只读新增列名字。
- `g21-postflight-schema-contract.json`：从主`g18-approved-scope-candidate.json`19文件及各自SHA推导，17新表、51nullable新增列；保留保护dump内9个旧表未改结构，并施加正式DDL最终变化。
- `g21-postflight-schema.py`：离线CLI，可import `validate(contract,facts,environment,previous=None)`；依赖同任务的`g21_structure_prerequisites.py`公共只读解析函数，其SHA也在交付receipt固定。driver必须校验contract和该依赖，不能只校验主validator文件。
- `test_g21_postflight_schema.py`：12项正/负unit；`g21-postflight-preparation-receipt.json`固定交付路径/SHA及readyForDriver标识。

从整合树调用：

```powershell
python doc/tasks/20261002-dcc-detail-integration/g21-postflight-schema.py validate --facts <first-schema.jsonl> --environment <frozen-before-migration-environment.jsonl> --result <first-result.json>
python doc/tasks/20261002-dcc-detail-integration/g21-postflight-schema.py validate --facts <repeat-schema.jsonl> --environment <same-frozen-environment.jsonl> --previous <first-result.json> --result <repeat-result.json>
```

Root/driver用实际路径代替尖括号。成功exit0/status=`POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE`；失败exit1/status=`FAIL`，result含errors。首跑结果包含`schemaFingerprint/contractFingerprint/environmentFingerprint/database/serverUuid`。重跑必须三个fingerprint均与首跑PASS一致；重复result不可替换为别库或未知first结果。

输入没有包络数组，只有逐行JSON对象。environment文件精确1对象`kind=environment`；schema文件依次runtime/table/column/index/ledger多对象。stdout若夹错误/警告/重复runtime/重复column/错index seq/非JSON，reader或validator失败；stderr/exit由driver独立保留，不能过滤错误后宣称PASS。

允许database仅`ruoyi-vue-pro`与`dcc_intqms_g18_rehearsal`，并与冻结environment相同。serverUuid也必须一致。默认engine必须InnoDB，charset必须utf8mb4；具体database与utf8mb4 default collation由实际environment解析。implicit table collation/engine的TODO只剩迁移前真实采集，其他宽度/default/nullable/index/expression均已静态明确。

## 精确目标与禁止假PASS

26受影响表（17新+9原表）、463最终列、85索引形态。列type、nullable、charset/collation、default、auto_increment、stored/virtual、on_update及generated AST逐项匹配。受影响表中额外未知列/索引也FAIL；完整原结构来自SHA已核实的protected dump，不是用本轮快照伪造新目标。

17新表：A lifecycle_event/obsolete_archive；B attributes/folder_template/history/project_folder；D reference/current_relation/current_relation_set/change_command/arrangement/controlled_event/remediation_task/outbox；application_round_link、placement、reviewer_config。全部按正式SQLtype/default/unique声明。匿名UNIQUE有11项，SQL未显式名字，合同按exact ordered shape唯一匹配，不猜MySQL自动生成名字；首跑/重跑完整schema fingerprint仍记录实际名字，因此名字变化会拒绝重跑一致性。

51新增列全部nullable（含3个生成列）；不以expr内部`IS NOT NULL`误解析成NOT NULL声明。owner7列、reviewer3冻结列、audit4列、attributes source2列、A10列、signature2列、C15列、attempt2列均精确。

核心最终断言：

- access_log.reason是varchar(2000) nullable，保留原字符集/collation/default。
- P1旧Master chain unique不存在，新`uk_dcc_new_logical_file_identity`与lookup的exact四列顺序保留；binary normalized_number及ascii revision_code不失去正式collation。
- A obligation unique是`tenant_id,controlled_file_id,process_instance_id,stage_code,department_id,deleted`；旧obligation_id/user索引保留；obsolete_audit.operator_id允许NULL。
- C NameClaim source_original_name/number binary生成键宽1024/512为STORED、generated指向准确字段；旧`uk_dcc_file_name_claim_active`无；新source unique及number unique列序/无prefix精确。
- File最终`c_version_key VARBINARY(320)`为STORED且nullable，AST明确保留INITIAL+selectedIteration条件和attempt>1后缀；仅字符串包含两个字段不能PASS。`uk_dcc_c_version(tenant_id,master_id,c_version_key)`和rework_predecessor unique存在。
- manual folder source_template_id/source_node_key允许NULL，unique维度仍正确；round bpm_round type64、bin collation、nullable，BPM和attribute-number两个unique都保留；placement唯一tenant/fileVersion；owner/reviewer不得新默认指定账号。

generated表达式使用真正递归AST：规范化冗余括号、反引号、MySQL charset字符串introducer、`CONVERT(x USING binary)`和`CAST(x AS binary)`等价；保留CASE结构、AND/IS NOT NULL、比较阈值、函数嵌套和字面值。`CONVERT(CONCAT(a,b) USING binary)`与`CONCAT(CONVERT(a USING binary),b)`不是同AST，拒绝；错误条件/列/后缀阈值明确FAIL。

历史6条原ledger也只读复核不变，尤其base/catalog仍旧SHA；19新执行ledger由演练driver独立核对，本schema工具不伪造APPLIED。4个seed/config候选（v4/future obsolete/remediation template/activation）没有结构DDL，其行值、BPM XML、policy/config成功由其它Owner/driver合同验证，不能由schema PASS替代。

## 已执行的离线证据

12项postflight unit PASS（最初缺工具有效RED9→GREEN）：完整范围/51nullable、错误type288替代320、owner/operator/manual nullability、source charset、round collation、default/virtual、缺table、旧constraints残留、unique顺序/nonunique/prefix/round/placement缺key、错误generated语义/函数结构、环境/UUID缺失、schema首跑重跑差异、JSONL roundtrip与错误行、reason容量、原ledger/额外列保护。

13项既有prerequisite validator unit回归PASS，旧工具没有被改写。SQL SHA19/19与Root候选一致，contract以最终叠加结果生成，P1旧key撤除与A drop/readd均显式处理；先C256、INITIAL288、failed-attempt320，目标只有最终320，非中间状态PASS。

当前只有`prepared_exact_postflight_contract/readyForDriver=true`，没有真实升级首跑/重跑schema证明。Root未来取得DB授权执行并采集后，本工具可证明schema最终形态；不能单独证明17新表数据为空、原历史零DML、迁移ledger19、seed/config/Quartz或真实页面成功，driver需分别核对。
