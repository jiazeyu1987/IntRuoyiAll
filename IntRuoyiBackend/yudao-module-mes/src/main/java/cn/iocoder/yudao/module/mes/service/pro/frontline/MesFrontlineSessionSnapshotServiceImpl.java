package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SESSION_SNAPSHOT_NOT_EXISTS;

@Service
public class MesFrontlineSessionSnapshotServiceImpl implements MesFrontlineSessionSnapshotService {

    static final Duration SNAPSHOT_TTL = Duration.ofHours(12);

    private final MesFrontlineSessionSnapshotStore snapshotStore;

    public MesFrontlineSessionSnapshotServiceImpl(MesFrontlineSessionSnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @Override
    public MesFrontlineSessionSnapshotReference issue(MesFrontlineSessionSnapshotContent content) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        requireContent(content, tenantId);
        String snapshotId = IdUtil.fastSimpleUUID();
        String snapshotHash = hash(content);
        MesFrontlineSessionSnapshot snapshot = new MesFrontlineSessionSnapshot(snapshotId, snapshotHash, content);
        snapshotStore.save(tenantId, snapshotId, JsonUtils.toJsonString(snapshot), SNAPSHOT_TTL);
        return new MesFrontlineSessionSnapshotReference(snapshotId, snapshotHash);
    }

    @Override
    public MesFrontlineSessionSnapshot require(String snapshotId, String snapshotHash, Long loginUserId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (snapshotId == null || snapshotId.isBlank() || snapshotHash == null || snapshotHash.isBlank()
                || loginUserId == null) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, "snapshot identity");
        }
        String snapshotJson = snapshotStore.get(tenantId, snapshotId);
        if (snapshotJson == null || snapshotJson.isBlank()) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_NOT_EXISTS, snapshotId);
        }
        MesFrontlineSessionSnapshot snapshot = JsonUtils.parseObject(snapshotJson, MesFrontlineSessionSnapshot.class);
        if (snapshot == null || snapshot.content() == null
                || !Objects.equals(snapshotId, snapshot.snapshotId())
                || !Objects.equals(snapshotHash, snapshot.snapshotHash())
                || !Objects.equals(snapshotHash, hash(snapshot.content()))
                || !Objects.equals(tenantId, snapshot.content().tenantId())
                || !Objects.equals(loginUserId, snapshot.content().loginUserId())) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, snapshotId);
        }
        return snapshot;
    }

    private static void requireContent(MesFrontlineSessionSnapshotContent content, Long tenantId) {
        if (content == null || !Objects.equals(tenantId, content.tenantId())
                || content.loginUserId() == null || content.routeId() == null
                || content.routeProcessId() == null || content.processId() == null
                || content.workstationId() == null || content.employeeSwitchSnapshots() == null
                || content.devices() == null || content.defectReasons() == null
                || content.productionSubmitContext() == null) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, "snapshot content");
        }
    }

    private static String hash(MesFrontlineSessionSnapshotContent content) {
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(content));
    }

}
