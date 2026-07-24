package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * ERP 生产用料清单同步明细 Mapper
 *
 * @author Codex
 */
@Mapper
public interface MesKingdeeProductionMaterialListMapper extends BaseMapperX<MesKingdeeProductionMaterialListDO> {

    IPage<MesKingdeeProductionMaterialListGroupRespVO> selectGroupPage(
            IPage<MesKingdeeProductionMaterialListGroupRespVO> page,
            @Param("reqVO") MesKingdeeProductionMaterialListPageReqVO reqVO);

    List<MesKingdeeProductionMaterialListGroupRespVO> selectGroupList(
            @Param("reqVO") MesKingdeeProductionMaterialListPageReqVO reqVO);

    default PageResult<MesKingdeeProductionMaterialListDO> selectPage(
            MesKingdeeProductionMaterialListPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getSourceBillNo, reqVO.getSourceBillNo())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getProductCode, reqVO.getProductCode())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getProductionOrderNo, reqVO.getProductionOrderNo())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getChildMaterialCode, reqVO.getChildMaterialCode())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getChildMaterialName, reqVO.getChildMaterialName())
                .betweenIfPresent(MesKingdeeProductionMaterialListDO::getSourceModifyTime, reqVO.getSourceModifyTime())
                .betweenIfPresent(MesKingdeeProductionMaterialListDO::getLastSyncTime, reqVO.getLastSyncTime())
                .orderByDesc(MesKingdeeProductionMaterialListDO::getSourceModifyTime)
                .orderByDesc(MesKingdeeProductionMaterialListDO::getId));
    }

    default MesKingdeeProductionMaterialListDO selectBySourceLine(String sourceBillNo, String productionOrderNo,
                                                                  Integer productionOrderLineNo,
                                                                  String childMaterialCode) {
        return selectOne(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getSourceBillNo, sourceBillNo)
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderNo, productionOrderNo)
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo, productionOrderLineNo)
                .eq(MesKingdeeProductionMaterialListDO::getChildMaterialCode, childMaterialCode));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByProductionOrderNo(String productionOrderNo) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderNo, productionOrderNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByWorkOrderId(Long workOrderId) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getWorkOrderId, workOrderId)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByWorkOrderIds(Collection<Long> workOrderIds) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .inIfPresent(MesKingdeeProductionMaterialListDO::getWorkOrderId, workOrderIds)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getWorkOrderId)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListBySourceBillNo(String sourceBillNo) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getSourceBillNo, sourceBillNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

}
