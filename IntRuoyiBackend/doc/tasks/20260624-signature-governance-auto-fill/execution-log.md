# 执行日志：电子签名治理自动回填后端支持

BDD: 查询电子签名治理候选 -> Given 当前租户存在真实 DCC 签名记录 / When 前端请求治理候选 / Then 后端返回签名 ID、业务 Key、版本、哈希、动作和含义字段，不返回伪造数据。

INFO: scope -> 仅新增/复用只读候选数据，不改变现有业务写入 API。

RED: mvn -pl yudao-module-dcc -Dtest=DccElectronicSignatureManagementServiceTest#getSignaturePage_enrichesControlledFileAndActorMetadata -DfailIfNoTests=false test -> FAIL, DCC 签名分页响应缺少 sourceObjectKey/sourceVersionId/controlledCopyObjectKey/controlledCopyVersionId。

GREEN: mvn -pl yudao-module-dcc -Dtest=DccElectronicSignatureManagementServiceTest#getSignaturePage_enrichesControlledFileAndActorMetadata -DfailIfNoTests=false test -> PASS
