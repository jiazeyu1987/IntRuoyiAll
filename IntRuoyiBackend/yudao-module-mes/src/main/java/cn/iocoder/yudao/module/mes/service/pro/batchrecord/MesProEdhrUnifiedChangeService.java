package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEffectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRecalculateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeSubmitReqVO;

public interface MesProEdhrUnifiedChangeService {

    PageResult<MesProEdhrUnifiedChangeRespVO> getPage(MesProEdhrUnifiedChangePageReqVO reqVO);

    PageResult<MesProEdhrUnifiedChangeImpactRespVO> getImpactPage(MesProEdhrUnifiedChangeImpactPageReqVO reqVO);

    PageResult<MesProEdhrUnifiedChangeEventRespVO> getEventPage(MesProEdhrUnifiedChangeEventPageReqVO reqVO);

    MesProEdhrUnifiedChangeRespVO create(MesProEdhrUnifiedChangeCreateReqVO reqVO);

    MesProEdhrUnifiedChangeRespVO submit(MesProEdhrUnifiedChangeSubmitReqVO reqVO);

    MesProEdhrUnifiedChangeRespVO recalculateImpact(MesProEdhrUnifiedChangeRecalculateImpactReqVO reqVO);

    MesProEdhrUnifiedChangeRespVO approve(MesProEdhrUnifiedChangeApproveReqVO reqVO);

    MesProEdhrUnifiedChangeRespVO requestEffect(MesProEdhrUnifiedChangeEffectReqVO reqVO);
}
