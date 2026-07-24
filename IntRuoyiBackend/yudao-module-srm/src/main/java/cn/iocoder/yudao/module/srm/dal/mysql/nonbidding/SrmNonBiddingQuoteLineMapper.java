package cn.iocoder.yudao.module.srm.dal.mysql.nonbidding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding.SrmNonBiddingQuoteLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmNonBiddingQuoteLineMapper extends BaseMapperX<SrmNonBiddingQuoteLineDO> {

    default List<SrmNonBiddingQuoteLineDO> selectListByQuoteId(Long quoteId) {
        return selectList(new LambdaQueryWrapperX<SrmNonBiddingQuoteLineDO>()
                .eq(SrmNonBiddingQuoteLineDO::getQuoteId, quoteId)
                .orderByAsc(SrmNonBiddingQuoteLineDO::getId));
    }

    default List<SrmNonBiddingQuoteLineDO> selectListByMaterialId(Long materialId) {
        return selectList(new LambdaQueryWrapperX<SrmNonBiddingQuoteLineDO>()
                .eq(SrmNonBiddingQuoteLineDO::getMaterialId, materialId)
                .orderByAsc(SrmNonBiddingQuoteLineDO::getId));
    }
}
