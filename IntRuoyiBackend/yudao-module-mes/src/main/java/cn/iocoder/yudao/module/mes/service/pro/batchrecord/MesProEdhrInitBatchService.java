package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestUploadReqVO;

public interface MesProEdhrInitBatchService {

    PageResult<MesProEdhrInitBatchRespVO> getPage(MesProEdhrInitBatchPageReqVO reqVO);

    MesProEdhrInitBatchRespVO get(Long id);

    MesProEdhrInitBatchRespVO create(MesProEdhrInitBatchCreateReqVO reqVO);

    MesProEdhrInitManifestRespVO uploadManifest(MesProEdhrInitManifestUploadReqVO reqVO);

    MesProEdhrInitBatchPrecheckRespVO runPrecheck(Long id);

    PageResult<MesProEdhrInitIssueRespVO> getIssuePage(MesProEdhrInitIssuePageReqVO reqVO);
}
