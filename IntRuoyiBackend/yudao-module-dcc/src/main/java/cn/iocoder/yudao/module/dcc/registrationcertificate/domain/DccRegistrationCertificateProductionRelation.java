package cn.iocoder.yudao.module.dcc.registrationcertificate.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DccRegistrationCertificateProductionRelation {

    private final boolean entrustedProduction;
    private final boolean selfProduction;
    private final List<DccRegistrationCertificateEntrustedEnterprise> entrustedEnterprises;

    public DccRegistrationCertificateProductionRelation(
            boolean entrustedProduction,
            boolean selfProduction,
            List<DccRegistrationCertificateEntrustedEnterprise> entrustedEnterprises) {
        this.entrustedProduction = entrustedProduction;
        this.selfProduction = selfProduction;
        this.entrustedEnterprises = List.copyOf(Objects.requireNonNull(
                entrustedEnterprises, "entrusted enterprises must not be null"));
        validate();
    }

    public boolean entrustedProduction() {
        return entrustedProduction;
    }

    public boolean selfProduction() {
        return selfProduction;
    }

    public List<DccRegistrationCertificateEntrustedEnterprise> entrustedEnterprises() {
        return entrustedEnterprises;
    }

    public void assertProjectionMatches(List<Long> projectedEnterpriseIds) {
        Objects.requireNonNull(projectedEnterpriseIds, "projected enterprise ids must not be null");
        List<Long> authoritativeIds = entrustedEnterprises.stream()
                .map(DccRegistrationCertificateEntrustedEnterprise::enterpriseId)
                .toList();
        if (!authoritativeIds.equals(projectedEnterpriseIds)) {
            throw new IllegalArgumentException("Entrusted enterprise projection does not match authoritative facts");
        }
    }

    private void validate() {
        if (!entrustedProduction && !selfProduction) {
            throw new IllegalArgumentException("At least one production mode is required");
        }
        if (entrustedProduction && entrustedEnterprises.isEmpty()) {
            throw new IllegalArgumentException("Entrusted production requires an authority enterprise");
        }
        if (!entrustedProduction && !entrustedEnterprises.isEmpty()) {
            throw new IllegalArgumentException("Non-entrusted production cannot retain authority enterprises");
        }
        Set<Long> enterpriseIds = new HashSet<>();
        for (DccRegistrationCertificateEntrustedEnterprise enterprise : entrustedEnterprises) {
            if (enterprise == null || enterprise.enterpriseId() == null || enterprise.enterpriseId() <= 0) {
                throw new IllegalArgumentException("Entrusted enterprise id must be positive");
            }
            if (enterprise.enterpriseName() == null || enterprise.enterpriseName().isBlank()) {
                throw new IllegalArgumentException("Entrusted enterprise name snapshot must not be blank");
            }
            if (!enterpriseIds.add(enterprise.enterpriseId())) {
                throw new IllegalArgumentException("Entrusted enterprise ids must be unique");
            }
        }
    }
}
