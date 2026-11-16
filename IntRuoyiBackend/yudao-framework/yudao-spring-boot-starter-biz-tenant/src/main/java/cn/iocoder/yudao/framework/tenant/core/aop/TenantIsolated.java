package cn.iocoder.yudao.framework.tenant.core.aop;

import java.lang.annotation.*;

/** Keep tenant ownership enforced even when the surrounding business call ignores tenant filtering. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TenantIsolated {
}
