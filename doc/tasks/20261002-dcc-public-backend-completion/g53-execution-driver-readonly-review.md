# G53 三迁移执行驱动有限只读审查

Status: ready_for_closeout — 当前读时驱动未执行；只读源审查，不修改驱动/旧工具/生产，不运行DB/API/Maven/服务/Git。用户已授权G50组合三SQL和真实UI验收；本Agent仅核这次具体执行范围，不代Root执行或声明首次重复PASS。

## 结论

当前没有发现确定的执行范围P1。驱动为固定本机source/既有clone、精确3SQL/rawSHA、不执行外部依赖；clone全部first/repeat完成后才进入source，重复无ledger写；原行按旧列集合核，不改历史或伪造旧APPLIED。与G51指出的不宜使用通用全SQL publisher、旧skipledger重写问题已经在本驱动中避开。

两个有限证据细节建议在执行前或终检闭合：mapping engine/id auto_increment/update_time EXTRA精确校验；失败journal明确activeDB/migration/phase/materialSHA与真实退出，以便部分DDL状态可以准确复核。它们是本次具体proof边界，不要求新增发布平台或改正式SQL。

## 精确范围及授权

`g53-user-authorization.json`真实actualUserReply=授权、source/clone/UUID、migrationId三个、maximumLedger3/库、preserveExistingBusinessRowsAndSignatures=true、cloneFirstRepeatBeforeSource=true、remoteOrPushAuthorized=false。driver main核实际答复/3ID顺序/clone条件/上限，目标仍由readonly.DATABASES和ExactReadonlyMysql固定，未将dev免QA扩成远程授权。

MIGRATIONS仅：20261004_dcc_product_identity_source(049d60...a2760)、20261005_dcc_project_folder_storage_mapping(afbdd3...5485a)、20261005_dcc_project_product_application_notify(696bb8...7a810)。material()每phase重读正式SQL rawSHA，材料文件实际byte必须等于唯一compose结果，不用JSON自报MATCH替代。SQL依赖只事实核验，payload只本3新源码，没有oldscope19/wholepublisher自动补依赖。

## 先Clone后Source / Phase身份

- main先run_database(DATABASES[1])，只有完整completed clone状态返回才run_database(DATABASES[0])；每库3migration×first/repeat=6SQL调用，combined成功才公开12实际execution结论。
- run_database每phase执行前先no_writers/no_routines/ExactReadonlyMysql当前DATABASE/UUID/MySQL8.0.40、verify_state精确已完成集合，既存partialnewschema/template/ledger会在第一phase前拒绝，不悄悄补齐/接管。
- SQLmaterial含实际同connection SELECT SESSION_IDENTITY和末尾migration/phase_COMPLETE；raw输出两JSONexact值+exit0后接受。既有固定mysql数据库参数与新鲜当前identity/writer读取共同构成前置，输出env本身是后验receipt，不能声称仅SELECT打印会阻止错误目标写。
- 每phase独占新的stepdir/actual-mysql stdout+stderr文件，旧目录及重复执行receipt拒绝，读时hash与material exactbytes核；单phase后有phase-receipt SHA，完整6步骤无缺失才能completed。

## 原行/Schema/ledger/template边界

- prepare_database新鲜备份before/after证明oldrows/SHOWCREATE不变；verify_state after(snapshot_original_rows)明确传baseline原列集合。product4列新增后仍比较原旧值，不因新NULL列改变hash而误报/允许改历史。
- 21保护表当前覆盖目录、逻辑folder、project、categorybinding、File/Master/placement、nameclaim/ownership/历史sidecars、DCC/unifiedsignature、全部旧migrationledger、全部旧notifytemplate/message及catalog/request/relation/accessrules；所有旧PK原列摘要一致，新增数量只允许3精确ledgerkeys与新template1（该driver起始严格要求templateabsent）。
- 3ledger查询不过滤environment/status/deleted，错误其它环境或错payload同ID会拒；exactnewSHA/file/tag/operation/envtest/statusAPPLIED/tenant0/actor/time/errors核，没有ON DUP UPDATE。repeat material没有ledgerINSERT，再比较beforeLedger全部JSON字段逐值相同，不改SKIPPED时间。
- template校验fullbinary等值payload，repeat比较含ID/created/updated/actor等全部捕获字段；旧其它template hash不变，不只COUNT1。
- product4列必须预absent→四nullable/defaultNULL/type正确，所有旧File新字段保持NULL，无backfill；mapping表预absent→12有序列/type/null/default/fsp6+5索引exact列序/unique/prefixNone、数据0行。原schema indexes/type/order/default/collation字段不变。
- 所有phase前后临时procedure必须absent；product脚本预DROP不会删别的旧routine，因为name存在即停。notify脚本失败可能留下routine，驱动不自动drop后retry；其seed写在正式SQL自己事务内，不当作两个schemaDDL可一起ROLLBACK。

## R01 — 本轮schema proof小缺口（建议补或Root精确终检记录）

`shape():36-37`仅COLUMNS+STATISTICS，没有TABLES.ENGINE，`validate_mapping_shape:77-103`不比较COLUMNS.EXTRA。因此validator可在仍具同列/索引定义但引擎变成MyISAM、id没有AUTO_INCREMENT或update_time缺on update的情况下接受。当前before-new-table-absence+冻结CREATE SQL保证正常执行会生成InnoDB与这些extra，故未发现实际错误SQL或当前scope写bug；但最终schemaVerified=True的精确结构证据不完整。

有限建议：在mapping终检明确InnoDB、id.extra=auto_increment、两个日期default/extra包含formalDEFAULT_GENERATED（按actual8.0.40）以及update_time on update CURRENT_TIMESTAMP(6)，creator/updater utf8 charsetcollation与实际SHOWCREATE对应；不扩全新DDL解析器。原表showCreate可用prepare receipt精确原metadata进一步交叉，不用SQL成功exit代metadata证明。

## R02 — 失败状态定位（建议本次journal绑定）

main except只保存STOPPED_REQUIRES_ACTUAL_STATE_REVIEW/errorType/message，rawSQL失败时stdout/stderr实际文件存在但不会生成phase-receipt/result当前db/phase字段。它没有自动重试，也没有宣称DDLrollback，行为安全；Root复核时仍需从唯一输出子目录定位最后attempt。

有限建议记录active database/migration/phase/materialSHA、是否已进入run_authorized_sql与实际raw退出/输出descriptor；不能把“没有成功phase-receipt”等同0数据库写。若首2DDL完成而后seed或ledgerINSERT失败，保持准确partial状态，clone未完整PASS绝不source；停止后需Root看实际schema而非重复main换dir重试。

## 当前实际证据与Operator约束

- 本审查未运行driver，也没有import其模块以免实际transport意外调用。源级读取上述强制条件正确，不伪造offline fakeMysql或实际MySQL执行PASS。
- 当前driver在进入每phase时逐个核material；建议Root执行前一次性核已准备12文件/manifest rawbytes，避免后phase文件缺失导致可预先阻止的partial。该建议不扩大实际DDL白名单。
- g51-readonly-preparation备份exit/gzip/hash/nonempty/current21原表rowproof存在；不把这变成任意数据库覆盖/恢复授权。驱动不restore clone、不delete/create数据库，不自动copy source结果作为clone状态。
- writer检查前instant0transaction/otherScopedSessions加Root现有任务writer/service排除窗口；不能单次读证明未来不会连接。Root保持排他直到全部first/repeat和原行Proof完成。
- gxp_event/tenant_id-primary-key sequence未在21表范围（原G51推荐扩核）；三SQL不会触它們，至少Root旧六event重要审计rawfacts必须保持独立只读前后收据，不把21表摘要宣传成全数据库所有对象0变更。这里不要求追加新backup平台。
- 真实source升级与Root已核current package/stdin auth/文件权限/前端页面验收仍是后续阶段。3SQL/source successful receipt是升级证明，不直接代表HTML端到端业务完成。

## 读时rawSHA

- doc/tasks/20261001-dcc-integration-unblock/g53-execute-upgrade.py · 15036 bytes · SHA256 `22cad504639ab6f6c99b287388289bc717caa0317d3380e3acf72b4c9bff980a`
- doc/tasks/20261001-dcc-integration-unblock/g53-user-authorization.json · 857 bytes · SHA256 `16e78078f5fcce1ced18386cc38f64aacd34f81d215a7fcede001f1351e0e0c6`
- doc/tasks/20261001-dcc-integration-unblock/g51-readonly-preparation.py · 9296 bytes · SHA256 `74342d01c2fb6ffa9596c8e6461839d7de03c65d7c77291439550418f3a1fc02`
- doc/tasks/20261001-dcc-integration-unblock/g51-prepare-execution-material.py · 4698 bytes · SHA256 `22346d9b1f05fa75746e65dec12c0b9c64db06de621676c53778731feacd931f`
- doc/tasks/20261001-dcc-integration-unblock/g21_mysql_support.py · 7732 bytes · SHA256 `6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331`
- IntRuoyiBackend/sql/mysql/20261004_dcc_product_identity_source.sql · 1576 bytes · SHA256 `049d60e2e8f4a7999ebc213cda77154fa78c315bff296f78be127462977a2760`
- IntRuoyiBackend/sql/mysql/20261005_dcc_project_folder_storage_mapping.sql · 1275 bytes · SHA256 `afbdd3bbe5646dbb1e43f8db423e874cace07e4668bf578512f03c3009a5485a`
- IntRuoyiBackend/sql/mysql/20261005_dcc_project_product_application_notify.sql · 2106 bytes · SHA256 `696bb80f60509ea453fe8c8fda07b42f8020e09415aaf2d2ed9b388ed907a810`
