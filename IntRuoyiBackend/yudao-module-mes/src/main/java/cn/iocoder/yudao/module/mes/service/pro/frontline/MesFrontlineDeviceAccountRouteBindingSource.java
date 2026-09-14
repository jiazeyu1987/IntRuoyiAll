package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

/**
 * Formal source for enabled route bindings of a device or shared operation account.
 */
public interface MesFrontlineDeviceAccountRouteBindingSource {

    List<MesFrontlineDeviceRouteBinding> listEnabledRouteBindings(Long loginUserId);

}
