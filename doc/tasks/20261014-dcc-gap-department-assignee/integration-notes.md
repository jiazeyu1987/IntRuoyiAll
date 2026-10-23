# G05 接入说明

## 已实现
- 三个独占生产文件已接入现有正式链路，无新接口/字段/流程或数据库迁移。
- 候选来自精确任务的冻结部门义务。即使跨部门负责人仍有办理该义务资格，其本人不再被加入该部门候选。
- 新保存从 AdminUserApi.getUser 读取当前人员事实，校验账号ID、租户、启用、部门相同及岗位；保持 requireReadyParticipants 对阶段权限、签名授权和有效图片的校验，然后才准备关系、真实签名和保存/转派。
- 已提交精确重放在原有分支回读，保持原签名/时间与载荷冲突处理。人员后续转部门不重写已提交历史。
- revision 整改负责人全员目录保留，仅会签人全员目录移除。

## 待 Root 复核/验证
- 2026-10-14接续：Root 已接受三个生产最小 diff 的软件实现 review，重跑4项前端组件测试 PASS，接管文件保护；本线程保持冻结且不重复编译。
- 无共享生产接入依赖，无文件转交申请。
- Maven 当前无唯一票。本轮没有运行 Maven；建议 Root 唯一票覆盖 DccWorkflowSignoffAssignmentTest、DccWorkflowRelationStageIntegrationTest、DccRelationStageSnapshotServiceTest，带 -am 使用当前 reactor 依赖。
- 当前 Node/Vue 与私有 javac/JUnit 是开发验证；不构成真实页面或正式运行验收。E2E 未获授权未运行。
- 隔离编译重新编译当前服务、context、请求 VO 与三类测试及 H2 helper，其余依赖读取已有 target/classes 与本地 Maven jar；不写共享 target、不把依赖产物当本轮全量源码编译证据。
- 专属 integration fixture 的唯一差异是本部门成员身份对齐；共享基类与迁移辅助类未写。

## 工程收尾
Git 提交/推送被本轮明确禁止；Root 统一票尚待覆盖三类当前源码及依赖。整体工程/E2E未验收，目标继续；不跨线程自行发消息，不自动标 completed。

## V01 交还（2026-10-15）
- Root可接管两份共享test schema及DccFileCategoryAdminServiceImplTest.java的限定七行fixture；精确合同/最小diff/当前hash均已归档。
- 当前真实隔离H2和结构合同45/45PASS（含H2 16项），不是分类服务整类或签名往返验收。旧G05 98项未重复编译。
- 无生产接线/迁移改动。新确认字段及C05三处精度均按正式迁移，category source NOTNULL无default。
- v01-fixture-candidates.md列出14个额外BaseDb测试潜在受影响文件与当前builder行锚点，均未授权/未改，需Root先结合正式票实际路径确认再授权；不批量默认、不改未知source负例。
- 当前类六请求与helper只补合法source，不动原taxonomy/已用Master/目录/治理拒绝断言；这些原语义仍须统一Maven票真实执行验证，不能用source guard先失败冒原负例PASS。

## V02 交还
Root14文件授权内18个正常Category来源已显式配置，56项结构/AST及独立H2列约束通过，保持1125原断言AST/14原文件去增量byte和LF还原。没有14类业务JUnit或reactor执行，不能替代原负例验收。14文件manifest/最小diff/前后AST见v02资料，更多inline SQL只列Root，不自动授权。

A21 checkout列已接实际G06正式DDL/DO/Mapper合同：测试schema只加nullable CHAR64一列；独立5项真实RED/GREEN，当前schema46项回归PASS。精确newbaseline/current SHA、单行diff及只读正式源SHA见A21资料。未改生产或G06test，未验证其服务canonical/重放；未来3个custom草稿字段按Root最新指令等实际DO/DDL后再接，当前不预造。

## A23/A26正式限定handoff
ActualDDL已读取，三CALL的类型与nullable/no-default guard、直接两依赖文件存在、actualFileDO三字段对应；不宣称完整迁移policy闭包/实库首次重复。DCC测试schema3custom列+A26现版本列nullable已接入，实际7项H2、4项route H2及当前公共canonical/readCanonical11项通过，当前schema50项合同PASS。公共真实版本7保持、PUBLIC null/缺失/0/字符串版本业务guard拒绝，不把schema可NULL当公共合法；完整业务snapshot save/restore仍待Root票。

最终schema-handoff.json列17当前schema/fixture SHA与该限定范围ready，schema-final-minimal.diff给Root相对各本轮基线的精确union差异。V02 14来源调用/1125 AST和SHA仍相符，未重跑实际14类或原98。文件归还Root保护，G06可用新共享fixture继续明确授权工作；Root整体freeze/reactor/E2E另验收，不新增round或任何未知custom字段。
