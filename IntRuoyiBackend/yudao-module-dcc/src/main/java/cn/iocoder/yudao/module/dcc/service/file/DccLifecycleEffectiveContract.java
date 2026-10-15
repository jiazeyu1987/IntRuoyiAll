package cn.iocoder.yudao.module.dcc.service.file;

/** Read-only identity of the exact deployed workflow contract, never an inferred file-field migration. */
public record DccLifecycleEffectiveContract(Long tenantId, Long controlledFileId, String processInstanceId,
        String processDefinitionId, String deploymentId, String processKey, String definitionDigest, Rule rule) {
    public enum Rule { LEGACY_V4_PRESET_DATE, NEW_EXPLICIT_MODE, DELAYED_CONFIRMATION_MODE }
}
