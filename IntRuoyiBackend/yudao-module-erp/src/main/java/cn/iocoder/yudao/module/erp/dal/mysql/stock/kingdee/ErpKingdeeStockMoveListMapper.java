package cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeStockMoveListDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeStockMoveListMapper extends BaseMapperX<ErpKingdeeStockMoveListDO> {

    default PageResult<ErpKingdeeStockMoveListDO> selectPage(ErpKingdeeStockMovePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpKingdeeStockMoveListDO>()
                .likeIfPresent(ErpKingdeeStockMoveListDO::getSourceBillNo, reqVO.getSourceBillNo())
                .eqIfPresent(ErpKingdeeStockMoveListDO::getDocumentStatus, reqVO.getDocumentStatus())
                .likeIfPresent(ErpKingdeeStockMoveListDO::getTransferDirect, reqVO.getTransferDirect())
                .betweenIfPresent(ErpKingdeeStockMoveListDO::getBillDate, reqVO.getBillDate())
                .orderByDesc(ErpKingdeeStockMoveListDO::getBillDate)
                .orderByDesc(ErpKingdeeStockMoveListDO::getId));
    }

    default ErpKingdeeStockMoveListDO selectBySource(String sourceFormId, String sourceFid) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeStockMoveListDO>()
                .eq(ErpKingdeeStockMoveListDO::getSourceFormId, sourceFormId)
                .eq(ErpKingdeeStockMoveListDO::getSourceFid, sourceFid));
    }

}
