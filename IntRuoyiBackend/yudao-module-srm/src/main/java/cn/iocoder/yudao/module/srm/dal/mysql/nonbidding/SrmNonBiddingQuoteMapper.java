package cn.iocoder.yudao.module.srm.dal.mysql.nonbidding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding.SrmNonBiddingQuoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmNonBiddingQuoteMapper extends BaseMapperX<SrmNonBiddingQuoteDO> {

    default List<SrmNonBiddingQuoteDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmNonBiddingQuoteDO>()
                .eq(SrmNonBiddingQuoteDO::getProjectId, projectId)
                .orderByAsc(SrmNonBiddingQuoteDO::getId));
    }

    default SrmNonBiddingQuoteDO selectByProjectIdAndSupplierId(Long projectId, Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmNonBiddingQuoteDO>()
                .eq(SrmNonBiddingQuoteDO::getProjectId, projectId)
                .eq(SrmNonBiddingQuoteDO::getSupplierId, supplierId)
                .last("LIMIT 1"));
    }
}
