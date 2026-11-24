package cn.iocoder.yudao.module.erp.service.config;

/** Deployed modules report their own durable Kingdee source mappings, including deleted history. */
public interface ErpKingdeeSourceEvidenceProvider {
    boolean hasEvidence(Long tenantId);
}
