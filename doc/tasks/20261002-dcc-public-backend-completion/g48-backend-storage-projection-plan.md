# LD03 后端实施合同

Status: ready_for_closeout — LD03 27源资产与219/6已冻结，Runtime方案另见g48-storage-runtime-plan。

唯一新结构 dcc_project_folder_storage_mapping：当前tenant/projectFolder/category唯一，保存project/base/leaf真实ID，leaf唯一。当前项目/逻辑folder锁后内部MANDATORY Service校验唯一类别/UPLOAD权限，锁category与准确全集唯一base，锁base再mapping/leaf；只直接插数据库叶子，不mkdir或授新角色/规则，不近似匹配旧目录。

正常NEW HTTP directoryId字段省略，提供值或显式null拒绝。public submit/create-working 与 Controller都走同一个PublicPlacement编排；其Function回调传不可由REST构建的DccDerivedUploadStorage。Workflow内部方法按context读base/leaf，DTO字段不作权限凭据；EXTERNAL/已有升版检入明确位置保持。context权限按当前用户/tenant/project/folder/category核，正式票据/名称/类型/产品/时间不变。

首次leaf/mapping/File/placement/audit同物理事务，原请求目录保持null。重复同key只精确复用；root变动/异常当前mapping拒绝。已有位置只源版本固定storageId，不以新mapping填历史。

维护有限闭环：category重绑与新mapping同category锁且占用拒；folder删除同project/folder锁且mapping占用拒；存储root/leaf/含它们子树删除及结构停用修改先锁准确目录，再核mapping占用，不能级联删已映射位置。只此source新增内部关系的管理守卫，不重新设计所有旧目录。

本批派生叶子的读取/下载权限只可精准动态投影其正式base当前规则：当前tenant有效mapping、project/folder/category/base/leaf有效、parent/base精确一致且根配置仍唯一一致。base撤权马上撤投影；无通用ancestor授权、无新增全员rule，File/项目/类别/assignment原额外门禁仍保留。

新SQL 20261005_dcc_project_folder_storage_mapping只新表/索引，无旧DML；ROOT负责新schema具体备份/隔离首次重复/源库授权和MySQL实际执行。LD01四列仍独立新迁移，不串旧依赖重放/账本改写。软件测试只H2与隔离读取端口，不冒真实文件/NAS或E2E。
