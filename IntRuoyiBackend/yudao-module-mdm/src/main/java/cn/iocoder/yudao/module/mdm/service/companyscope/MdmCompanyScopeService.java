package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmRoleCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmUserCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeRespVO;

import java.util.Collection;
import java.util.Set;

public interface MdmCompanyScopeService {

    PageResult<MdmCompanyScopeRespVO> getCompanyScopePage(MdmCompanyScopePageReqVO reqVO);

    Long createUserCompanyScope(MdmUserCompanyScopeCreateReqDTO reqDTO);

    Long createRoleCompanyScope(MdmRoleCompanyScopeCreateReqDTO reqDTO);

    Set<Long> getEnabledCompanyIdsForUser(Long userId);

    void validateUserCompanyAccess(Long userId, Long companyId);

    void validateUserCompanyAccessBatch(Long userId, Collection<Long> companyIds);

    Set<Long> resolveRecipientUserIds(Long companyId, Collection<Long> roleIds, String permission);

}
