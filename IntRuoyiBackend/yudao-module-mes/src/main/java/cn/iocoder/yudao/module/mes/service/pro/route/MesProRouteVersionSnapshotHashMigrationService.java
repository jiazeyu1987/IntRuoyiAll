package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MesProRouteVersionSnapshotHashMigrationService {

    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProRouteSnapshotCanonicalizer canonicalizer;

    public MesProRouteVersionSnapshotHashMigrationService(MesProRouteVersionMapper routeVersionMapper,
                                                          MesProRouteSnapshotCanonicalizer canonicalizer) {
        this.routeVersionMapper = routeVersionMapper;
        this.canonicalizer = canonicalizer;
    }

    @Transactional(rollbackFor = Exception.class)
    public MigrationResult backfillAll() {
        AtomicReference<MigrationResult> result = new AtomicReference<>();
        TenantUtils.executeIgnore(() -> result.set(backfillAllTenants()));
        return result.get();
    }

    private MigrationResult backfillAllTenants() {
        List<MesProRouteVersionDO> versions = new ArrayList<>(routeVersionMapper.selectAllPhysicalRows());
        versions.sort(Comparator.comparing(MesProRouteVersionDO::getId,
                Comparator.nullsLast(Long::compareTo)));
        List<MigrationBlocker> blockers = validateVersions(versions, false);
        if (!blockers.isEmpty()) {
            return new MigrationResult(versions.size(), 0, List.copyOf(blockers));
        }
        int updatedCount = 0;
        for (MesProRouteVersionDO version : versions) {
            if (version.getRouteSnapshotSha256() != null
                    && version.getRouteSnapshotFormatVersion() != null) {
                continue;
            }
            MesProRouteVersionDO update = new MesProRouteVersionDO();
            update.setId(version.getId());
            update.setRouteSnapshotSha256(canonicalizer.sha256(version.getRouteSnapshotJson()));
            update.setRouteSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
            int affectedRows = routeVersionMapper.updateSnapshotIdentityPhysical(update);
            if (affectedRows != 1) {
                throw new IllegalStateException("route snapshot backfill affected rows=" + affectedRows
                        + ", routeVersionId=" + version.getId());
            }
            updatedCount++;
        }
        List<MesProRouteVersionDO> persisted = new ArrayList<>(routeVersionMapper.selectAllPhysicalRows());
        persisted.sort(Comparator.comparing(MesProRouteVersionDO::getId,
                Comparator.nullsLast(Long::compareTo)));
        List<MigrationBlocker> recheckBlockers = validateVersions(persisted, true);
        if (persisted.size() != versions.size()) {
            recheckBlockers.add(new MigrationBlocker(null, null, null, "ROW_COUNT_CHANGED",
                    "before=" + versions.size() + ", after=" + persisted.size()));
        }
        if (!recheckBlockers.isEmpty()) {
            throw new MigrationExecutionException(new MigrationResult(
                    persisted.size(), updatedCount, List.copyOf(recheckBlockers)));
        }
        return new MigrationResult(persisted.size(), updatedCount, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public MigrationResult backfillAllOrThrow() {
        MigrationResult result = backfillAll();
        if (!result.blockers().isEmpty()) {
            throw new IllegalStateException("route snapshot migration blocked: " + result.blockers());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public MigrationResult readinessAllTenants() {
        AtomicReference<MigrationResult> result = new AtomicReference<>();
        TenantUtils.executeIgnore(() -> {
            List<MesProRouteVersionDO> versions = new ArrayList<>(routeVersionMapper.selectAllPhysicalRows());
            versions.sort(Comparator.comparing(MesProRouteVersionDO::getId,
                    Comparator.nullsLast(Long::compareTo)));
            result.set(new MigrationResult(versions.size(), 0,
                    List.copyOf(validateVersions(versions, true))));
        });
        return result.get();
    }

    private List<MigrationBlocker> validateVersions(List<MesProRouteVersionDO> versions,
                                                    boolean requireIdentity) {
        List<MigrationBlocker> blockers = new ArrayList<>();
        for (MesProRouteVersionDO version : versions) {
            MesProRouteSnapshotCanonicalizer.Validation validation =
                    isCandidateLifecycle(version.getLifecycleStatus())
                            ? canonicalizer.validateCandidate(version.getRouteId(), version.getRouteSnapshotJson())
                            : canonicalizer.validate(version.getRouteId(), version.getRouteSnapshotJson());
            for (MesProRouteSnapshotCanonicalizer.Blocker blocker : validation.blockers()) {
                blockers.add(new MigrationBlocker(version.getId(), version.getRouteId(), version.getVersionNo(),
                        blocker.reasonCode(), blocker.detail()));
            }
            if (validation.ready()) {
                validateExistingIdentity(version, blockers, requireIdentity);
            }
        }
        blockers.sort(Comparator
                .comparing(MigrationBlocker::routeVersionId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(MigrationBlocker::reasonCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(MigrationBlocker::detail, Comparator.nullsLast(String::compareTo)));
        return blockers;
    }

    private boolean isCandidateLifecycle(String lifecycleStatus) {
        return MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(lifecycleStatus)
                || MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL.equals(lifecycleStatus)
                || MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH.equals(lifecycleStatus)
                || MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(lifecycleStatus)
                || MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED.equals(lifecycleStatus);
    }

    private void validateExistingIdentity(MesProRouteVersionDO version, List<MigrationBlocker> blockers,
                                          boolean requireIdentity) {
        String hash = version.getRouteSnapshotSha256();
        String formatVersion = version.getRouteSnapshotFormatVersion();
        if ((hash == null) != (formatVersion == null)) {
            blockers.add(new MigrationBlocker(version.getId(), version.getRouteId(), version.getVersionNo(),
                    "SNAPSHOT_IDENTITY_PARTIAL", "hash and format version must be populated together"));
            return;
        }
        if (hash == null) {
            if (requireIdentity) {
                blockers.add(new MigrationBlocker(version.getId(), version.getRouteId(), version.getVersionNo(),
                        "SNAPSHOT_IDENTITY_MISSING", "hash and format version are required"));
            }
            return;
        }
        if (!MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(formatVersion)) {
            blockers.add(new MigrationBlocker(version.getId(), version.getRouteId(), version.getVersionNo(),
                    "SNAPSHOT_FORMAT_UNSUPPORTED", "formatVersion=" + formatVersion));
            return;
        }
        String calculated = canonicalizer.sha256(version.getRouteSnapshotJson());
        if (!calculated.equals(hash)) {
            blockers.add(new MigrationBlocker(version.getId(), version.getRouteId(), version.getVersionNo(),
                    "SNAPSHOT_HASH_MISMATCH", "stored=" + hash + ", calculated=" + calculated));
        }
    }

    public record MigrationBlocker(Long routeVersionId, Long routeId, String versionNo,
                                   String reasonCode, String detail) {
    }

    public record MigrationResult(int scannedCount, int updatedCount, List<MigrationBlocker> blockers) {
    }

    public static final class MigrationExecutionException extends IllegalStateException {

        private final MigrationResult result;

        public MigrationExecutionException(MigrationResult result) {
            super("route snapshot migration recheck failed: " + result.blockers());
            this.result = result;
        }

        public MigrationResult getResult() {
            return result;
        }
    }
}
