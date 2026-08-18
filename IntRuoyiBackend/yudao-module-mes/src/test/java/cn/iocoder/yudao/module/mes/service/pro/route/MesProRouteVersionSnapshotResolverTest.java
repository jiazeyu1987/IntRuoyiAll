package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProRouteVersionSnapshotResolverTest {

    private final MesProRouteVersionMapper mapper = mock(MesProRouteVersionMapper.class);
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();
    private final MesProRouteVersionSnapshotResolver resolver =
            new MesProRouteVersionSnapshotResolver(mapper, canonicalizer);

    @BeforeEach
    void setUpEnforcementGate() {
        when(mapper.selectSnapshotIdentityEnforcementReady()).thenReturn(1);
    }

    @Test
    void resolve_shouldUseExactPublishedVersionAndRouteProcessId() {
        String snapshot = snapshot(9L, 101L, "same-name");
        MesProRouteVersionDO version = publishedVersion(11L, 9L, snapshot);
        when(mapper.selectById(11L)).thenReturn(version);

        MesProRouteVersionSnapshotResolver.ResolvedRouteProcessSnapshot resolved = resolver.resolve(11L, 101L);

        assertEquals(9L, resolved.routeId());
        assertEquals(11L, resolved.routeVersionId());
        assertEquals(101L, resolved.routeProcessId());
        assertEquals(501L, resolved.processId());
        assertEquals("same-name", resolved.processNameSnapshot());
        assertEquals(version.getRouteSnapshotSha256(), resolved.routeSnapshotSha256());
    }

    @Test
    void resolve_shouldNotFallbackToSameNameOrCurrentProjection() {
        String snapshot = snapshot(9L, 201L, "same-name");
        when(mapper.selectById(11L)).thenReturn(publishedVersion(11L, 9L, snapshot));

        assertThrows(IllegalStateException.class, () -> resolver.resolve(11L, 101L));
        assertFalse(java.util.Arrays.stream(MesProRouteVersionSnapshotResolver.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(MesProRouteProcessMapper.class)));
    }

    @Test
    void resolve_shouldRejectUnpublishedMissingHashHashMismatchAndDuplicateIdentity() {
        String snapshot = snapshot(9L, 101L, "P1");
        MesProRouteVersionDO draft = publishedVersion(11L, 9L, snapshot);
        draft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        when(mapper.selectById(11L)).thenReturn(draft);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(11L, 101L));

        MesProRouteVersionDO missingHash = publishedVersion(12L, 9L, snapshot);
        missingHash.setRouteSnapshotSha256(null);
        when(mapper.selectById(12L)).thenReturn(missingHash);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(12L, 101L));

        MesProRouteVersionDO mismatch = publishedVersion(13L, 9L, snapshot);
        mismatch.setRouteSnapshotSha256("0".repeat(64));
        when(mapper.selectById(13L)).thenReturn(mismatch);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(13L, 101L));

        String duplicate = "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                + "{\"routeProcessId\":101,\"processId\":501,\"sort\":1},"
                + "{\"routeProcessId\":101,\"processId\":501,\"sort\":2}]}}}";
        when(mapper.selectById(14L)).thenReturn(publishedVersion(14L, 9L, duplicate));
        assertThrows(IllegalStateException.class, () -> resolver.resolve(14L, 101L));
    }

    @Test
    void resolve_shouldRejectBeforeDatabaseEnforcementIsReady() {
        when(mapper.selectSnapshotIdentityEnforcementReady()).thenReturn(0);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> resolver.resolveVersion(11L));

        assertEquals("route snapshot consumer enforcement is not ready", error.getMessage());
    }

    @Test
    void resolveVersion_shouldReadSupersededFrozenSnapshot() {
        String snapshot = snapshot(9L, 101L, "P1");
        MesProRouteVersionDO version = publishedVersion(11L, 9L, snapshot);
        version.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED);
        version.setActive(Boolean.FALSE);
        when(mapper.selectById(11L)).thenReturn(version);

        MesProRouteVersionSnapshotResolver.ResolvedRouteVersionSnapshot resolved = resolver.resolveVersion(11L);

        assertEquals(snapshot, resolved.routeSnapshotJson());
    }

    private MesProRouteVersionDO publishedVersion(Long id, Long routeId, String snapshot) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(routeId)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(snapshot)
                .routeSnapshotSha256(canonicalizer.sha256(snapshot))
                .routeSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION)
                .build();
    }

    private static String snapshot(Long routeId, Long routeProcessId, String processName) {
        return "{\"routeId\":" + routeId + ",\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                + "{\"routeProcessId\":" + routeProcessId + ",\"processId\":501,"
                + "\"routeProcessWorkstationId\":601,\"sort\":1,\"processName\":\"" + processName
                + "\"}]}}}";
    }
}
