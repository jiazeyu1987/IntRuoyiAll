package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

import java.util.List;

/** Historical summaries are evidence, never an execution or approval source. */
public final class MesProRouteVersionUsagePolicy {
    private MesProRouteVersionUsagePolicy() { }

    public static boolean isHistoricalSummary(MesProRouteVersionDO version) {
        return version != null && MesRouteLegacySummaryIdentity.isLegacyMarker(
                version.getLifecycleStatus(), version.getRouteSnapshotFormatVersion());
    }

    public static void requireUsable(MesProRouteVersionDO version) {
        if (isHistoricalSummary(version)) {
            throw new IllegalStateException("历史路线摘要不完整，不可用于排产、审批或执行；请正常创建并发布完整新版本：routeVersionId="
                    + version.getId());
        }
    }

    /** A marker alone cannot authorize a new first publication. */
    public static boolean hasKnownArchivedSummary(List<MesProRouteVersionDO> versions) {
        return knownArchivedSummary(versions) != null;
    }

    public static MesProRouteVersionDO knownArchivedSummary(List<MesProRouteVersionDO> versions) {
        MesProRouteVersionDO found = null;
        if (versions == null) return null;
        for (MesProRouteVersionDO version : versions) {
            if (!isHistoricalSummary(version)) continue;
            if (!MesRouteLegacySummaryIdentity.isKnownArchivedIdentity(version)) {
                throw new IllegalStateException("历史路线摘要归档身份不一致：routeVersionId=" + version.getId());
            }
            if (found != null) throw new IllegalStateException("同一路线存在多个历史摘要归档");
            found = version;
        }
        return found;
    }
}
