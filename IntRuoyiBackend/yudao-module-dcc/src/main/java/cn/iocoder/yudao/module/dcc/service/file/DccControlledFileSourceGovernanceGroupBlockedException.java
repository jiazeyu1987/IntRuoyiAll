package cn.iocoder.yudao.module.dcc.service.file;

final class DccControlledFileSourceGovernanceGroupBlockedException extends RuntimeException {

    private final String reasonCode;
    private final String detail;

    DccControlledFileSourceGovernanceGroupBlockedException(String reasonCode, String detail) {
        super(reasonCode + ": " + detail);
        this.reasonCode = reasonCode;
        this.detail = detail;
    }

    String reasonCode() {
        return reasonCode;
    }

    String detail() {
        return detail;
    }
}
