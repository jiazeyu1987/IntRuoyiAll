package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

import java.util.Collection;

@Mapper
public interface ErpKingdeeProductionReplenishmentListMapper extends BaseMapperX<ErpKingdeeProductionReplenishmentListDO> {

    @Delete("DELETE FROM erp_kingdee_production_replenishment_list WHERE id = #{id}")
    int hardDeleteById(Long id);

    default PageResult<ErpKingdeeProductionReplenishmentListDO> selectPage(
            ErpProductionReplenishmentListPageReqVO reqVO) {
        return selectPageByProductionReplenishmentListIds(reqVO, null);
    }

    default PageResult<ErpKingdeeProductionReplenishmentListDO> selectPageByProductionReplenishmentListIds(
            ErpProductionReplenishmentListPageReqVO reqVO, Collection<Long> productionReplenishmentListIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListDO>()
                .inIfPresent(ErpKingdeeProductionReplenishmentListDO::getId, productionReplenishmentListIds)
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getSourceBillNo,
                        reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeProductionReplenishmentListDO::getDocumentStatus,
                        reqVO.getDocumentStatus())
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getStockOrgName,
                        reqVO.getStockOrgName())
                .likeIfPresent(ErpKingdeeProductionReplenishmentListDO::getProductionOrgName,
                        reqVO.getProductionOrgName())
                .betweenIfPresent(ErpKingdeeProductionReplenishmentListDO::getBillDate,
                        reqVO.getBillDate())
                .orderByDesc(ErpKingdeeProductionReplenishmentListDO::getBillDate)
                .orderByDesc(ErpKingdeeProductionReplenishmentListDO::getId));
    }

    default ErpKingdeeProductionReplenishmentListDO selectBySource(String sourceFormId,
                                                           String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeProductionReplenishmentListDO>()
                .eq(ErpKingdeeProductionReplenishmentListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeProductionReplenishmentListDO::getSourceFid, sourceFid));
    }

}
