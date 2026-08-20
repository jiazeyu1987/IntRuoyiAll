package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

/**
 * Single write contract for route-version snapshot identity columns.
 */
public final class MesProRouteVersionSnapshotIdentityWriter {

    private static final MesProRouteSnapshotCanonicalizer CANONICALIZER =
            new MesProRouteSnapshotCanonicalizer();

    private MesProRouteVersionSnapshotIdentityWriter() {
    }

    public static void apply(MesProRouteVersionDO target, String routeSnapshotJson) {
        if (target == null) {
            throw new IllegalArgumentException("route version target is required");
        }
        if (StrUtil.isBlank(routeSnapshotJson)) {
            throw new IllegalArgumentException("route version snapshot is required");
        }
        target.setRouteSnapshotJson(routeSnapshotJson);
        target.setRouteSnapshotSha256(CANONICALIZER.sha256(routeSnapshotJson));
        target.setRouteSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
    }
}
