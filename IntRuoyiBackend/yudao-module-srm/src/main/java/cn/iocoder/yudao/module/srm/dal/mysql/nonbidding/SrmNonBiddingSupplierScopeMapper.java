package cn.iocoder.yudao.module.srm.dal.mysql.nonbidding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding.SrmNonBiddingSupplierScopeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmNonBiddingSupplierScopeMapper extends BaseMapperX<SrmNonBiddingSupplierScopeDO> {

    default List<SrmNonBiddingSupplierScopeDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmNonBiddingSupplierScopeDO>()
                .eq(SrmNonBiddingSupplierScopeDO::getProjectId, projectId)
                .orderByAsc(SrmNonBiddingSupplierScopeDO::getId));
    }

    default SrmNonBiddingSupplierScopeDO selectByProjectIdAndSupplierId(Long projectId, Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmNonBiddingSupplierScopeDO>()
                .eq(SrmNonBiddingSupplierScopeDO::getProjectId, projectId)
                .eq(SrmNonBiddingSupplierScopeDO::getSupplierId, supplierId)
                .last("LIMIT 1"));
    }
}
