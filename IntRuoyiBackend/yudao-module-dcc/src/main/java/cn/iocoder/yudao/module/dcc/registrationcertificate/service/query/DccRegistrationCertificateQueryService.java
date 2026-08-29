package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;

public interface DccRegistrationCertificateQueryService {

    PageResult<DccRegistrationCertificatePageItem> getPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext);

    DccRegistrationCertificateDetail getDetail(
            Long tenantId, Long actorId, Long certificateId, Long versionId, DccRequestAuditContext auditContext);

    PageResult<DccRegistrationCertificateOldIndexItem> getOldIndexPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext);
}
