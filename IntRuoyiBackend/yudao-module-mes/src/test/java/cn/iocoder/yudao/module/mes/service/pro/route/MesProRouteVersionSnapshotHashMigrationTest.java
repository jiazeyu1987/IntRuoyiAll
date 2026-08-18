package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProRouteVersionSnapshotHashMigrationTest {

    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    @Test
    void canonicalV1_shouldSortKeysNormalizeDecimalsAndKeepArrayOrder() {
        String canonical = canonicalizer.canonicalize("""
                {"z":1.2300,"a":[3,{"beta":2,"a":1}],"zero":-0.0}
                """);

        assertEquals("{\"a\":[3,{\"a\":1,\"beta\":2}],\"z\":1.23,\"zero\":0}", canonical);
        assertEquals(canonicalizer.sha256(canonical), canonicalizer.sha256(
                "{\"zero\":0.000,\"a\":[3,{\"a\":1,\"beta\":2}],\"z\":1.23}"));
    }

    @Test
    void validate_shouldReportInvalidMissingAndDuplicateFrozenIdentity() {
        assertEquals("JSON_INVALID", canonicalizer.validate(9L, "{").blockers().get(0).reasonCode());
        assertEquals("ROUTE_ID_MISMATCH",
                canonicalizer.validate(9L, validSnapshot(8L, 101L)).blockers().get(0).reasonCode());
        assertTrue(canonicalizer.validate(9L,
                "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                        + "{\"processId\":501}]}}}").blockers().stream()
                .anyMatch(blocker -> "ROUTE_PROCESS_ID_MISSING".equals(blocker.reasonCode())));
        assertTrue(canonicalizer.validate(9L,
                "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                        + "{\"routeProcessId\":101,\"processId\":501},"
                        + "{\"routeProcessId\":101,\"processId\":501}]}}}").blockers().stream()
                .anyMatch(blocker -> "ROUTE_PROCESS_ID_DUPLICATE".equals(blocker.reasonCode())));
    }

    @Test
    void backfill_shouldUseStoredJsonOnlyAndLeaveJsonBytesUnchanged() {
        MesProRouteVersionMapper mapper = mock(MesProRouteVersionMapper.class);
        String originalJson = validSnapshot(9L, 101L);
        MesProRouteVersionDO legacy = version(11L, 9L, originalJson, null, null);
        MesProRouteVersionDO persisted = version(11L, 9L, originalJson,
                canonicalizer.sha256(originalJson), MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
        when(mapper.selectAllPhysicalRows()).thenReturn(List.of(legacy), List.of(persisted));
        when(mapper.updateSnapshotIdentityPhysical(any())).thenReturn(1);
        MesProRouteVersionSnapshotHashMigrationService service =
                new MesProRouteVersionSnapshotHashMigrationService(mapper, canonicalizer);

        MesProRouteVersionSnapshotHashMigrationService.MigrationResult result = service.backfillAll();

        assertEquals(1, result.scannedCount());
        assertEquals(1, result.updatedCount());
        assertTrue(result.blockers().isEmpty());
        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(mapper).updateSnapshotIdentityPhysical(captor.capture());
        assertEquals(canonicalizer.sha256(originalJson), captor.getValue().getRouteSnapshotSha256());
        assertEquals(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION,
                captor.getValue().getRouteSnapshotFormatVersion());
        assertEquals(originalJson, legacy.getRouteSnapshotJson());
    }

    @Test
    void backfill_shouldAcceptDraftClientIdentityButKeepPublishedIdentityStrict() {
        String draftJson = "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                + "{\"clientRouteProcessId\":-1,\"processId\":501,\"sort\":1}]}}}";
        MesProRouteVersionDO draft = version(15L, 9L, draftJson, null, null);
        draft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        MesProRouteVersionDO persistedDraft = version(15L, 9L, draftJson,
                canonicalizer.sha256(draftJson), MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
        persistedDraft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        MesProRouteVersionMapper draftMapper = mock(MesProRouteVersionMapper.class);
        when(draftMapper.selectAllPhysicalRows()).thenReturn(List.of(draft), List.of(persistedDraft));
        when(draftMapper.updateSnapshotIdentityPhysical(any())).thenReturn(1);

        MesProRouteVersionSnapshotHashMigrationService.MigrationResult draftResult =
                new MesProRouteVersionSnapshotHashMigrationService(draftMapper, canonicalizer).backfillAll();

        assertTrue(draftResult.blockers().isEmpty());
        assertEquals(1, draftResult.updatedCount());

        MesProRouteVersionDO active = version(16L, 9L, draftJson, null, null);
        active.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE);
        MesProRouteVersionMapper activeMapper = mock(MesProRouteVersionMapper.class);
        when(activeMapper.selectAllPhysicalRows()).thenReturn(List.of(active));

        MesProRouteVersionSnapshotHashMigrationService.MigrationResult activeResult =
                new MesProRouteVersionSnapshotHashMigrationService(activeMapper, canonicalizer).backfillAll();

        assertTrue(activeResult.blockers().stream()
                .anyMatch(blocker -> "ROUTE_PROCESS_ID_MISSING".equals(blocker.reasonCode())));
        verify(activeMapper, never()).updateSnapshotIdentityPhysical(any());

        String positiveClientJson = draftJson.replace("-1", "1");
        MesProRouteVersionDO positiveClientDraft = version(17L, 9L, positiveClientJson, null, null);
        positiveClientDraft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        MesProRouteVersionDO persistedPositiveClientDraft = version(17L, 9L, positiveClientJson,
                canonicalizer.sha256(positiveClientJson), MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
        persistedPositiveClientDraft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        MesProRouteVersionMapper positiveClientMapper = mock(MesProRouteVersionMapper.class);
        when(positiveClientMapper.selectAllPhysicalRows())
                .thenReturn(List.of(positiveClientDraft), List.of(persistedPositiveClientDraft));
        when(positiveClientMapper.updateSnapshotIdentityPhysical(any())).thenReturn(1);
        MesProRouteVersionSnapshotHashMigrationService.MigrationResult positiveClientResult =
                new MesProRouteVersionSnapshotHashMigrationService(positiveClientMapper, canonicalizer).backfillAll();
        assertTrue(positiveClientResult.blockers().stream()
                .anyMatch(blocker -> "CLIENT_ROUTE_PROCESS_ID_INVALID".equals(blocker.reasonCode())));
        verify(positiveClientMapper, never()).updateSnapshotIdentityPhysical(any());

        String duplicateClientJson = "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                + "{\"clientRouteProcessId\":-1,\"processId\":501,\"sort\":1},"
                + "{\"clientRouteProcessId\":-1,\"processId\":502,\"sort\":2}]}}}";
        MesProRouteVersionDO duplicateClientDraft = version(18L, 9L, duplicateClientJson, null, null);
        duplicateClientDraft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        MesProRouteVersionMapper duplicateClientMapper = mock(MesProRouteVersionMapper.class);
        when(duplicateClientMapper.selectAllPhysicalRows()).thenReturn(List.of(duplicateClientDraft));
        MesProRouteVersionSnapshotHashMigrationService.MigrationResult duplicateClientResult =
                new MesProRouteVersionSnapshotHashMigrationService(duplicateClientMapper, canonicalizer).backfillAll();
        assertTrue(duplicateClientResult.blockers().stream()
                .anyMatch(blocker -> "CLIENT_ROUTE_PROCESS_ID_DUPLICATE".equals(blocker.reasonCode())));

        MesProRouteVersionDO superseded = version(19L, 9L, draftJson, null, null);
        superseded.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED);
        MesProRouteVersionMapper supersededMapper = mock(MesProRouteVersionMapper.class);
        when(supersededMapper.selectAllPhysicalRows()).thenReturn(List.of(superseded));
        MesProRouteVersionSnapshotHashMigrationService.MigrationResult supersededResult =
                new MesProRouteVersionSnapshotHashMigrationService(supersededMapper, canonicalizer).backfillAll();
        assertTrue(supersededResult.blockers().stream()
                .anyMatch(blocker -> "ROUTE_PROCESS_ID_MISSING".equals(blocker.reasonCode())));
    }

    @Test
    void backfill_shouldReturnStableBlockersAndWriteNothingWhenAnyRowIsInvalid() {
        MesProRouteVersionMapper mapper = mock(MesProRouteVersionMapper.class);
        MesProRouteVersionDO invalid = version(12L, 9L,
                "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":[]}}}", null, null);
        when(mapper.selectAllPhysicalRows()).thenReturn(List.of(invalid));
        MesProRouteVersionSnapshotHashMigrationService service =
                new MesProRouteVersionSnapshotHashMigrationService(mapper, canonicalizer);

        MesProRouteVersionSnapshotHashMigrationService.MigrationResult result = service.backfillAll();

        assertFalse(result.blockers().isEmpty());
        assertEquals(0, result.updatedCount());
        assertEquals(12L, result.blockers().get(0).routeVersionId());
        verify(mapper, never()).updateSnapshotIdentityPhysical(any());
    }

    @Test
    void backfill_shouldFailWhenIdentityWriteOrPostWriteRecheckFails() {
        String json = validSnapshot(9L, 101L);
        MesProRouteVersionDO legacy = version(13L, 9L, json, null, null);
        MesProRouteVersionMapper noRowMapper = mock(MesProRouteVersionMapper.class);
        when(noRowMapper.selectAllPhysicalRows()).thenReturn(List.of(legacy));
        when(noRowMapper.updateSnapshotIdentityPhysical(any())).thenReturn(0);
        MesProRouteVersionSnapshotHashMigrationService noRowService =
                new MesProRouteVersionSnapshotHashMigrationService(noRowMapper, canonicalizer);
        assertThrows(IllegalStateException.class, noRowService::backfillAll);

        MesProRouteVersionMapper driftMapper = mock(MesProRouteVersionMapper.class);
        MesProRouteVersionDO drifted = version(13L, 9L, json, "0".repeat(64),
                MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
        when(driftMapper.selectAllPhysicalRows()).thenReturn(List.of(legacy), List.of(drifted));
        when(driftMapper.updateSnapshotIdentityPhysical(any())).thenReturn(1);
        MesProRouteVersionSnapshotHashMigrationService driftService =
                new MesProRouteVersionSnapshotHashMigrationService(driftMapper, canonicalizer);
        assertThrows(MesProRouteVersionSnapshotHashMigrationService.MigrationExecutionException.class,
                driftService::backfillAll);
    }

    @Test
    void readiness_shouldExposeMissingIdentityAsConsumerEnforcementBlocker() {
        MesProRouteVersionMapper mapper = mock(MesProRouteVersionMapper.class);
        when(mapper.selectAllPhysicalRows()).thenReturn(List.of(
                version(14L, 9L, validSnapshot(9L, 101L), null, null)));
        MesProRouteVersionSnapshotHashMigrationService service =
                new MesProRouteVersionSnapshotHashMigrationService(mapper, canonicalizer);

        MesProRouteVersionSnapshotHashMigrationService.MigrationResult result = service.readinessAllTenants();

        assertEquals(1, result.blockers().size());
        assertEquals("SNAPSHOT_IDENTITY_MISSING", result.blockers().get(0).reasonCode());
    }

    private MesProRouteVersionDO version(Long id, Long routeId, String snapshot, String hash, String format) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(routeId)
                .versionNo("V1")
                .routeSnapshotJson(snapshot)
                .routeSnapshotSha256(hash)
                .routeSnapshotFormatVersion(format)
                .build();
    }

    private String validSnapshot(Long routeId, Long routeProcessId) {
        return "{\"routeId\":" + routeId + ",\"configSnapshots\":{\"flowGraph\":{\"nodes\":["
                + "{\"routeProcessId\":" + routeProcessId + ",\"processId\":501,\"sort\":1}]}}}";
    }
}
