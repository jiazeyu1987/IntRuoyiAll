package cn.iocoder.yudao.module.srm.dal.mysql.supplier;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SrmSupplierPortalApplicationMapper extends BaseMapperX<SrmSupplierPortalApplicationDO> {

    default SrmSupplierPortalApplicationDO selectByUserId(Long tenantId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSupplierPortalApplicationDO>()
                .eq(SrmSupplierPortalApplicationDO::getTenantId, tenantId)
                .eq(SrmSupplierPortalApplicationDO::getUserId, userId)
                .orderByDesc(SrmSupplierPortalApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default List<SrmSupplierPortalApplicationDO> selectApprovedListBySupplierId(Long tenantId, Long supplierId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierPortalApplicationDO>()
                .eq(SrmSupplierPortalApplicationDO::getTenantId, tenantId)
                .eq(SrmSupplierPortalApplicationDO::getSupplierId, supplierId)
                .eq(SrmSupplierPortalApplicationDO::getApplicationStatus, "APPROVED")
                .orderByDesc(SrmSupplierPortalApplicationDO::getId));
    }

    default PageResult<SrmSupplierPortalApplicationDO> selectPage(SrmSupplierPortalApplicationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSupplierPortalApplicationDO>()
                .eqIfPresent(SrmSupplierPortalApplicationDO::getId, reqVO.getId())
                .likeIfPresent(SrmSupplierPortalApplicationDO::getCompanyName, reqVO.getCompanyName())
                .likeIfPresent(SrmSupplierPortalApplicationDO::getContactName, reqVO.getContactName())
                .eqIfPresent(SrmSupplierPortalApplicationDO::getApplicationStatus, reqVO.getApplicationStatus())
                .orderByDesc(SrmSupplierPortalApplicationDO::getSubmittedTime)
                .orderByDesc(SrmSupplierPortalApplicationDO::getId));
    }

    default List<SrmSupplierPortalApplicationDO> selectUnifiedApprovalList(Long tenantId, String applicationStatus,
                                                                           Long userId, Long auditBy,
                                                                           String keyword) {
        return selectUnifiedApprovalList(tenantId,
                applicationStatus == null ? List.of() : List.of(applicationStatus), userId, auditBy, keyword);
    }

    default List<SrmSupplierPortalApplicationDO> selectUnifiedApprovalList(Long tenantId,
                                                                           Collection<String> applicationStatuses,
                                                                           Long userId, Long auditBy,
                                                                           String keyword) {
        LambdaQueryWrapperX<SrmSupplierPortalApplicationDO> query = new LambdaQueryWrapperX<SrmSupplierPortalApplicationDO>()
                .eq(SrmSupplierPortalApplicationDO::getTenantId, tenantId)
                .inIfPresent(SrmSupplierPortalApplicationDO::getApplicationStatus, applicationStatuses)
                .eqIfPresent(SrmSupplierPortalApplicationDO::getUserId, userId)
                .eqIfPresent(SrmSupplierPortalApplicationDO::getAuditBy, auditBy)
                .orderByDesc(SrmSupplierPortalApplicationDO::getSubmittedTime)
                .orderByDesc(SrmSupplierPortalApplicationDO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper.like(SrmSupplierPortalApplicationDO::getCompanyName, value)
                    .or().like(SrmSupplierPortalApplicationDO::getUnifiedSocialCreditCode, value)
                    .or().like(SrmSupplierPortalApplicationDO::getContactName, value)
                    .or().like(SrmSupplierPortalApplicationDO::getContactPhone, value));
        }
        return selectList(query);
    }
}
