# 本机 DCC 运行库升级与真实验收方案

状态：prepared_awaiting_database_write_approval。对象仅本机既有 int-ruoyi-mysql / ruoyi-vue-pro（23306）和整合 worktree int_qms slot6（8067/48067）。Docker/MySQL/Redis 恢复已授权并完成；迁移、隔离库恢复、任务启用尚未执行，待用户批准具体方案。无远端操作。

## 已核实范围

- 当前库已有 41,615 文件、36,685 Master、339 DCC 签名；历史行不能通过回填新字段改写。
- 正式包已扩为 19 根迁移、45 依赖，policy gate 已通过。真正执行候选是19 SQL，并非重放依赖闭包。已核实17缺表、51缺新增列，以及reason容量、旧索引替换和新生成表达式。defaults/collation/generated/index 与 BPM/policy/template 行已采集。
- 两项旧账本 SHA256 已找到精确历史 Git 原文件，CRLF 重建与账本相同。当前 SQL 含语义变化，旧 base/catalog 不重跑、不改账本、不重新导入产品目录。
- 已存在的旧生命周期、名称 claim、目录/BPM/FormCenter 表按结构和数据核对；缺 ledger 不表示未执行。种子/配置必须核实实际行后确定。

## 执行顺序与保护

1. 从已有容器读取真实连接与 DCC/BPM/GxP/配置 schema 和数据，只记录脱敏事实；核对现有 V1–V4 process/model/bytearray、ACTIVE/待生效作废 policy、通知模板和实际 job。冻结真正待执行项、原始 SQL SHA256、依赖满足证据与 affected tables。
2. 对精确 affected tables 和完整 schema 做一致性备份，文件置于仓库之外的本机任务备份目录，SHA256/字节数/gzip 完整性校验；凭据不输出、不提交。该备份用于本轮迁移回滚，不宣称附件灾备或长期法规归档。
3. 获准后在同一 MySQL 容器创建仅本任务隔离演练库，以真实备份恢复 schema/相关数据。执行正式前向迁移首次和重复执行，验证生成列、索引容量/顺序、唯一性、原历史业务值/行数/签名/BPM hash 未改变。演练失败立即停止，不修改原库。
4. 演练通过才对原本机库执行已冻结的同一清单。新增列保留历史 NULL，不补造名称/人选/申请轮次，保留历史 BPM 和签名。旧 policy/模板若已相符跳过；冲突即停止，不覆盖。记录首错、已提交 DDL、迁移账本和恢复路径。
5. 打包当前任务 Jar；显式禁止 Flowable 自动 DDL、Quartz 全量启动写入及无关 DCC background workers。仅启动登记 slot6；后台全量任务停用阶段不作为按日期生效验收通过。
6. 使用真实前端配置任务自有项目、审核人、目录/类型/矩阵、用户签名及申请数据，完成全部已确认业务闭环。业务动作只能 Playwright 真实页面；DB/API 只读证明。依赖/权限/策略缺失如实定位，不猜 admin 或模拟成功。
7. 生效使用 Asia/Shanghai、工作台提前 7 天提醒、每分钟检查。正式 job 先暂停登记，先核对运行范围；实际开启和触发经正式任务页，确认不会处理非任务历史数据后验收。调度启用与注册分开记录。
8. 任一原库 DDL/配置不一致，暂停任务服务与后续操作，仅恢复冻结清单的16个受影响原表及其原始数据，清理本次确实新建的17张表；先复核无其它writer及恢复预览，不能向原库恢复全表schema或覆盖其它表。MySQL DDL隐式提交，不能用ROLLBACK承诺撤销。恢复属于本轮需批准的数据库写操作，不删除共享容器/volume，不动主目录及其它任务服务。

## 最终审批材料

最终精确列表及原始 SHA256 见 g18-approved-scope-candidate.json，备份对象见 g18-backup-scope.json，校验与受影响原表单独恢复载荷见 g18-backup-receipt.json。隔离演练库名为 dcc_intqms_g18_rehearsal，不存在才创建；将真实schema/210表数据恢复到该库后执行同一19SQL两次，并核对原16表的原列/历史身份、正文及BPM/签名不变。

原库执行仅该19SQL，不运行base/catalog或旧seed。新增V4只插6套定义/模型/info及其正文，保留V1–V3；新增待生效作废policy2条、通知模板1条，现行历史policy不覆盖。账号/项目审核人不SQL猜填。activation SQL本次不执行；后续正式任务页单独配置/启用并核对任务作用范围。源码560后端/68当前前端组合、项目types/build及Jar package均PASS；真实页面与最终本地int_qms合入尚未完成。

执行方式经G20复核：不能调用通用全量发布器，因为它会重放旧初始化且改写旧账本。采用严格19文件白名单、原SHA和当前真实25项外部依赖检查，仅为本次19迁移追加独立执行记录/账本，不覆盖既有历史迁移行；重复执行保存任务回执而不重写旧行。账本恢复仅涉及新增且仍与本任务operation/hash一致的19项记录，不能用全账本备份覆盖历史。依赖事实缺失或任何SQL首错即停。候选SQL和执行范围不增加其它业务数据修改。
