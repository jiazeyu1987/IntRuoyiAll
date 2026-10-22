package cn.iocoder.yudao.module.system.service.gxpaudit;

/** Exact frozen creation sources; registration does not activate a signature writer. */
public final class SignatureCreationProtocolRegistry {
    private SignatureCreationProtocolRegistry() {}

    public static Protocol require(String evidenceFormatVersion) {
        if ("V1".equals(evidenceFormatVersion)) {
            return new Protocol("V1", "signature.record.create", "SERVICE_METHOD",
                    "ElectronicSignatureServiceImpl.sign");
        }
        if ("V2".equals(evidenceFormatVersion)) {
            return new Protocol("V2", "signature.record.create", "SERVICE_METHOD",
                    "ElectronicSignatureTransactionExecutor.executeAuthenticated");
        }
        throw new IllegalArgumentException("Unsupported signature creation protocol: " + evidenceFormatVersion);
    }

    public record Protocol(String evidenceFormatVersion, String operationId, String sourceType, String sourceLocator) {}
}
