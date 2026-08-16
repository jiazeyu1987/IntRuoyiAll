package cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeProductionPickListMapper extends BaseMapperX<ErpKingdeeProductionPickListDO> {

    default PageResult<ErpKingdeeProductionPickListDO> selectPage(
            ErpProductionPickListPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpKingdeeProductionPickListDO>()
                .likeIfPresent(ErpKingdeeProductionPickListDO::getSourceBillNo,
                        reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeProductionPickListDO::getDocumentStatus,
                        reqVO.getDocumentStatus())
                .betweenIfPresent(ErpKingdeeProductionPickListDO::getBillDate,
                        reqVO.getBillDate())
                .orderByDesc(ErpKingdeeProductionPickListDO::getBillDate)
                .orderByDesc(ErpKingdeeProductionPickListDO::getId));
    }

    default ErpKingdeeProductionPickListDO selectBySource(String sourceFormId,
                                                           String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeProductionPickListDO>()
                .eq(ErpKingdeeProductionPickListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeProductionPickListDO::getSourceFid, sourceFid));
    }

}
