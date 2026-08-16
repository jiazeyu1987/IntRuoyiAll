package cn.iocoder.yudao.module.dcc.registrationcertificate.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record DccRegistrationCertificateFormalFacts(
        Long versionId,
        Long snapshotId,
        DccRegistrationCertificateProductionRelation productionRelation,
        List<Long> boundFileIds) {

    public DccRegistrationCertificateFormalFacts {
        if (versionId == null || versionId <= 0 || snapshotId == null || snapshotId <= 0) {
            throw new IllegalArgumentException("Formal version and snapshot ids must be positive");
        }
        Objects.requireNonNull(productionRelation, "production relation must not be null");
        boundFileIds = List.copyOf(Objects.requireNonNull(boundFileIds, "bound file ids must not be null"));
        if (boundFileIds.isEmpty() || boundFileIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Formal facts require positive bound file ids");
        }
        if (new HashSet<>(boundFileIds).size() != boundFileIds.size()) {
            throw new IllegalArgumentException("Bound file ids must be unique");
        }
    }
}
