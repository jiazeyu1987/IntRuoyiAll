package cn.iocoder.yudao.module.mdm.api.enterprise;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

@Service
@Validated
public class MdmEnterpriseApiImpl implements MdmEnterpriseApi {

    @Resource
    private MdmEnterpriseService enterpriseService;

    @Override
    public List<MdmEnterpriseRespDTO> getEnabledEnterprises(Collection<Long> enterpriseIds,
                                                             Collection<String> allowedTypes) {
        return BeanUtils.toBean(enterpriseService.getEnabledEnterprises(enterpriseIds, allowedTypes),
                MdmEnterpriseRespDTO.class);
    }

}
