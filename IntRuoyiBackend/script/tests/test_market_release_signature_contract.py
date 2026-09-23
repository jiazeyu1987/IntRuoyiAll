from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read(relative_path: str) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


signature_service = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "batchrecord/MesProBatchRecordExecutionSignatureService.java"
)
subject_adapter = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "batchrecord/MesBatchRecordSignatureSubjectAdapter.java"
)
release_service = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "batchrecord/MesProEdhrReleaseServiceImpl.java"
)
approve_req = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/"
    "batchrecord/vo/MesProEdhrReleaseApproveReqVO.java"
)
finalization_command = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/productionrelease/core/"
    "MesReleaseFinalizationCommand.java"
)
manager_service = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java"
)
audit_recorder = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "processpool/team/MesTeamLeaderActiveOrderReleaseAuditRecorder.java"
)
signoff_service = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "productionrelease/manager/MesProductionReleaseSignoffService.java"
)
release_transaction_do = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/"
    "batchrecord/MesProEdhrReleaseTransactionDO.java"
)
release_transaction_mapper = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/"
    "batchrecord/MesProEdhrReleaseTransactionMapper.java"
)
detail_service = read(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
    "processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java"
)
release_migration = read(
    "sql/mysql/20260923_mes_release_transaction_signature_id.sql"
)


assert 'ACTION_MARKET_RELEASE = "MARKET_RELEASE"' in signature_service
assert "ACTION_MARKET_RELEASE" in subject_adapter
assert "recordMarketReleaseSignature" in signature_service
assert "EDHR_MARKET_RELEASE" in signature_service
assert "上市放行" in signature_service

assert "MesProBatchRecordExecutionSignatureService" in release_service
assert "recordMarketReleaseSignature" in release_service
assert ".setSignatureId(" in release_service
assert "getSignatureId()" in release_service

assert "private Long signatureId;" in approve_req
assert "@JsonIgnore\n    private Long signatureId;" in finalization_command
assert ".setSignatureId(command.getSignatureId())" in release_service
assert ".setSignatureId(command.getSignatureId())" in manager_service
assert ".setSignatureId(" in manager_service
assert 'metadata.put("signatureId", command.getSignatureId())' in audit_recorder
assert "findVerifiedSignatureId" in signoff_service
assert "private Long approvalSignatureId;" in release_transaction_do
assert "approval_signature_id = #{signatureId}" in release_transaction_mapper
assert "MesProEdhrReleaseTransactionMapper" in detail_service
assert "getApprovalSignatureId()" in detail_service
assert "approval_signature_id" in release_migration
assert "ADD COLUMN `approval_signature_id` BIGINT NULL" in release_migration

print("PASS: market release formal signature contract")
