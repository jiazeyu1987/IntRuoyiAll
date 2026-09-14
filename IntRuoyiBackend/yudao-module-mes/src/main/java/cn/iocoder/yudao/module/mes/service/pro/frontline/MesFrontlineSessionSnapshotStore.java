package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.Duration;

public interface MesFrontlineSessionSnapshotStore {

    void save(Long tenantId, String snapshotId, String snapshotJson, Duration ttl);

    String get(Long tenantId, String snapshotId);

}
