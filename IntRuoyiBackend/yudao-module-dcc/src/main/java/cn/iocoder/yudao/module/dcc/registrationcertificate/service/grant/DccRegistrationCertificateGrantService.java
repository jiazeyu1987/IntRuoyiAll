package cn.iocoder.yudao.module.dcc.registrationcertificate.service.grant;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID;

@Service
public class DccRegistrationCertificateGrantService {

    private static final String REQUEST_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_TYPE_VIEW_OLD_CERTIFICATE = "VIEW_OLD_CERTIFICATE";
    private static final String REQUEST_TYPE_DOWNLOAD_FILE = "DOWNLOAD_FILE";
    private static final String REQUEST_FILE_STATUS_REQUESTED = "REQUESTED";
    private static final String REQUEST_FILE_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_FILE_STATUS_GRANTED = "GRANTED";
    private static final String GRANT_TYPE_VIEW_OLD_CERTIFICATE = "VIEW_OLD_CERTIFICATE";
    private static final String GRANT_TYPE_DOWNLOAD = "DOWNLOAD";
    private static final String GRANT_STATUS_ACTIVE = "ACTIVE";

    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    private final DccRegistrationCertificateGrantMapper grantMapper;

    public DccRegistrationCertificateGrantService(DccRegistrationCertificateAccessRequestMapper requestMapper,
                                                  DccRegistrationCertificateAccessRequestFileMapper requestFileMapper,
                                                  DccRegistrationCertificateGrantMapper grantMapper) {
        this.requestMapper = require(requestMapper, "requestMapper");
        this.requestFileMapper = require(requestFileMapper, "requestFileMapper");
        this.grantMapper = require(grantMapper, "grantMapper");
    }

    @Transactional(rollbackFor = Exception.class)
    public List<DccRegistrationCertificateGrantDO> createGrantsForApprovedRequest(
            Long tenantId, Long approverId, Long requestId, String approvalKey, LocalDateTime approvedAt) {
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        if (request == null || !tenantId.equals(request.getTenantId())
                || !REQUEST_STATUS_APPROVED.equals(request.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        List<DccRegistrationCertificateGrantDO> existing = grantMapper.selectByRequest(tenantId, requestId);
        if (!existing.isEmpty()) {
            return existing;
        }
        if (REQUEST_TYPE_VIEW_OLD_CERTIFICATE.equals(request.getRequestType())) {
            return List.of(createGrant(tenantId, request, null, approverId, grantKey(approvalKey, "VIEW", requestId),
                    GRANT_TYPE_VIEW_OLD_CERTIFICATE, approvedAt));
        }
        if (!REQUEST_TYPE_DOWNLOAD_FILE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        List<DccRegistrationCertificateAccessRequestFileDO> requestFiles =
                requestFileMapper.selectByRequestId(tenantId, requestId);
        if (requestFiles.isEmpty() || requestFiles.stream()
                .anyMatch(file -> !Boolean.TRUE.equals(file.getDownloadRequested())
                        || !isDownloadGrantable(file.getStatus()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        return requestFiles.stream()
                .map(file -> {
                    DccRegistrationCertificateGrantDO grant = createGrant(tenantId, request, file, approverId,
                            grantKey(approvalKey, "DOWNLOAD", file.getId()), GRANT_TYPE_DOWNLOAD, approvedAt);
                    file.setStatus(REQUEST_FILE_STATUS_GRANTED);
                    requestFileMapper.updateById(file);
                    return grant;
                })
                .toList();
    }

    private static boolean isDownloadGrantable(String status) {
        return REQUEST_FILE_STATUS_REQUESTED.equals(status) || REQUEST_FILE_STATUS_APPROVED.equals(status);
    }

    private DccRegistrationCertificateGrantDO createGrant(
            Long tenantId, DccRegistrationCertificateAccessRequestDO request,
            DccRegistrationCertificateAccessRequestFileDO requestFile, Long approverId,
            String grantKey, String grantType, LocalDateTime approvedAt) {
        DccRegistrationCertificateGrantDO grant = DccRegistrationCertificateGrantDO.builder()
                .requestId(request.getId())
                .requestFileId(requestFile == null ? null : requestFile.getId())
                .ownerCompanyId(request.getOwnerCompanyId())
                .certificateId(request.getCertificateId())
                .businessFileId(requestFile == null ? null : requestFile.getBusinessFileId())
                .granteeUserId(request.getRequesterUserId())
                .grantType(grantType)
                .grantKey(grantKey)
                .status(GRANT_STATUS_ACTIVE)
                .grantedAt(approvedAt)
                .expiresAt(approvedAt.plusHours(24))
                .detailJson(JsonUtils.toJsonString(Map.of(
                        "approvalKey", grantKey,
                        "approvedBy", approverId
                )))
                .build();
        grant.setTenantId(tenantId);
        try {
            grantMapper.insert(grant);
            return grant;
        } catch (DuplicateKeyException ex) {
            DccRegistrationCertificateGrantDO existing =
                    grantMapper.selectByTenantAndGrantKey(tenantId, grantKey);
            if (existing != null) {
                return existing;
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_CONFLICT);
        }
    }

    private static String grantKey(String approvalKey, String scope, Long id) {
        if (approvalKey == null || approvalKey.trim().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_CONFLICT);
        }
        return approvalKey.trim() + ":" + scope + ":" + id;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
