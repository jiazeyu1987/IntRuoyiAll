package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminErrors.CONFIGURATION_REQUIRED;
import static cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminErrors.CUSTOM_MATRIX_PUBLIC_FORBIDDEN;

/** 类型、正式类别及审批规则一次提交；任何规则校验失败均回滚类型。 */
@Service
public class DccFileTypeConfigurationService {
    @Resource private DccFileTypeTaxonomyAdminService taxonomyService;
    @Resource private DccFileCategoryAdminService categoryService;
    @Resource private DccCategoryApprovalMatrixAdminService matrixService;
    @Resource private DccFileTypeTaxonomyMapper taxonomyMapper;
    @Resource private DccFileCategoryMapper categoryMapper;
    @Resource private DccFileDirectoryMapper directoryMapper;

    @Transactional(rollbackFor = Exception.class)
    @com.mzt.logapi.starter.annotation.LogRecord(type = "DCC文件类型", subType = "新增文件类型", bizNo = "{{#_ret}}", success = "新增类型及审批配置 {{#request}}")
    public Long create(DccFileTypeTaxonomySaveReqVO request) {
        var parent = request.getParentId() == null ? null : taxonomyMapper.selectById(request.getParentId());
        if (parent != null && parent.getLevelNo() >= 2 && !Boolean.TRUE.equals(request.getPathNodeOnly())) {
            var binding = request.getDirectoryBinding();
            var directory = binding == null || binding.getDirectoryId() == null ? null
                    : directoryMapper.selectTenantForUpdate(TenantContextHolder.getRequiredTenantId(), binding.getDirectoryId());
            if (binding == null || !Boolean.TRUE.equals(binding.getActive()) || directory == null
                    || !Boolean.TRUE.equals(directory.getActive()) || Boolean.TRUE.equals(directory.getDeleted())
                    || !java.util.Objects.equals(directory.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                throw new ServiceException(BAD_REQUEST.getCode(), "请选择本租户启用的存储目录");
        }
        Long id = taxonomyService.createTaxonomy(request);
        DccFileTypeTaxonomyDO type = taxonomyMapper.selectById(id);
        if (type.getLevelNo() < 3) return id;
        if (Boolean.TRUE.equals(request.getPathNodeOnly())) {
            if (type.getLevelNo() >= 5 || request.getApprovalSourceMode() != null
                    || request.getMatrixSourceCategoryId() != null || request.getApprovalMatrix() != null)
                throw exception(CONFIGURATION_REQUIRED);
            return id;
        }
        DccFileCategorySaveReqVO category = new DccFileCategorySaveReqVO();
        category.setCode(type.getCode()); category.setName(type.getName());
        category.setActive(type.getActive()); category.setSort(type.getSort());
        category.setFileTypeTaxonomyId(id); category.setApprovalSourceMode(request.getApprovalSourceMode());
        category.setMatrixSourceCategoryId(request.getMatrixSourceCategoryId());
        category.setApprovalMatrix(request.getApprovalMatrix());
        category.setDirectoryBinding(request.getDirectoryBinding());
        category.setRemark(request.getRemark());
        createCategory(category);
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    @com.mzt.logapi.starter.annotation.LogRecord(type = "DCC文件类别", subType = "新增类别及矩阵", bizNo = "{{#_ret}}", success = "新增类别配置 {{#request}}")
    public Long createCategory(DccFileCategorySaveReqVO request) {
        DccCategoryApprovalMatrixSaveReqVO matrix = resolveMatrix(request, true);
        Long id = categoryService.createCategory(request);
        if (matrix != null) matrixService.saveApprovalMatrix(id, matrix);
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    @com.mzt.logapi.starter.annotation.LogRecord(type = "DCC文件类别", subType = "修改类别及矩阵", bizNo = "{{#request.id}}", success = "修改类别配置 {{#request}}")
    public void updateCategory(DccFileCategorySaveReqVO request) {
        DccCategoryApprovalMatrixSaveReqVO matrix = resolveMatrix(request, false);
        categoryService.updateCategory(request);
        if (matrix != null) matrixService.saveApprovalMatrix(request.getId(), matrix);
    }

    private DccCategoryApprovalMatrixSaveReqVO resolveMatrix(DccFileCategorySaveReqVO request, boolean creating) {
        String mode = request.getApprovalSourceMode();
        DccFileTypeTaxonomyDO type = request.getFileTypeTaxonomyId() == null ? null
                : taxonomyMapper.selectById(request.getFileTypeTaxonomyId());
        if (type == null || (!"PUBLIC_MATRIX".equals(mode) && !"APPLICATION_CUSTOM".equals(mode)))
            throw exception(CONFIGURATION_REQUIRED);
        if ("其他".equals(type.getName()) && "PUBLIC_MATRIX".equals(mode))
            throw exception(CUSTOM_MATRIX_PUBLIC_FORBIDDEN);
        DccCategoryApprovalMatrixSaveReqVO matrix = request.getApprovalMatrix();
        if ("APPLICATION_CUSTOM".equals(mode)) {
            if (!"其他".equals(type.getName()) || matrix != null || request.getMatrixSourceCategoryId() != null)
                throw exception(CONFIGURATION_REQUIRED);
            return null;
        }
        if (matrix != null && request.getMatrixSourceCategoryId() != null) throw exception(CONFIGURATION_REQUIRED);
        if (request.getMatrixSourceCategoryId() != null) {
            DccFileCategoryDO source = categoryMapper.selectById(request.getMatrixSourceCategoryId());
            if (source == null || !Boolean.TRUE.equals(source.getActive())
                    || !"PUBLIC_MATRIX".equals(source.getApprovalSourceMode())) throw exception(CONFIGURATION_REQUIRED);
            var existing = matrixService.getApprovalMatrix(source.getId());
            if (existing.getRouteId() == null || existing.getEffectiveTime() == null
                    || existing.getEffectiveTime().isAfter(LocalDateTime.now()) || existing.getRules().isEmpty())
                throw exception(CONFIGURATION_REQUIRED);
            matrix = new DccCategoryApprovalMatrixSaveReqVO().setEffectiveTime(LocalDateTime.now())
                    .setRemark("复制已有矩阵：类别 " + source.getId() + "，版本 " + existing.getRouteVersionNo())
                    .setRules(existing.getRules().stream().map(rule ->
                            BeanUtils.toBean(rule, DccCategoryApprovalMatrixSaveReqVO.Rule.class)).toList());
        }
        if (matrix == null) {
            if (creating || request.getId() == null || matrixService.getApprovalMatrix(request.getId()).getRouteId() == null)
                throw exception(CONFIGURATION_REQUIRED);
            return null;
        }
        if (matrix.getEffectiveTime() == null || matrix.getEffectiveTime().isAfter(LocalDateTime.now()))
            throw exception(CONFIGURATION_REQUIRED);
        return matrix;
    }
}
