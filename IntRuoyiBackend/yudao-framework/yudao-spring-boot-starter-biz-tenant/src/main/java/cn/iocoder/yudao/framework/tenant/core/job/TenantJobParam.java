package cn.iocoder.yudao.framework.tenant.core.job;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@link TenantJob} 的显式租户范围参数。
 *
 * 带范围的参数仅供手动触发任务使用；未带范围的定时任务仍由 {@link TenantJobAspect} 遍历全部启用租户。
 */
public record TenantJobParam(Long tenantId, String handlerParam) {

    private static final String SCOPE_PREFIX = "__TENANT_JOB_SCOPE__:";
    private static final String SEPARATOR = ":";

    public static String forTenant(Long tenantId, String handlerParam) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant job scope requires a positive tenant id");
        }
        String encodedHandlerParam = handlerParam == null ? "-" : "+" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(handlerParam.getBytes(StandardCharsets.UTF_8));
        return SCOPE_PREFIX + tenantId + SEPARATOR + encodedHandlerParam;
    }

    /**
     * 解析任务参数。未携带显式范围时返回 {@code null}，以保留定时任务的全部租户语义。
     */
    public static TenantJobParam parse(String param) {
        if (param == null || !param.startsWith(SCOPE_PREFIX)) {
            return null;
        }
        String payload = param.substring(SCOPE_PREFIX.length());
        int separatorIndex = payload.indexOf(SEPARATOR);
        if (separatorIndex <= 0) {
            throw new IllegalArgumentException("Malformed tenant job scope parameter");
        }
        Long tenantId;
        try {
            tenantId = Long.valueOf(payload.substring(0, separatorIndex));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Malformed tenant job scope tenant id", ex);
        }
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant job scope requires a positive tenant id");
        }
        String encodedHandlerParam = payload.substring(separatorIndex + SEPARATOR.length());
        if ("-".equals(encodedHandlerParam)) {
            return new TenantJobParam(tenantId, null);
        }
        if (!encodedHandlerParam.startsWith("+")) {
            throw new IllegalArgumentException("Malformed tenant job scope handler parameter");
        }
        try {
            String handlerParam = new String(Base64.getUrlDecoder().decode(
                    encodedHandlerParam.substring(1)), StandardCharsets.UTF_8);
            return new TenantJobParam(tenantId, handlerParam);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Malformed tenant job scope handler parameter", ex);
        }
    }

}
