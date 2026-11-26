package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;

public interface DccRegistrationCertificateQueryService {

    class ExportLimitExceeded extends RuntimeException {
        public ExportLimitExceeded() { super("超出同步导出上限，请缩小导出范围"); }
    }

    java.util.List<DccRegistrationCertificatePageItem> getExportRows(Long tenantId, Long actorId,
            DccRegistrationCertificatePageQuery query, java.util.List<Long> versionIds, int limit);

    DccRegistrationCertificatePageItem getExportRow(Long tenantId, Long actorId, Long versionId);

    PageResult<DccRegistrationCertificatePageItem> getPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext);

    DccRegistrationCertificateDetail getDetail(
            Long tenantId, Long actorId, Long certificateId, Long versionId, DccRequestAuditContext auditContext);

    PageResult<DccRegistrationCertificateOldIndexItem> getOldIndexPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext);
}
