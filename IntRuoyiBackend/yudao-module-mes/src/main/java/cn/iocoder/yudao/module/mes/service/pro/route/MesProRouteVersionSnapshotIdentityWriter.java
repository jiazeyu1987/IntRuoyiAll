package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;

/**
 * Single write contract for route-version snapshot identity columns.
 */
public final class MesProRouteVersionSnapshotIdentityWriter {

    private static final MesProRouteSnapshotCanonicalizer CANONICALIZER =
            new MesProRouteSnapshotCanonicalizer();

    private MesProRouteVersionSnapshotIdentityWriter() {
    }

    /** Keeps an explicitly unbound frozen SOP distinct from an absent snapshot field. */
    public static String toSnapshotJson(Object snapshot) {
        return JSON.toJSONString(snapshot,
                (PropertyFilter) (source, name, value) -> value != null || "sopFileUrl".equals(name),
                SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect);
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
