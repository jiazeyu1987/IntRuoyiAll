# LD03 / G48 只选择项目文件夹

Status: ready_for_closeout — LD03 software frozen, actual schema/runtime remains Root-owned. 唯一后端/Java/Maven owner；Root独占真实数据库、运行/E2E、Git，FE由另一owner。本批不改LD04/System/product审批。

采用已批准g48-single-project-folder-plan.md：正式tenant/projectFolder/category→唯一配置base→新DBleaf映射，不能取第一叶子/近似名称/NAS mkdir。当前Root实际category908710/base908991皆tenant1启用、绑定唯一；不借tenant0。

- Given项目和逻辑文件夹合法、类别唯一启用base为非叶子，When NEW首次创建且无客户端directory，Then同事务插精确DBleaf/mapping/File/placement/Gxp，后续同key只复用原mapping，不改历史。
- Given客户端提供directory、根配置缺失/歧义/foreign/变动，When normalNEW，Then拒绝不猜；票据/产品/type/权限/日期守卫不放宽。
- Given晚期File/placement/Gxp失败或删除/重配映射占用根和逻辑folder，When同正式事务处理，Then零新增残留/共用锁保护，不回填旧资料、不移动正文/NAS、不自动grant权限。
- Given历史升版/检入原位置，Wheninherit，Then只依原版明确记录，不用当前新mapping重新定位。

最小内部context由Service产生传Workflow，HTTP目录字段对正常NEW不可用；EXTERNAL保原合同。New表和H2/forwardSQL只结构，无实际DDL。当前stage不扩大Retention、QA或其它业务。

## 最终结果

有效RED r3为真实H2项目/folder/唯一base/publiccallback应先有mapping但实0，1assertfail0errors；首green-r3真实1case通过。5最终H2动作证明精确复用、lateGxp真实失败回滚、caller目录拒/配置根冲突、映射占用+当前base撤权/manualleafoverride、锁后category active/type正确读取。Workflow170/HTTP2/目录20/类别17/权限5组合最终219/6全0，2026-10-05 01:17:58，所有工具/fixture失败日志独立保留不冒业务RED。5方法仅选newG48，不运行继承parent全部场景；重复前批不累加。

生产17、测试6、H2/schema4共27资产rawSHA和6永久XML已归档；见g48-storage-fingerprints、verification-receipt、junit-archive。Source正常NEW入口均走正式PublicPlacement；不能有callerNAS第二轨。新mapping/leaf/File/placement/Gxp同物理事务，旧EXTERNAL/history继承不改；无新角色或ACL行全员授权，未手动配置leaf才动态base投影。根/叶/文件夹和categoryconfig占用真实FORUPDATE保护，声明SQL/H2证据，未假称MySQL并发全域证明。

SQL只新1表12列与索引，fullclosure9PASS（旧8factsonly，不重放），实际未执行；Root具体备份/隔离首次重复/授权方案见g48-storage-runtime-plan。原Stage1产品/Stage2自由上传约束保持，未触系统或Stage4审批/待办/FE。

本Agent无actualDB/Redis/token/service/package/Git/browser/E2E。Maven已停止、6targetXML先永久复制并通知XML_ARCHIVED_FREE_MAVEN后第四BE接手；此后只封存资料不改源码/不重测。
