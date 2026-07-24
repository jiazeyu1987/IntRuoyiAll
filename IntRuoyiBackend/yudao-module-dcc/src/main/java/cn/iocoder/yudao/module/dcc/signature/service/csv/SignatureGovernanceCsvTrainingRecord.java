package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvTrainingRecord(String trainingRecordId,
                                                   String traineeUserId,
                                                   String sopDocumentId,
                                                   String evidenceRef,
                                                   boolean effective) {

    public SignatureGovernanceCsvTrainingRecord {
        trainingRecordId = trimToNull(trainingRecordId);
        traineeUserId = trimToNull(traineeUserId);
        sopDocumentId = trimToNull(sopDocumentId);
        evidenceRef = trimToNull(evidenceRef);
    }

    boolean isEffective() {
        return effective && !isBlank(trainingRecordId) && !isBlank(traineeUserId)
                && !isBlank(sopDocumentId) && !isBlank(evidenceRef);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
