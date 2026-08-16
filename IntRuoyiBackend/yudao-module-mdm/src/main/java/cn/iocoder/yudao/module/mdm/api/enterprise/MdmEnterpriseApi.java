package cn.iocoder.yudao.module.mdm.api.enterprise;

import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;

import java.util.Collection;
import java.util.List;

public interface MdmEnterpriseApi {

    List<MdmEnterpriseRespDTO> getEnabledEnterprises(Collection<Long> enterpriseIds,
                                                      Collection<String> allowedTypes);

}
