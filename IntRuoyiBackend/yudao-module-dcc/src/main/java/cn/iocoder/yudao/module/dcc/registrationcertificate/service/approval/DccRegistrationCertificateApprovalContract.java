package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

public final class DccRegistrationCertificateApprovalContract {

    public static final String PROCESS_DEFINITION_KEY = "dcc-registration-certificate-access";
    public static final String APPROVAL_TASK_DEFINITION_KEY = "REG_CERT_ACCESS_APPROVAL";
    public static final String APPROVER_ROLE_CODE = "dcc_registration_certificate_approver";
    public static final String APPROVAL_PERMISSION = "dcc:registration-certificate:access-request:approve";
    public static final String UPLOAD_APPROVAL_PERMISSION = "dcc:registration-certificate:upload:approve";
    public static final String REQUEST_TYPE_UPLOAD_CERTIFICATE = "UPLOAD_CERTIFICATE";
    public static final String BUSINESS_KEY_PREFIX = "DCC_REG_CERT_ACCESS:";

    private DccRegistrationCertificateApprovalContract() {
    }
}
