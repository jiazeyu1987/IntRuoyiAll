package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 活跃订单资料文件的统一预览权限提供者。
 *
 * 该提供者只负责资料文件的在线预览，不把普通订单资料文件伪装成 eDHR 批次附件。
 */
@Service
public class MesActiveOrderDossierBusinessFileAccessProvider implements BusinessFileAccessProvider {

    public static final String PROVIDER_ID = "mes-active-order-dossier";
    public static final String BUSINESS_TYPE = "MES_ACTIVE_ORDER_DOSSIER_FILE";

    private final MesProcessPoolActiveOrderDossierFileMapper dossierFileMapper;
    private final MesActiveOrderDossierFileService dossierFileService;

    public MesActiveOrderDossierBusinessFileAccessProvider(
            MesProcessPoolActiveOrderDossierFileMapper dossierFileMapper,
            MesActiveOrderDossierFileService dossierFileService) {
        this.dossierFileMapper = Objects.requireNonNull(dossierFileMapper, "dossierFileMapper");
        this.dossierFileService = Objects.requireNonNull(dossierFileService, "dossierFileService");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Optional<BusinessFileAccessReference> resolve(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException("资料文件编号必须为正数：" + fileId);
        }
        List<MesProcessPoolActiveOrderDossierFileDO> rows = dossierFileMapper.selectListByFileId(fileId);
        if (rows == null) {
            throw new IllegalStateException("活跃订单资料文件查询结果为空：" + fileId);
        }
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        if (rows.size() != 1) {
            throw new IllegalStateException("活跃订单资料文件归属不唯一：fileId=" + fileId
                    + ", objectCount=" + rows.size());
        }
        MesProcessPoolActiveOrderDossierFileDO row = rows.get(0);
        validateRowIdentity(fileId, row);
        return Optional.of(toReference(row));
    }

    @Override
    public boolean supports(BusinessFileAccessOperation operation) {
        return operation == BusinessFileAccessOperation.PREVIEW
                || operation == BusinessFileAccessOperation.ONLYOFFICE_PREVIEW;
    }

    @Override
    public void assertAllowed(BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        if (request == null || request.operation() == null || request.fileId() == null) {
            throw new IllegalArgumentException("活跃订单资料文件预览请求不完整");
        }
        if (!supports(request.operation())) {
            throw new IllegalArgumentException("活跃订单资料文件不支持该文件操作：" + request.operation());
        }
        requireReference(reference);
        if (request.userId() == null || request.serviceIdentity() != null) {
            throw new IllegalArgumentException("活跃订单资料文件预览必须使用登录用户身份");
        }
        if (!Objects.equals(request.fileId(), referenceFileId(reference))) {
            throw new IllegalArgumentException("活跃订单资料文件请求编号与正式归属不一致");
        }
        MesProcessPoolActiveOrderDossierFileDO live = dossierFileService.requireReadableFile(
                request.userId(), request.fileId());
        BusinessFileAccessReference liveReference = toReference(live);
        if (!Objects.equals(liveReference, reference)) {
            throw new IllegalStateException("资料文件正式归属已变化：" + request.fileId());
        }
    }

    private void requireReference(BusinessFileAccessReference reference) {
        if (reference == null || !PROVIDER_ID.equals(reference.providerId())
                || !BUSINESS_TYPE.equals(reference.businessType())
                || reference.businessId() == null || reference.tenantId() == null
                || isBlank(reference.versionKey())) {
            throw new IllegalArgumentException("活跃订单资料文件正式归属不完整");
        }
    }

    private Long referenceFileId(BusinessFileAccessReference reference) {
        String prefix = "DOSSIER-" + reference.businessId() + "-";
        if (!reference.versionKey().startsWith(prefix)) {
            throw new IllegalArgumentException("活跃订单资料文件正式归属版本不完整");
        }
        String rest = reference.versionKey().substring(prefix.length());
        int separator = rest.indexOf('-');
        if (separator <= 0) {
            throw new IllegalArgumentException("活跃订单资料文件正式归属文件编号缺失");
        }
        try {
            return Long.valueOf(rest.substring(0, separator));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("活跃订单资料文件正式归属文件编号无效", ex);
        }
    }

    private void validateRowIdentity(Long fileId, MesProcessPoolActiveOrderDossierFileDO row) {
        if (row == null || row.getId() == null || row.getId() <= 0
                || row.getActiveOrderId() == null || row.getActiveOrderId() <= 0
                || row.getFileId() == null || !Objects.equals(row.getFileId(), fileId)
                || row.getTenantId() == null || row.getTenantId() <= 0
                || isBlank(row.getCategoryKey()) || isBlank(row.getSha256())) {
            throw new IllegalStateException("活跃订单资料文件正式归属记录不完整：" + fileId);
        }
    }

    private BusinessFileAccessReference toReference(MesProcessPoolActiveOrderDossierFileDO row) {
        validateRowIdentity(row.getFileId(), row);
        return new BusinessFileAccessReference(PROVIDER_ID, BUSINESS_TYPE, row.getId(),
                "DOSSIER-" + row.getId() + "-" + row.getFileId() + "-" + row.getSha256(),
                row.getTenantId(), null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
