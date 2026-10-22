package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * eDHR 不合格评审材料的统一在线预览权限提供者。
 */
@Service
public class MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider implements BusinessFileAccessProvider {

    public static final String PROVIDER_ID = "mes-edhr-nonconformance-review-material";
    public static final String BUSINESS_TYPE = "MES_EDHR_NONCONFORMANCE_REVIEW_MATERIAL";

    private final MesProEdhrNonconformanceReviewMapper reviewMapper;

    public MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider(
            MesProEdhrNonconformanceReviewMapper reviewMapper) {
        this.reviewMapper = Objects.requireNonNull(reviewMapper, "reviewMapper");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Optional<BusinessFileAccessReference> resolve(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException("不合格评审材料文件编号必须为正数：" + fileId);
        }
        List<MesProEdhrNonconformanceReviewDO> rows = reviewMapper.selectListByReviewMaterialFileId(fileId);
        if (rows == null) {
            throw new IllegalStateException("不合格评审材料查询结果为空：" + fileId);
        }
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        if (rows.size() != 1) {
            throw new IllegalStateException("不合格评审材料归属不唯一：fileId=" + fileId
                    + ", objectCount=" + rows.size());
        }
        MesProEdhrNonconformanceReviewDO row = rows.get(0);
        validateRowIdentity(fileId, row);
        return Optional.of(toReference(fileId, row));
    }

    @Override
    public boolean supports(BusinessFileAccessOperation operation) {
        return operation == BusinessFileAccessOperation.PREVIEW
                || operation == BusinessFileAccessOperation.ONLYOFFICE_PREVIEW;
    }

    @Override
    public void assertAllowed(BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        if (request == null || request.operation() == null || request.fileId() == null) {
            throw new IllegalArgumentException("不合格评审材料预览请求不完整");
        }
        if (!supports(request.operation())) {
            throw new IllegalArgumentException("不合格评审材料不支持该文件操作：" + request.operation());
        }
        requireReference(reference);
        if (request.userId() == null || request.serviceIdentity() != null) {
            throw new IllegalArgumentException("不合格评审材料预览必须使用登录用户身份");
        }
        if (!Objects.equals(request.fileId(), referenceFileId(reference))) {
            throw new IllegalArgumentException("不合格评审材料请求编号与正式归属不一致");
        }
        BusinessFileAccessReference liveReference = resolve(request.fileId()).orElseThrow(
                () -> new IllegalStateException("不合格评审材料正式归属已缺失：" + request.fileId()));
        if (!Objects.equals(liveReference, reference)) {
            throw new IllegalStateException("不合格评审材料正式归属已变化：" + request.fileId());
        }
    }

    private void requireReference(BusinessFileAccessReference reference) {
        if (reference == null || !PROVIDER_ID.equals(reference.providerId())
                || !BUSINESS_TYPE.equals(reference.businessType())
                || reference.businessId() == null || reference.tenantId() == null
                || isBlank(reference.versionKey())) {
            throw new IllegalArgumentException("不合格评审材料正式归属不完整");
        }
    }

    private Long referenceFileId(BusinessFileAccessReference reference) {
        String prefix = "NCR-MATERIAL-" + reference.businessId() + "-";
        if (!reference.versionKey().startsWith(prefix)) {
            throw new IllegalArgumentException("不合格评审材料正式归属版本不完整");
        }
        String rest = reference.versionKey().substring(prefix.length());
        int separator = rest.indexOf('-');
        if (separator <= 0) {
            throw new IllegalArgumentException("不合格评审材料正式归属文件编号缺失");
        }
        try {
            return Long.valueOf(rest.substring(0, separator));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("不合格评审材料正式归属文件编号无效", ex);
        }
    }

    private void validateRowIdentity(Long fileId, MesProEdhrNonconformanceReviewDO row) {
        if (row == null || row.getId() == null || row.getId() <= 0
                || row.getReviewMaterialFileId() == null
                || row.getActiveOrderId() == null || row.getActiveOrderId() <= 0
                || row.getTenantId() == null || row.getTenantId() <= 0
                || isBlank(row.getReviewMaterialUrl())
                || isBlank(row.getReviewStatus())
                || !MesProEdhrNonconformanceReviewService.STATUS_CLOSED.equals(row.getReviewStatus())) {
            throw new IllegalStateException("不合格评审材料正式归属记录不完整：" + fileId);
        }
        if (isBlank(row.getReviewMaterialsJson())) {
            if (!Objects.equals(row.getReviewMaterialFileId(), fileId)) {
                throw new IllegalStateException("历史评审材料编号与正式归属不一致：" + fileId);
            }
            return;
        }
        JsonNode materials = JsonUtils.parseTree(row.getReviewMaterialsJson()).get("activeMaterials");
        if (materials == null || !materials.isArray()) {
            throw new IllegalStateException("不合格评审材料清单格式无效：" + row.getId());
        }
        int matches = 0;
        for (JsonNode material : materials) {
            JsonNode id = material.get("fileId");
            boolean sameId = id != null && ((id.isIntegralNumber() && id.canConvertToLong()
                    && id.longValue() == fileId) || (id.isTextual() && fileId.toString().equals(id.textValue())));
            if (sameId) {
                if (material.path("url").asText().isBlank()) {
                    throw new IllegalStateException("不合格评审材料路径缺失：" + fileId);
                }
                matches++;
            }
        }
        if (matches != 1) {
            throw new IllegalStateException("不合格评审材料未唯一列入有效清单：" + fileId);
        }
    }

    private BusinessFileAccessReference toReference(Long fileId, MesProEdhrNonconformanceReviewDO row) {
        String versionKey = "NCR-MATERIAL-" + row.getId() + "-" + fileId
                + "-" + row.getActiveOrderId() + "-" + normalizeVersionTail(row);
        return new BusinessFileAccessReference(PROVIDER_ID, BUSINESS_TYPE, row.getId(),
                versionKey, row.getTenantId(), null);
    }

    private String normalizeVersionTail(MesProEdhrNonconformanceReviewDO row) {
        return row.getUpdateTime() == null ? "NO_UPDATE_TIME" : row.getUpdateTime().toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
