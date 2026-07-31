package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineEmployeeTemplateBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline.MesFrontlineEmployeeTemplateBindingMapper;
import org.springframework.stereotype.Service;

@Service
public class MesFrontlineTemplateBindingSourceImpl implements MesFrontlineTemplateBindingSource {

    private final MesFrontlineEmployeeTemplateBindingMapper templateBindingMapper;

    public MesFrontlineTemplateBindingSourceImpl(
            MesFrontlineEmployeeTemplateBindingMapper templateBindingMapper) {
        this.templateBindingMapper = templateBindingMapper;
    }

    @Override
    public MesFrontlineTemplateDescriptor findTemplate(MesFrontlineTemplateRequest request) {
        MesFrontlineEmployeeTemplateBindingDO binding = templateBindingMapper.selectEnabledByContext(
                request.actualEmployeeId(), request.routeProcessId(), request.processId());
        if (binding == null) {
            return null;
        }
        return new MesFrontlineTemplateDescriptor(binding.getTemplateNo(), binding.getTemplateType(),
                binding.getRouteProcessId(), binding.getProcessId(), binding.getActualEmployeeId());
    }
}
