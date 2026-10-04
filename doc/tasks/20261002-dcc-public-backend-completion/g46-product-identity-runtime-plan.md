# LD01 四列前向升级准备

Status: prepared_not_executed. Root 负责实际库/备份/部署和授权；此 Agent 只完成离线软件与 SQL 合同准备。

唯一新迁移：IntRuoyiBackend/sql/mysql/20261004_dcc_product_identity_source.sql。只向 dcc_controlled_file 添加四个 nullable 无默认业务值字段：product_source VARCHAR(32)、product_catalog_id BIGINT、product_relation_id BIGINT、product_create_request_id BIGINT。不新增产品表，不改变 product_master_id 语义，不写旧产品目录/项目/文件/申请/签名/六条历史审计。

正式 migration policy full closure 9 项已准备通过；执行白名单仅此新迁移，另外八项核现有事实，禁止重放历史初始化/种子或改旧 ledger checksum。孤立选新文件首次 policy invocation 缺依赖失败已保留，最终 explicit closure 保持真实依赖。

Root 执行前冻结：实际目标 source database/UUID、schema/server version/SQL mode、四列全部 absent（partial/wrong shape 先停）、实际 current approved source/SQL raw SHA；冻结受影响原表精确全部原列（ordinal/type/null/default/generated/index）及全部旧行 PK+原列字节摘要，不用 COUNT 代替。新迁移同ID ledger若存在须 exact SQL SHA/status/environment/operationId；不改旧记录。

备份：按现有正式Root流程新鲜只读备份 dcc_controlled_file schema+data 及精确infra_release_migration范围，带真实same-source identity/完成exit/bytes/hash/可恢复性与原行摘要；排除并发写者。只选已有任务隔离副本，禁止本脚本自动建/删/恢复库。

授权范围可具体Review：隔离副本首次/重复此一个迁移，成功后本机测试库同一迁移、最多一条新APPLIED ledger；部署当前 reviewed package。授权不扩到业务数据回填、历史tenant0 catalog repair、质量批准、旧依赖或其它模块。实际授权由 Root 向用户或按已有当轮范围核实，本 Agent 不执行或提问。

首次/重复：MySQL information_schema.COLUMNS procedure逐列守护，不使用不支持ADD COLUMN IF NOT EXISTS；重复应无新增列/旧行/旧ledger变化。MySQL DDL隐式提交，部分失败立即保留真实phase/SQL/exit/schema，不伪装事务rollback、不自动retry。Root既有严格driver若不支持本迁移须单独有限方案，不能假装现有sidecar driver可直接执行。

Postflight：四列精确ordinal新增位置、type/null/default；旧全部列metadata/索引/generated不变，旧行旧列逐PK摘要一致；原四新字段全NULL（没有backfill），仅允许newmigrationledger精确一条且重复不变。当前代码会让新批准产品目录写当前tenant，历史tenant0目录不认作当前tenant产品。

软件与UI验收：先升级后部署包；Root真实UI新建→审核→批准新项目产品（全新任务自有数据）→project-product返回DCC_CATALOG三个真实目录/关系/申请ID且MDM ID null→选择项目文件夹类型→上传非14位业务产品编码→正常会签。MDM绑定项目仍严格走MDM；失效MDM不转目录。旧项目270/catalog613保留原事实，无直接修旧业务行。不能把Mock/H2或SQL合同当真实UI PASS。
