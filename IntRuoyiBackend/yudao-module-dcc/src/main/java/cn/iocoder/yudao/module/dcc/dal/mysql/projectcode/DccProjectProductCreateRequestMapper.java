package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccProjectProductCreateRequestMapper
        extends BaseMapperX<DccProjectProductCreateRequestDO> {

    @Select("SELECT * FROM dcc_project_product_create_request WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    DccProjectProductCreateRequestDO selectByIdForUpdate(@Param("id") Long id);

    default List<DccProjectProductCreateRequestDO> selectPendingList() {
        return selectList(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .in(DccProjectProductCreateRequestDO::getStatus,
                        DccProjectProductCreateStatusConstants.PENDING_REVIEW,
                        DccProjectProductCreateStatusConstants.PENDING_APPROVAL,
                        DccProjectProductCreateStatusConstants.WRITING,
                        DccProjectProductCreateStatusConstants.REJECTED,
                        DccProjectProductCreateStatusConstants.WRITE_FAILED)
                .orderByDesc(DccProjectProductCreateRequestDO::getId));
    }

    default DccProjectProductCreateRequestDO selectActiveByProjectCode(String projectCode) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectProductCreateRequestDO::getProjectCode, projectCode)
                .in(DccProjectProductCreateRequestDO::getStatus,
                        DccProjectProductCreateStatusConstants.ACTIVE_REQUEST_STATUSES));
    }

    default DccProjectProductCreateRequestDO selectActiveByProductCode(String productCode) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectProductCreateRequestDO::getProductCode, productCode)
                .in(DccProjectProductCreateRequestDO::getStatus,
                        DccProjectProductCreateStatusConstants.ACTIVE_REQUEST_STATUSES));
    }

    default DccProjectProductCreateRequestDO selectActiveByProductName(String productName) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectProductCreateRequestDO::getProductName, productName)
                .in(DccProjectProductCreateRequestDO::getStatus,
                        DccProjectProductCreateStatusConstants.ACTIVE_REQUEST_STATUSES));
    }

    default List<DccProjectProductCreateRequestDO> selectActiveByAny(Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .in(DccProjectProductCreateRequestDO::getStatus, statuses));
    }
    default List<DccProjectProductCreateRequestDO> selectByPreviousRequestIds(Collection<Long> requestIds) {
        if (requestIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapperX<DccProjectProductCreateRequestDO>()
                .eq(DccProjectProductCreateRequestDO::getTenantId,
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .in(DccProjectProductCreateRequestDO::getPreviousRequestId, requestIds));
    }
}
