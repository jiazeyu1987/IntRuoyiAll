package cn.iocoder.yudao.module.system.api.workbench;

/** Module-owned declaration. Labels are presentation; sourceId is an extensible protocol string. */
public record SourceDescriptor(String sourceId, String ownerModule, int contractVersion, int sortRank,
        String label, String taskType, String policyId, String actionKind, int payloadVersion) {
    public SourceDescriptor {
        if (!identifier(sourceId) || !identifier(ownerModule) || !identifier(policyId) || !identifier(actionKind)
                || contractVersion != 1 || payloadVersion != 1 || sortRank < 0
                || label == null || label.isBlank() || taskType == null || taskType.isBlank())
            throw new IllegalArgumentException("CONTRACT_VERSION_UNSUPPORTED or invalid source descriptor");
    }
    private static boolean identifier(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_.-]{0,79}");
    }
}
