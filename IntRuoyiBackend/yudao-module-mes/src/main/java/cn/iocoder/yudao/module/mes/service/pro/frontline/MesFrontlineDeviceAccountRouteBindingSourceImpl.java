package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineDeviceAccountRouteBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline.MesFrontlineDeviceAccountRouteBindingMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesFrontlineDeviceAccountRouteBindingSourceImpl
        implements MesFrontlineDeviceAccountRouteBindingSource {

    private final MesFrontlineDeviceAccountRouteBindingMapper routeBindingMapper;

    public MesFrontlineDeviceAccountRouteBindingSourceImpl(
            MesFrontlineDeviceAccountRouteBindingMapper routeBindingMapper) {
        this.routeBindingMapper = routeBindingMapper;
    }

    @Override
    public List<MesFrontlineDeviceRouteBinding> listEnabledRouteBindings(Long loginUserId) {
        return routeBindingMapper.selectEnabledListByDeviceAccountUserId(loginUserId).stream()
                .map(this::toBinding)
                .toList();
    }

    private MesFrontlineDeviceRouteBinding toBinding(MesFrontlineDeviceAccountRouteBindingDO binding) {
        return new MesFrontlineDeviceRouteBinding(
                binding.getDeviceAccountUserId(),
                binding.getRouteId(),
                null,
                null,
                binding.getDeviceId(),
                null,
                null,
                binding.getWorkstationId(),
                null,
                null);
    }
}
