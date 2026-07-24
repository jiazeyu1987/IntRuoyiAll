package cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeInventoryListDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeInventoryListMapper extends BaseMapperX<ErpKingdeeInventoryListDO> {

    default PageResult<ErpKingdeeInventoryListDO> selectPage(ErpKingdeeInventoryListPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpKingdeeInventoryListDO>()
                .likeIfPresent(ErpKingdeeInventoryListDO::getMaterialNumber, reqVO.getMaterialNumber())
                .likeIfPresent(ErpKingdeeInventoryListDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(ErpKingdeeInventoryListDO::getWarehouseName, reqVO.getWarehouseName())
                .likeIfPresent(ErpKingdeeInventoryListDO::getLotNumber, reqVO.getLotNumber())
                .orderByDesc(ErpKingdeeInventoryListDO::getSourceModifyTime)
                .orderByDesc(ErpKingdeeInventoryListDO::getId));
    }

    default ErpKingdeeInventoryListDO selectBySourceLine(String sourceLineKey) {
        return selectOne(ErpKingdeeInventoryListDO::getSourceLineKey, sourceLineKey);
    }

}
