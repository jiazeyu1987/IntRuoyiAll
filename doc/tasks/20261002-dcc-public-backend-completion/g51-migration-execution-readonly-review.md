# G51 三项本机升级执行只读审查

Status: ready_for_closeout — 仅执行方案/源码只读审查。本Agent未修改生产/旧工具、运行SQL/DB/API/Maven/Git、启动服务或真实E2E。3SQL当前执行授权仍未收到答复，本报告不是执行授权/实际首次重复PASS。

## 结论与推荐范围

G50业务写边界合理：本机既有int-ruoyi-mysql，source ruoyi-vue-pro与已存在dcc_intqms_g18_rehearsal，固定serverUUID 92ca05d0-aec8-11f1-a944-02b4e226a5ef；只3新SQL，最多3新迁移键/库，原数据不填不改；源码已完成不等于runtime升级已执行。

执行推荐为Root有限task-specific3白名单编排，复用已冻结LocalMysql传输与旧行摘要函数，**不要直接调用通用publish/deploy整个包或g21的19脚本scope**。完整dependency policy闭包用于只读核验，实际SQL列表只有以下三个，源文件rawbytes/SHA必须匹配G50：

| 迁移ID | rawSHA | 实际允许变化 |
| --- | --- | --- |
| 20261004_dcc_product_identity_source | 049d60e2e8f4a7999ebc213cda77154fa78c315bff296f78be127462977a2760 | dcc_controlled_file追加4 nullable字段，旧字段/值不动 |
| 20261005_dcc_project_folder_storage_mapping | afbdd3bbe5646dbb1e43f8db423e874cace07e4668bf578512f03c3009a5485a | 新空mapping表12列/PK+2unique+2secondary |
| 20261005_dcc_project_product_application_notify | 696bb80f60509ea453fe8c8fda07b42f8020e09415aaf2d2ed9b388ed907a810 | 缺失时仅1新template；已有exactMATCH时0新增，冲突拒绝 |

推荐明确顺序如上（依赖的旧对象分别先只读核验）。不将依赖缺ledger推成需重放旧SQL；真实旧ledger若存在，保原payload/hash/status/tag/times。缺ledger但对应事实已满足时，记录独立externalDependencyFacts及来源/SQL定义/当前schema raw证据，不能INSERT假APPLIED或修改旧SHA来喂通用preflight。

## 已确认的通用执行器不适用风险

1. `script/deploy/publish-int-ruoyi.ps1#Get-ReleaseDatabaseSqlScripts:4174-4226` 默认扫描SQL roots日期脚本，没有此次单3 migration CLI白名单；还含SSH/部署操作。`-SkipDatabaseSync`不等于跳过required SQL：schema/seed仍在required preflight体系。不能把它当本机仅3脚本执行命令。
2. `script/release/release_preflight_plan.py#build_preflight_plan:125-129` 当前APPLIED但SHA不同的已知migration会转为APPLY/reapply，并非BLOCKED_CHECKSUM。因此若把完整closure放进执行manifest，历史文件版本差异可能重放DDL/seed。必须在Root当前scope层拒任何非三白名单APPLY；不靠普通preflightpassed当范围证明。
3. `publish-int-ruoyi.ps1#Invoke-RequiredDatabaseSqlScripts:4937-4942` 对SKIP_ALREADY_APPLIED也写state；`Invoke-ReleaseMigrationStateUpdate:4717` ON DUPLICATE KEY UPDATE会改status/file/SHA/releaseTag/finished/error/operation/updater/time。与G50“全部旧ledger不改、repeat不覆盖”冲突。
4. `release_deploy_executor.py:23-65` 自身不自动扩closure，只遍历传入plan；但skip仍调用record_state(SKIPPED_ALREADY_APPLIED)，APPLY也调用RUNNING/APPLIED/FAILED。可复用其明确失败传播思路，不能直接照写callback去改旧记录。只有三**本次新键**可按真实执行状态转RUNNING→APPLIED/FAILED；repeat遇匹配APPLIED必须只读验证/私有journal，不写SKIPPED状态或更新时间。

这些是执行路径/范围适配问题，不是3正式SQL或已审业务源码失败；本review不改全局publisher/preflight行为。

## 现有任务工具可复用与限制

- `g21_mysql_support.LocalMysql(database)`只允许两个既有库，Docker凭据只在容器MYSQL_PWD env内，命令无--force；run_authorized_sql写新stem独占stdout/stderr，已存在receipt拒绝。可用于Root获准之后的固定target传输，**函数名字不自动证明已经获得授权**。
- `.read`仅粗分号拆分并检查SELECT/SHOW头，不是任意SQL安全沙箱；本次只传Root预先封存的查询模板，不能开放caller任意SELECT（例如OUTFILE/LOAD_FILE/GET_LOCK等副作用函数）。不用修改旧冻结helper。
- `.run_authorized_sql`只返回database/SQLSHA/exit/raw输出，不校验actualUUID/列/ledger，也不自建账本。Root每个step前需新鲜actualsamecontainer/database/UUID/timezone/sqlmode/write-exclusion核验；执行material/output记录同session身份和准确SQLSHA，不把打印env当自动阻止错库的守卫。固定mysql database参数+新鲜UUID/prewrite排他与私有真实receipt都必须成立。
- `snapshot_original_rows(...existing_columns)`可以复用： before捕获原有列序，after必须传**before原列集合**，否则4新列会使同一旧行hash变化而误判历史DML。列值NULL/HEX和PK/JSON排序可逐row比较；`compare_original_rows`要求exact新增数，而不是<=上限。
- 它仅支持id/ID_主键；正式`gxp_audit_ledger_sequence` PK是tenant_id（DO/TableSQL均证明）。不能直接加入它就声称工具已覆盖，需Root有限单表tenant_id原始ordered读/摘要对比（或任务专属collector明确真实PK），不用扩通用平台。
- `g21_migration_scope.py:17-33/78-91/123-150` 固定19ID、16旧表/17新表、45package等；它不是3脚本参数化driver。`g18-readonly-backup.py`固定g18scope/备份路径与old19affected名单，顶层即parse/执行；不能import执行它或覆写g18receipt当本次新鲜备份。本次另建Root专属备份目录/receipt，旧工具仅阅读/封存其rawSHA，合适函数只读复用。
- g18备份有exit/gzip/nonempty/hash，但没有自动证明当前3scope、actualsameconnectionUUID与所有必要数据覆盖。schema/data在分开的dump命令中，排除写者是必须条件，single-transaction不保护并发DDL或两次dump之间的变化。仅gzip PASS不能替代完整CREATE/数据表marker/实际rowcount/restorability与新scope来源证明。

## 执行前必须冻结

| 范围 | exactmetadata/原值证明 |
| --- | --- |
| source/clone/session | 实际DATABASE、serverUUID、MySQLversion、sqlmode、sessiontimezone、container/端口、当前每个库结构与本轮预像（不能clone抄source事实） |
| dcc_controlled_file | 全部旧列ordinal/type/null/default/fsp/charset/collation/generated，SHOWCREATE与所有index列序/unique/subpart；原所有PK→原列hash，覆盖全部tenant。4新增列应初始全absent，partial/wrongshape先停止 |
| mapping table | 起始不存在；若已存在须12列/5index/engine精确+无未知业务行，不能IF NOT EXISTS silentlyskip错误表 |
| system_notify_template | 全部原列/PK逐row摘要和code唯一index/collation；目标code用原SQL同样的全局existing_count+binary完整payload判MATCH/ABSENT/CONFLICT，不只看COUNT1或code |
| 旧运行关系/历史 | dcc_file_directory、category_directory_binding、project_code、project_folder、product_catalog、project_product_create_request/relation、controlled_file_master、project_file_placement、controlled_file_name_claim、legacy_scope/evidence/reservation、DCC/unified signature、gxp_event/sequence、notify_message及原ledger；实际存在table/真实PK先核。不新增业务数据/ACL/权限/签名/QA登记 |
| infra_release_migration | 全表原row/PK原列摘要；3目标ID查所有environment/状态/deleted，不能WHEREtest/APPLIED过滤遮住错行；原env+migration唯一索引及operation/release/tag/times/hash精确 |
| 临时procedure | information_schema.ROUTINES/PARAMETERS与实际SHOWCREATE：dcc_product_identity_source、g49_dcc_project_notify应初始absent。前者SQL预DROP IF EXISTS会删同名旧routine，故发现任何不明现存对象先停；后者不predrop，partial失败留下procedure也不能自动删后重跑 |

保持写者排除直到所有pre/post完成：既有DDL没有保护全业务writer。只停已获准、已归属的任务服务；若另一服务/Quartz/数据库event/外部连接仍会写保护表且无可控排除，不擅自停它或以单次trx=0假持续排他，明确阻止实际执行。

## Root推荐执行参数（仅方案，未调用）

- backend/SQLRoot固定C:/IntRuoyiAll-int_main/IntRuoyiBackend/sql/mysql；3精确migrationID列表+上述rawSHA；externalClosure单独只读，writeWhitelist长度=3且不重复，stage=CLONE_FIRST/CLONE_REPEAT/SOURCE_FIRST/SOURCE_REPEAT，不按全目录glob执行。
- `LocalMysql.database`仅dcc_intqms_g18_rehearsal或ruoyi-vue-pro；同固定serverUUID/targetEnvironment=test。每个库/step新的task-private_directory和唯一stem，不覆盖/复用失败receipt，不打印原SQL业务参数/凭据。
- 原列后检参数显式`existing_columns={table:before[table]['columns']}`。首次permitted_additions仅模板1(若此前ABSENT)和ledger新增实际三键数；重复全部0。ledger/template新增仍另核完整载荷/ID，不因helper接受新增数量就证明正确。
- 新APPLIED ledger内容必须真实migration/file/rawSHA/environment/releaseTag/operationId/started/finished+exit证明；禁止宽ON DUP UPDATE改旧行，禁止checksum等价替换。RUNNING/FAILED既存新键属具体失败/恢复流程，不能elapsed-time或删row自动retry。

现有演练库可先只读判断是否具备实际旧依赖与缺新scope。G50授权文本没有扩大为重新DROP/restore全部schema；若旧clone不适合，不自动清库/复制source进clone。备份恢复办法是失败时另行Root concrete授权，不将“备份存在”当可覆盖任何库的常驻授权。

## 首次/重复实际验证边界

1. Clone第一轮各script真实exit0+私有完整输出+SourceSHA/session，3新状态与newledger，所有旧原列/row/index/routine保护；失败立即停止，不把DDL事务ROLLBACK当恢复。schema1/2已提交而template3失败时必须记录partial已发生，不宣称zero write。
2. 四列exact VARCHAR32/BIGINTx3 NULL/defaultNULL/无业务DEFAULT，最终原行4新字段全NULL，不回填；旧column metadata/index和oldrow原列hash全部一致。
3. mapping表engineInnoDB、12有序列、2microsecond日期+CURRENT_TIMESTAMP6、PK+2unique+2secondary、index无截短，表仍0行。迁移阶段不能为了H2/界面创建映射leaf/项目业务动作。
4. template有且唯一exactbinarypayload，旧template原row全不变；首次ABSENT→仅目标1新增，原有exactMATCH→0新增。重复rowID+所有字段包括create/update时间不变；不要insertmock站内消息。
5. Repeat不仅script不报错：oldhistory+新template/ledger3键都按第一轮结果精确不变，procedure执行完不存在，没有newledger/SKIPPED改时间；无法通过partial/wrongshape/冲突时明确FAIL。
6. Clone真验证通过后，Source按自身新的备份、schema、preimage、writer、identity重新核，不复用clone自报PASS当source准备完成；source实际同样first/repeat证据。代码包与真正运行datasource复核、真实UI业务验收是后续独立步骤，所有业务写必须Playwright真实页面，API/DB只读核验。

离线static/闭包/源码包219或102/43等GREEN不能证明真实MySQL语法/fsp/index/DDL隐式提交/实际rollback或E2E。本报告只提供可执行前置审查与已发现路径风险，Root当前3SQL未授权仍不得执行。

## 读时rawSHA

- doc/tasks/20261001-dcc-integration-unblock/g50-four-direction-local-runtime-upgrade-plan.md · 3782 bytes · SHA256 `620bf7084e59c283a7f163691e703f1dea7af063c401b3d8de083c7322fa46af`
- doc/tasks/20261001-dcc-integration-unblock/g21_mysql_support.py · 7732 bytes · SHA256 `6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331`
- doc/tasks/20261001-dcc-integration-unblock/g21_migration_scope.py · 9702 bytes · SHA256 `5fe9fe0bdcadec348a0f4ba3e4b0aab56efdc9df1b2f2fb6792b5382b754430d`
- doc/tasks/20261001-dcc-integration-unblock/g18-readonly-backup.py · 3570 bytes · SHA256 `a76c13c49d11b18060b1a18d9a22ef8dbadbdb59677a30fbfd17931e0a4ea9fe`
- IntRuoyiBackend/script/release/release_deploy_executor.py · 2428 bytes · SHA256 `8b3bb4e59a34531f0362bded1cdf0f71d36a9ee6bc0f5e0543125620b135bb3c`
- IntRuoyiBackend/script/release/release_preflight_plan.py · 11764 bytes · SHA256 `bea4af16614cc31d193328b0ab3022432c7ea68ddc1e7fbe3dc5a043302ed2a8`
- IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1 · 262575 bytes · SHA256 `2b7a8dfb51a16e2592dfbd9a8b6aaa7647d53d59c9c3b1967b2432e9009bc632`
- IntRuoyiBackend/sql/mysql/20261004_dcc_product_identity_source.sql · 1576 bytes · SHA256 `049d60e2e8f4a7999ebc213cda77154fa78c315bff296f78be127462977a2760`
- IntRuoyiBackend/sql/mysql/20261005_dcc_project_folder_storage_mapping.sql · 1275 bytes · SHA256 `afbdd3bbe5646dbb1e43f8db423e874cace07e4668bf578512f03c3009a5485a`
- IntRuoyiBackend/sql/mysql/20261005_dcc_project_product_application_notify.sql · 2106 bytes · SHA256 `696bb80f60509ea453fe8c8fda07b42f8020e09415aaf2d2ed9b388ed907a810`
