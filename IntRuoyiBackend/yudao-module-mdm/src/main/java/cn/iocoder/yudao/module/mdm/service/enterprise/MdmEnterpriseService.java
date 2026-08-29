package cn.iocoder.yudao.module.mdm.service.enterprise;

import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;

import java.util.Collection;
import java.util.List;

public interface MdmEnterpriseService {

    Long createEnterprise(MdmEnterpriseSaveReqVO reqVO);

    void updateEnterprise(MdmEnterpriseSaveReqVO reqVO);

    void updateEnterpriseStatus(Long id, String status);

    void deleteEnterprise(Long id);

    MdmEnterpriseDO getEnterprise(Long id);

    PageResult<MdmEnterpriseDO> getEnterprisePage(MdmEnterprisePageReqVO reqVO);

    List<MdmEnterpriseDO> listSimpleEnterprises(String type, String status, String keyword);

    List<MdmEnterpriseDO> listEnabledEnterprises(Collection<String> allowedTypes, String keyword, int limit);

    List<MdmEnterpriseDO> getEnabledEnterprises(Collection<Long> enterpriseIds,
                                                 Collection<String> allowedTypes);

}
