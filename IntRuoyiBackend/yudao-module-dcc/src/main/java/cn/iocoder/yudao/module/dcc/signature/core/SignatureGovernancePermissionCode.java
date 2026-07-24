package cn.iocoder.yudao.module.dcc.signature.core;

public interface SignatureGovernancePermissionCode {

    String RETENTION_QUERY = "signature-governance:retention:query";
    String RETENTION_MANAGE = "signature-governance:retention:manage";
    String PERIODIC_REVIEW_QUERY = "signature-governance:periodic-review:query";
    String PERIODIC_REVIEW_MANAGE = "signature-governance:periodic-review:manage";
    String CSV_PACKAGE_QUERY = "signature-governance:csv-package:query";
    String CSV_PACKAGE_MANAGE = "signature-governance:csv-package:manage";
    String POLICY_QUERY = "signature-governance:policy:query";
    String POLICY_MANAGE = "signature-governance:policy:manage";
}
