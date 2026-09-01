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
            throw new IllegalArgumentException("正式版本 ID 和快照 ID 必须为正数");
        }
        Objects.requireNonNull(productionRelation, "生产关系不能为空");
        boundFileIds = List.copyOf(Objects.requireNonNull(boundFileIds, "已绑定文件 ID 列表不能为空"));
        if (boundFileIds.isEmpty() || boundFileIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("正式注册证信息必须包含有效的已绑定文件 ID");
        }
        if (new HashSet<>(boundFileIds).size() != boundFileIds.size()) {
            throw new IllegalArgumentException("已绑定文件 ID 不得重复");
        }
    }
}
