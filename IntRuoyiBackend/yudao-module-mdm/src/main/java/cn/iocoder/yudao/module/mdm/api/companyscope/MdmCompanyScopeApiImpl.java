package cn.iocoder.yudao.module.mdm.api.companyscope;

import cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
public class MdmCompanyScopeApiImpl implements MdmCompanyScopeApi {

    @Resource
    private MdmCompanyScopeService companyScopeService;

    @Override
    public Set<Long> getEnabledCompanyIdsForUser(Long userId) {
        return companyScopeService.getEnabledCompanyIdsForUser(userId);
    }

    @Override
    public void validateUserCompanyAccess(Long userId, Long companyId) {
        companyScopeService.validateUserCompanyAccess(userId, companyId);
    }

    @Override
    public void validateUserCompanyAccessBatch(Long userId, Collection<Long> companyIds) {
        companyScopeService.validateUserCompanyAccessBatch(userId, companyIds);
    }

    @Override
    public Set<Long> resolveRecipientUserIds(Long companyId, Collection<Long> roleIds, String permission) {
        return companyScopeService.resolveRecipientUserIds(companyId, roleIds, permission);
    }

}
