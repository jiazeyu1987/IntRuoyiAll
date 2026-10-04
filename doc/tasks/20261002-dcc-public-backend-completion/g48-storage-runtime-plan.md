# LD03 一张映射表升级与真实验收

Status: prepared_not_executed. 后端27资产已冻结，219/6相关验证通过；Root独占实际库、备份、部署、Git及真实UI验收。

本阶段唯一新SQL：20261005_dcc_project_folder_storage_mapping.sql，rawSHA afbdd3bbe5646dbb1e43f8db423e874cace07e4668bf578512f03c3009a5485a。新表12列，2 UNIQUE+2普通索引+PK，两个DATETIME(6)与CURRENT_TIMESTAMP(6)精度一致。不ALTER旧表、不写旧目录/文件/项目/位置/版本/签名/六条审计，不mkdir NAS或配置远程ACL，不新增审计operation/批准节点。

完整release policy闭包9项通过。执行白名单只该新迁移，其余8项只核现有结构/正式历史来源，不能重放旧seed/改旧ledger SHA。LD01四nullable列为另一独立新迁移，两个执行范围不得被此闭包隐式扩大。

Root执行前：核实际sourceDB/UUID、MySQL版本/SQL mode/currentprofile、新表absent；若同表已存在partial/wrongshape或同migrationledger错SHA/environment/status立即停。新鲜只读schema+data备份与实际exit/hash/完整table/rowproof；冻结dcc_file_directory、dcc_project_folder、dcc_project_code、dcc_category_directory_binding、File/Master/placement/signatures及原ledger精确原列/PK逐行摘要，尤其旧目录身份与租户保持；不要用COUNT代替不改旧行证明。

隔离演练：只选Root明确已有的任务专属副本，执行此一表首次/重复，复核精确12列ordinal/type/null/default/fsp与5索引列序，表empty、新ledger最多1 exactpayload，原行/元数据/ledger不变。MySQL DDL隐式提交，失败保真实phase/SQL/exit/元数据，不假ROLLBACK/自动重试/恢复。通过后按Root具体授权执行本机source同一新迁移；本Agent无DDL授权执行动作。

Root实际配置已确认tenant1类别908710→唯一base908991，binding/root均启用未删，不用tenant0。通过真实UI创建本任务新项目/产品、选择其逻辑项目文件夹与正式类型并上传，正常NEW payload省略directoryId；后端事务自动建立一个DB叶子与映射，File与placement采用该叶子，audit包含mapping/base/leaf/created事实。再同folder/type新文件，精确复用mapping，不选第一个既有叶子。客户端目录值/显式null拒绝，不以隐藏前端字段代后端校验。

正常公开NEW service/Controller都走同编排；Workflow只用内部不可REST构建context。旧EXTERNAL及升版检入明确版本位置继承保留，不回填缺历史位置。后端现有配置根更改、映射目录/逻辑folder删除共用实际锁并占用拒，派生目录动态权限只投影当前正式base，manualBound叶子停止投影，base撤权同步，原额外File/类别/project/assignment门禁不变。

H2事务/Mapper与HTTP合同已通过不等于真实MySQL、正文、NAS或E2E。后端scope冻结后仍需Root独立Review、实际迁移、打包部署和真实页面验收；第四阶段开发可独立继续，不等待本Agent额外测试。
