package cn.iocoder.yudao.module.mdm.api.companyscope;

import java.util.Collection;
import java.util.Set;

public interface MdmCompanyScopeApi {

    Set<Long> getEnabledCompanyIdsForUser(Long userId);

    void validateUserCompanyAccess(Long userId, Long companyId);

    void validateUserCompanyAccessBatch(Long userId, Collection<Long> companyIds);

    Set<Long> resolveRecipientUserIds(Long companyId, Collection<Long> roleIds, String permission);

}
