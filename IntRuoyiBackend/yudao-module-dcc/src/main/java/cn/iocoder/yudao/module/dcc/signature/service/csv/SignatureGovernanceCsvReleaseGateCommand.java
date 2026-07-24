package cn.iocoder.yudao.module.dcc.signature.service.csv;

public record SignatureGovernanceCsvReleaseGateCommand(String releaseId,
                                                       SignatureGovernanceCsvPackageCommand packageCommand) {

    public SignatureGovernanceCsvReleaseGateCommand {
        if (isBlank(releaseId) || packageCommand == null) {
            throw new IllegalArgumentException("CSV release gate requires releaseId and package command");
        }
        releaseId = releaseId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
