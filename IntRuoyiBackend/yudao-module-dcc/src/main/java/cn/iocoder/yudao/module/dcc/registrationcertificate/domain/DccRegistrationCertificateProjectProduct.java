package cn.iocoder.yudao.module.dcc.registrationcertificate.domain;

import java.util.List;

/** Formal DCC identity frozen with the certificate, independent of showroom numbering. */
public record DccRegistrationCertificateProjectProduct(
        Long projectCodeId, Long productCatalogId, String productCode, String productName,
        List<Binding> bindings) {
    public record Binding(String source, Long relationId) { }
}
