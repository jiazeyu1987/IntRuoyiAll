# 正式57：只读身份准入调查与处理路线

## 当前结论

保持阻断。测试58可丢弃165张证的任意匹配授权不适用于57；不向57补业务数据，不改变正式preflight，不更改历史账本或原SQL，不跳过租户或VOIDED证件。

2026-11-27 11:46:27（北京时间）仅在57执行一次READ ONLY事务，按当前正式多证preflight的sources/projects/candidates原逻辑追踪主档、当前快照、项目、两类原始关系和对应审批。没有重查已经确认的102张汇总，也没有执行写入、重启或恢复；未连接58。

## 实际结果

| 项目 | 当前57结果 |
| --- | --- |
| 首个正式阻塞 | UNRESOLVED_PRODUCT：102张证、65个项目 |
| 项目是否存在 | 65个均存在且未删除 |
| 对应原始数据关系 | dcc_data_relation共0行，包括非CONFIRMED及已删除行 |
| 对应原始审批产品关系 | dcc_project_product_relation共0行，包括非ACTIVE及已删除行 |
| 快照身份不符 | 136张，包含与102张未解析证重叠的77张 |
| 快照声明自有产品但主档无项目 | 55张 |
| 有解析来源但当前快照项目/产品身份不同 | 4张（项目及产品ID均有分歧） |
| 合并不同证件 | 161张，同一租户；153 ACTIVE、8 VOIDED |
| 可选产品绑定表 | 尚不存在 |

102张缺来源的根因已收敛为两类关系均无原始行，不能表述为“审批状态没通过”或“目录名称为空”。缺少关联后，解析产品编码/名称自然为空；两者不是两批不同问题。项目存在或MDM产品有值也不能替代正式DCC目录来源。

这些是57本次实测的161张，不沿用58曾有165张的清单。VOIDED仍属于现行迁移的未删除正式历史范围；不能因已作废就跳过。

## 业务链路与代码依据

| 业务节点 | 代码依据及影响 |
| --- | --- |
| 从项目解析产品 | `registrationcertificate/service/certificate/DccRegistrationCertificateProjectProductService.java:20` 校验租户、项目启用和非库项目；空来源直接报错，多个目录、重复来源或缺编码名称均拒绝。仅有项目主档不能取得可用DCC产品。 |
| 来源与审批身份 | `registrationcertificate/dal/mysql/DccRegistrationCertificateProjectProductMapper.java:13` 仅接受CONFIRMED数据关系，或ACTIVE产品关系加同租户COMPLETED申请，且申请生成项目、目录和relationId完全对应；目录还须未删除。65个项目的两类原始关系均无行。 |
| 正式新建产品审批 | `controller/admin/projectcode/DccProjectProductCreateController.java:57` 起按create/review/update权限分步处理；`service/projectcode/productcreate/DccProjectProductCreateWriteService.java:124` 完成时同时记录relationId和COMPLETED。这是需要真实业务申请的写入链路，不能由迁移伪造审批状态。 |
| 数据关系确认与注册证 | `service/relation/DccDataRelationServiceImpl.java:76` 对指定注册证验证产品及项目绑定后才CONFIRMED。不能在尚未解析的历史证上循环自动补关系来制造来源。 |
| 普通证件修改边界 | `registrationcertificate/service/change/DccRegistrationCertificateChangeService.java:368` 将ownProduct/projectProduct/projectProducts/projectCodeId等作为保护字段；`DccRegistrationCertificateProductMaintenanceService.java:23` 是已有自有产品现行证追加产品能力，不能把它当任意历史身份纠错入口。 |
| 发布准入及落库 | `target-preflight/20261127_dcc_registration_certificate_multi_products.preflight.sql` 先拒绝未解析产品，再查当前快照身份；新迁移在身份通过后才建立逐证绑定。通过多证基数规则不能自动使缺失来源或旧快照分歧消失。 |

上述Java路径均相对于 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`。这是当前失败节点的实际调用链核对，不等于整条业务流程或E2E验收。

## 在当前授权内的正式处理路线

1. 固定本次只读原件与161张逐证问题清单，按102缺来源、55无项目、4身份分歧分组，77张重叠不重复计数。保留原证件、审批、版本/快照和下游引用；不给目标项目或目录填写猜测值。
2. 对65个有项目但无来源的项目，用真实产品目录与批准材料确认目标及来源。如果必须补关系或完成申请，这已经是业务数据变更；当前“不补业务数据”范围内不执行。不能只将状态设为CONFIRMED/COMPLETED，也不能用MDM ID或名称匹配取代DCC目录证据。
3. 对55个无项目的自有产品快照，先明确是原录入错误、实际外部产品，还是应有项目的历史业务；不默认ownProduct=false，不临时配项目。对4个分歧逐证比对批准原件与当前项目产品，确认哪一份身份具有业务依据；不默认最新关系或旧快照必然正确。
4. 确需纠错时，另有明确授权与精确清单后，走受控历史身份纠错能力：原租户/版本/快照全字段预演与并发校验、保留原批准快照、追加有审批依据的新事实及审计、分别验证待生效版本、绑定与下游。该能力不能由58临时回填工具直接代替；本次不实现或执行数据库写入。
5. 若业务确认仅追溯、不参与运行，需要先确认并实现明确的非运行模型及审查标准；当前系统没有“加个归档标记即豁免”的合同。未实现前仍保持准入阻断，不删证件或缩小检查范围。
6. 身份问题有合法处理且全租户正式preflight真实PASS后，另行检查当前账本、所有新SQL只读预检、候选包和恢复点，再按正式发布流程推进57。代码和本机测试通过不是目标准入通过。

当前边界下没有既“不补业务数据”又自动消除上述实际身份问题的操作。交付只读证据和可审核路线，不把不满足条件改写为发布成功。

## 证据

私密目录：`C:/SpugHandoff/release/private/prod57-admission-20261127/`，包含实际执行SQL、完整逐证结果、摘要和stderr。数据库版本MySQL8.0.39，事务隔离REPEATABLE-READ。本次诊断SQL SHA256 `9501918d6984432eb2c90ce4d4869023e21ed0e9b638994e8780d4d1f15dca27`；原始观测SHA256 `87fa5339c3ef0e470cd294b1bde6c897ea6c19b93927f2ece712a6f03c3826dd`。源提交 `04e39c9eb7e3246accc50a5125cfb981119c7d8a`；该提交以来正式多证preflight未改动。

同目录仓库文件 `prod57-identity-diagnostic.readonly.sql` 是本次执行查询，不是正式迁移或放行门禁；不含逐证业务数据。首个阻塞原件另保留在既有 `test-prod-20261126/prod-multi-preflight-20261127.json`，不能把后续分类诊断冒充正式preflight PASS。
