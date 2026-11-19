package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

/** Permission facts are reused only during a single tenant-bound GET, never during writes. */
public final class DccReadRequestSnapshot {
    private DccReadRequestSnapshot() {}

    public static <T> T read(String fact, Supplier<T> loader) {
        Long tenant = TenantContextHolder.getTenantId();
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request)
                || !"GET".equals(request.getRequest().getMethod()) || tenant == null || TenantContextHolder.isIgnore()) {
            return loader.get();
        }
        String key = DccReadRequestSnapshot.class.getName() + "." + tenant + "." + fact;
        if (request.getAttribute(key, RequestAttributes.SCOPE_REQUEST) instanceof Value<?> cached) {
            @SuppressWarnings("unchecked") T value = (T) cached.value();
            return value;
        }
        T value = loader.get();
        request.setAttribute(key, new Value<>(value), RequestAttributes.SCOPE_REQUEST);
        return value;
    }

    private record Value<T>(T value) {}
}
