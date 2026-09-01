package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED;

@Service
public class DccRegistrationCertificateRecipientResolver {

    private final MdmCompanyScopeApi companyScopeApi;

    public DccRegistrationCertificateRecipientResolver(MdmCompanyScopeApi companyScopeApi) {
        if (companyScopeApi == null) {
            throw new IllegalArgumentException("公司范围服务不能为空");
        }
        this.companyScopeApi = companyScopeApi;
    }

    public List<DccRegistrationCertificateRecipient> resolve(Long ownerCompanyId, Collection<Long> roleIds,
                                                             String permission) {
        if (ownerCompanyId == null || ownerCompanyId <= 0 || roleIds == null || roleIds.isEmpty()
                || permission == null || permission.trim().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
        }
        LinkedHashSet<Long> normalizedRoleIds = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            if (roleId == null || roleId <= 0) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
            }
            normalizedRoleIds.add(roleId);
        }
        Set<Long> userIds = companyScopeApi.resolveRecipientUserIds(
                ownerCompanyId, List.copyOf(normalizedRoleIds), permission.trim());
        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
        }
        List<DccRegistrationCertificateRecipient> recipients = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED);
            }
            recipients.add(new DccRegistrationCertificateRecipient(userId, ownerCompanyId));
        }
        return List.copyOf(recipients);
    }
}
