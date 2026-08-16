package cn.iocoder.yudao.module.mdm.service.enterprise;

import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;

import java.util.Collection;
import java.util.List;

public interface MdmEnterpriseService {

    Long createEnterprise(MdmEnterpriseSaveReqVO reqVO);

    List<MdmEnterpriseDO> getEnabledEnterprises(Collection<Long> enterpriseIds,
                                                 Collection<String> allowedTypes);

}
