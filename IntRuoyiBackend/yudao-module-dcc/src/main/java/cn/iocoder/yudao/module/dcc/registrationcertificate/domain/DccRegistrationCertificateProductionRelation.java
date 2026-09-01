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
                entrustedEnterprises, "受托企业列表不能为空"));
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

    public void assertProjectionMatches(
            List<DccRegistrationCertificateEntrustedEnterprise> projectedEnterprises) {
        Objects.requireNonNull(projectedEnterprises, "受托企业投影列表不能为空");
        if (!entrustedEnterprises.equals(projectedEnterprises)) {
            throw new IllegalArgumentException("受托企业投影与正式数据不一致");
        }
    }

    private void validate() {
        if (!entrustedProduction && !selfProduction && entrustedEnterprises.isEmpty()) {
            return;
        }
        if (!entrustedProduction && !selfProduction) {
            throw new IllegalArgumentException("委托生产和自行生产至少选择一项");
        }
        if (entrustedProduction && entrustedEnterprises.isEmpty()) {
            throw new IllegalArgumentException("选择委托生产时必须填写受托企业");
        }
        if (!entrustedProduction && !entrustedEnterprises.isEmpty()) {
            throw new IllegalArgumentException("未选择委托生产时不能保留受托企业");
        }
        Set<Long> enterpriseIds = new HashSet<>();
        for (DccRegistrationCertificateEntrustedEnterprise enterprise : entrustedEnterprises) {
            if (enterprise == null || enterprise.enterpriseId() == null || enterprise.enterpriseId() <= 0) {
                throw new IllegalArgumentException("受托企业 ID 必须为正数");
            }
            if (enterprise.enterpriseName() == null || enterprise.enterpriseName().isBlank()) {
                throw new IllegalArgumentException("受托企业名称快照不能为空");
            }
            if (!enterpriseIds.add(enterprise.enterpriseId())) {
                throw new IllegalArgumentException("受托企业 ID 不得重复");
            }
        }
    }
}
