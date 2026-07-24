package cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface MesKingdeeProductionMaterialListQueryService {

    PageResult<MesKingdeeProductionMaterialListRespVO> getPage(
            @Valid MesKingdeeProductionMaterialListPageReqVO pageReqVO);

    PageResult<MesKingdeeProductionMaterialListGroupRespVO> getGroupPage(
            @Valid MesKingdeeProductionMaterialListPageReqVO pageReqVO);

    List<MesKingdeeProductionMaterialListDetailRespVO> getDetailList(String sourceBillNo);

}
