package cn.iocoder.yudao.module.mes.service.pro.schedule.identity;

/**
 * 路线工序身份。
 *
 * <p>用于路线工序配置、工作台在制统计和跨路线隔离。基础工序 {@code processId}
 * 只能作为能力匹配辅助身份，不能替代 {@code routeVersionId + routeProcessId}。</p>
 */
public record RouteProcessIdentity(Long routeId, Long routeVersionId, Long routeProcessId) {

    private static final String ROUTE_PROCESS_PREFIX = "ROUTE_PROCESS_";

    public static RouteProcessIdentity of(Long routeId, Long routeVersionId, Long routeProcessId) {
        return new RouteProcessIdentity(routeId, routeVersionId, routeProcessId);
    }

    public String availabilityKey() {
        return availabilityKey(routeProcessId);
    }

    public static String availabilityKey(Long routeProcessId) {
        return ROUTE_PROCESS_PREFIX + routeProcessId;
    }

    public static String legacyAvailabilityKey(Long routeId, Long processId) {
        return ROUTE_PROCESS_PREFIX + routeId + "_" + processId;
    }

}
