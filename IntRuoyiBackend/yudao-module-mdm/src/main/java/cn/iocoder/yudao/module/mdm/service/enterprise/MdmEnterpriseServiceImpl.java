package cn.iocoder.yudao.module.mdm.service.enterprise;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.enterprise.MdmEnterpriseMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_EMPTY;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_RESULT_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DELETED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TYPE_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TYPE_MISMATCH;

@Service
@Validated
public class MdmEnterpriseServiceImpl implements MdmEnterpriseService {

    @Resource
    private MdmEnterpriseMapper enterpriseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEnterprise(MdmEnterpriseSaveReqVO reqVO) {
        if (reqVO == null) {
            throw exception(MDM_ENTERPRISE_FIELD_REQUIRED, "request");
        }
        String enterpriseCode = requireText(reqVO.getEnterpriseCode(), "enterpriseCode");
        String name = requireText(reqVO.getName(), "name");
        String type = requireType(reqVO.getType());
        String status = requireStatus(reqVO.getStatus());
        MdmEnterpriseDO enterprise = MdmEnterpriseDO.builder()
                .enterpriseCode(enterpriseCode)
                .name(name)
                .type(type)
                .status(status)
                .revision(1)
                .build();
        try {
            enterpriseMapper.insert(enterprise);
        } catch (DuplicateKeyException exception) {
            throw exception(MDM_ENTERPRISE_CODE_DUPLICATE);
        }
        return enterprise.getId();
    }

    @Override
    public List<MdmEnterpriseDO> getEnabledEnterprises(Collection<Long> enterpriseIds,
                                                       Collection<String> allowedTypes) {
        List<Long> requestedIds = validateEnterpriseIds(enterpriseIds);
        Set<String> requiredTypes = validateAllowedTypes(allowedTypes);
        List<MdmEnterpriseDO> enterprises = enterpriseMapper.selectList(
                new LambdaQueryWrapperX<MdmEnterpriseDO>().in(MdmEnterpriseDO::getId, requestedIds));
        Map<Long, MdmEnterpriseDO> enterprisesById = indexResults(enterprises);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<MdmEnterpriseDO> orderedResult = new ArrayList<>(requestedIds.size());
        for (Long enterpriseId : requestedIds) {
            MdmEnterpriseDO enterprise = enterprisesById.get(enterpriseId);
            if (enterprise == null) {
                throw exception(MDM_ENTERPRISE_NOT_FOUND, enterpriseId);
            }
            validateFormalEnterprise(enterprise, tenantId, requiredTypes);
            orderedResult.add(enterprise);
        }
        return orderedResult;
    }

    private List<Long> validateEnterpriseIds(Collection<Long> enterpriseIds) {
        if (enterpriseIds == null || enterpriseIds.isEmpty()) {
            throw exception(MDM_ENTERPRISE_BATCH_EMPTY);
        }
        List<Long> requestedIds = new ArrayList<>(enterpriseIds);
        if (requestedIds.stream().anyMatch(Objects::isNull)) {
            throw exception(MDM_ENTERPRISE_NOT_FOUND, "null");
        }
        if (new LinkedHashSet<>(requestedIds).size() != requestedIds.size()) {
            throw exception(MDM_ENTERPRISE_BATCH_DUPLICATE);
        }
        return requestedIds;
    }

    private Set<String> validateAllowedTypes(Collection<String> allowedTypes) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            throw exception(MDM_ENTERPRISE_TYPE_MISMATCH, "allowedTypes");
        }
        Set<String> validatedTypes = new LinkedHashSet<>();
        for (String allowedType : allowedTypes) {
            String normalizedType = StrUtil.trimToNull(allowedType);
            if (!MdmEnterpriseTypeEnum.isValid(normalizedType)) {
                throw exception(MDM_ENTERPRISE_TYPE_MISMATCH, allowedType);
            }
            validatedTypes.add(normalizedType);
        }
        return validatedTypes;
    }

    private Map<Long, MdmEnterpriseDO> indexResults(List<MdmEnterpriseDO> enterprises) {
        if (enterprises == null) {
            throw exception(MDM_ENTERPRISE_BATCH_RESULT_INVALID);
        }
        Map<Long, MdmEnterpriseDO> result = new LinkedHashMap<>();
        for (MdmEnterpriseDO enterprise : enterprises) {
            if (enterprise == null || enterprise.getId() == null
                    || result.putIfAbsent(enterprise.getId(), enterprise) != null) {
                throw exception(MDM_ENTERPRISE_BATCH_RESULT_INVALID);
            }
        }
        return result;
    }

    private void validateFormalEnterprise(MdmEnterpriseDO enterprise, Long tenantId, Set<String> allowedTypes) {
        if (!Objects.equals(tenantId, enterprise.getTenantId())) {
            throw exception(MDM_ENTERPRISE_TENANT_MISMATCH, enterprise.getId());
        }
        if (Boolean.TRUE.equals(enterprise.getDeleted())) {
            throw exception(MDM_ENTERPRISE_DELETED, enterprise.getId());
        }
        if (!MdmEnterpriseStatusEnum.ENABLE.getStatus().equals(enterprise.getStatus())) {
            throw exception(MDM_ENTERPRISE_DISABLED, enterprise.getId());
        }
        if (!MdmEnterpriseTypeEnum.isValid(enterprise.getType()) || !allowedTypes.contains(enterprise.getType())) {
            throw exception(MDM_ENTERPRISE_TYPE_MISMATCH, enterprise.getId());
        }
    }

    private String requireText(String value, String field) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            throw exception(MDM_ENTERPRISE_FIELD_REQUIRED, field);
        }
        return normalized;
    }

    private String requireType(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (!MdmEnterpriseTypeEnum.isValid(normalized)) {
            throw exception(MDM_ENTERPRISE_TYPE_INVALID, value);
        }
        return normalized;
    }

    private String requireStatus(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (!MdmEnterpriseStatusEnum.isValid(normalized)) {
            throw exception(MDM_ENTERPRISE_STATUS_INVALID, value);
        }
        return normalized;
    }

}
