# 同一产品关联多张注册证

## 业务结论

用户确认：同一产品可关联多张注册证。一张证仍可关联多个产品，同一张证内不能重复关联同一产品或为同一项目绑定两个产品。不同证的审批预留、正式绑定、驳回释放相互独立。

本记录取代 `20261127-product-migration-admission` 中“一产品一证”的业务前提；原迁移、只读检查与既有发布包保持原字节，不能修改历史校验和或伪造 APPLIED。

## 业务链路核对

| 节点 | 代码依据与结果 |
| --- | --- |
| 上传选择产品 | `ProductBindings.vue` 明确多证规则；`DccRegistrationCertificateUploadController.projectCandidates` 经公司/项目范围校验后调用 `UploadService.candidateBinding`，其他证的绑定不再禁用候选。实际产品身份仍由 `PrerequisiteValidator` 从有效项目产品关系解析，单证重复校验保留。 |
| 现行证追加产品 | `DccRegistrationCertificateProductController.add` 保留上传和当前查询两项权限；`ProductMaintenanceService.addProducts` 继续校验租户、公司范围、ACTIVE 状态、无待审事项、行版本及快照版本；追加后生成新快照、复制受托企业证据并记审计。 |
| 提交及审批落库 | `CommandTransactionService` 创建/重提调用 `ProductBindingService.reserve`；正式通过调用 `confirm`，拒绝/撤回调用 `releasePending`。键和查重均按租户+注册证限定，其他证不会占用产品或被释放。 |
| 数据关系核验 | `DccDataRelationServiceImpl.confirm` 以指定注册证分别验证 `containsProduct` 和 `containsProject`。`dcc_data_relation.registration_certificate_id` 是该关系显式选择的证，不代表产品的全部注册证，不据此否定其他有效证。 |
| 目录展示 | `ProductCatalogRegistrationSyncService.enrichCurrentCertificate` 读取该产品全部 BOUND、未删除、ACTIVE 的证；逐一核验当前版本和快照。目录名称/证号列逐证展示链接，继续按原查询权限控制打开；无显式选证且存在多证时不任取第一张填充单证属性。 |
| 下游文件 | `ProjectCodeFileAssociationService` 与 `DccProjectCodeListQuery` 的绑定连接均带租户、证件 ID 和 BOUND 条件，能保留同一项目下不同证的文件证据。未改变审批、文件投递或下游业务状态规则。 |

## 数据库及发布输入

新增 `20261127_dcc_registration_certificate_multi_products.sql`，通过既有 supersedes 机制替代两个不可变旧输入：`20261125_dcc_registration_certificate_products`、`20261127_dcc_registration_certificate_products_admission`。新迁移强制绑定同名只读 preflight，`applyOrder=1`。

主键改为 `(tenant_id,certificate_id,product_catalog_id)`；项目唯一键改为 `(tenant_id,certificate_id,project_code_id)`。新增产品查询索引。新建表、已知旧键的空表和已有合法绑定的旧表均可升级，不删除、覆盖历史绑定，不更改关系原先选择的注册证。

多证及关系显式选证不同不再报冲突；缺失/歧义产品身份、当前快照身份不符仍在绑定表 DDL 前阻止执行。已存在的绑定表只接受已知键结构；表内身份冲突在停写执行阶段另行验证，不覆盖数据。只读 preflight 因需同时支持表不存在的目标，仅校验可选绑定表结构，不能代替停写后的表内行核验。

测试环境此前 FAILED 迁移账本仍需维护流程核实处理，supersedes 不绕过 FAILED。现有运行代理仍在停写后的迁移阶段执行数据 preflight；本提交不声称已修复维护流程的停写前执行缺口。

此前历史报告中 `UNRESOLVED_PRODUCT=102`、`SNAPSHOT_PRODUCT_MISMATCH=140` 是冲突报告行数；多证规则确认本身不能自动修复这些身份问题。`MULTIPLE_CERTIFICATES=2`、`RELATION_CERTIFICATE_MISMATCH=94` 两类判定基于已废弃的全局单证前提，不再据此阻断新迁移。功能开发阶段未连接或写入服务器业务库；后续用户授权的58临时测试数据匹配结果见文末，未部署或重启服务。

## 验证结果

- MySQL 8.0.39 独立容器：无网络、无映射端口、tmpfs 数据，仅任务合成夹具。6 个测试方法覆盖多证回填、旧空/非空表升级、创建人和时间保留、重复执行、正式快照不可变、单证唯一键、跨租户以及 11 类真实身份冲突、未知表结构和已有错误绑定拒绝。
- 发布源注册检查：4 项通过；全部新 SQL 已明确注册，旧源校验和保持。
- Vue 3.5.13 SFC 编译：`ProductBindings.vue`、`ProductCatalogTabPanel.vue` 均通过（模板及 TypeScript 脚本解析）；未宣称完整前端构建或 E2E。
- Maven 离线 reactor 编译通过。扩展测试命令：`mvn -o -pl yudao-module-dcc -am test '-Dtest=DccRegistrationCertificate*Test,DccProductCatalogRegistrationSyncServiceTest,DccDataRelationServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false'`。
- DCC 扩展回归共 572 项：568 通过、1 跳过、3 失败。直接改动对应的绑定 7 项、追加产品 6 项、上传 24 项、目录 4 项全部通过；下游文件关联和数据关系测试通过。整体 Maven 结果为 FAILURE，不记为全绿。
- 3 项失败与本次差异无关：`BpmIntegrationTest` 两个下载审批测试夹具缺少必填 `ownerCompanyName`，在未改动的 `ApprovalService.requireSummaryText` 报错；`CommandControllerTest.lifecycleActionControllers_exposeApprovedRoutesPermissionsAndIdempotencyHeaders` 断言附件控制器不得存在，而该文件在基线 `9a8d14665` 已存在。上述服务、控制器及测试相对基线 diff 为空；本任务未改动这些断言或业务。

固定提交的 CRLF 干净检出发布输入、历史发布包完整性、目标计划与 FAILED 账本负例结果另见同目录 `release-input-verification.json`。

已核验功能提交 `0eadff1a0b9a61157b3051e7a98839503b66f7dc`：926 个迁移、938 个根 SQL、1256 个包输入，无漏登记或缺失输入，历史封包完整性通过。本地缓存账本快照 test 855 条、prod 851 条，两者均计划 5 个待执行迁移，新迁移先执行，两个旧迁移为 SKIP_IMMUTABLE；缺失新 preflight 和旧迁移 FAILED 两个负例均准确阻断。这些是历史快照的离线计划核验，不能替代维护人员对当前服务器的检查。正常、暂存和提交 hook 的菜单门禁通过；任务测试容器已按容器 ID 和标签核实后移除，未影响其他环境。

历史维护侧对固定提交 `77d4f35f88208875200b729201adb8322177dac2` 的58真实只读执行返回 `UNRESOLVED_PRODUCT`。当时未具备发布条件，原只读诊断方案和历史证据见 `identity-resolution-plan.md`、`identity-evidence-summary.json`。之后的用户授权和实际处理更新如下，旧FAILED账本始终未修改。

## 用户授权后的58临时测试数据匹配

用户确认这165张证后续全部删除，授权由本线程随意匹配。重新只读核实：当前异常集合与165张清单完全一致，均属租户169；157张ACTIVE、8张VOIDED，4张含待生效版本。不存在数据关系、文件、审批、授权或办理任务的下游引用；另有192个版本及2条既有审计。完整变更前数据和定义保留在私密目录。

在真实58执行前，以实际表定义、触发器和范围内变更前数据在MySQL 8.0.39隔离容器演练。验证了范围外证件、跨租户和无维护令牌均被拒绝；全部写入后注入失败可整笔回滚；成功后原不可变保护恢复；相同操作重放被拒绝。容器无网络、无映射端口，任务结束已核对归属并移除。

2026-11-27 10:19:51（北京时间）实际完成：165张临时匹配到已有有效项目20、产品目录305；追加169份快照及169条审计，4个待生效版本的最新快照同步处理。原192个版本、201份快照、16条受托企业证据及2条审计逐项保持原值。原触发器元数据、范围外证件、目标项目/产品和发布账本前后一致。未删除任何证件，未触及57，58前后端继续停机。

未修改的正式preflight在事务提交前和提交后的真实58均返回 `TARGET_PREFLIGHT_PASS:20261127_dcc_registration_certificate_multi_products`。这是身份预检通过，尚未执行新多证迁移或恢复旧FAILED任务，不代表已部署、页面验收或E2E通过。执行记录和摘要见 `test58-temporary-match.md`、`temporary-test-data-repair.json`。

## 保留修复数据的正式恢复方案

收到正式恢复复核后，已完成 `preserved-recovery-plan.md` 及 `preserved-recovery-verification.json`：原检查点完整文件hash核验；从该实际检查点隔离恢复13张相关表且校验值一致；恢复后的855条真实账本通过现行正式检查，计划新多证优先、两个旧产品迁移SKIP_IMMUTABLE；精确回填修复的原ID/时间/内容后，新多证SQL及preflight通过。FAILED负例仍阻断，6项恢复编排边界测试通过。本轮未在58恢复、写库或部署。
