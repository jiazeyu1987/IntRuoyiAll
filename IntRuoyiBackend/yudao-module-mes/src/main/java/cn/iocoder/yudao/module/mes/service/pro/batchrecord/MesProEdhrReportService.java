package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryRespVO;

public interface MesProEdhrReportService {

    PageResult<MesProEdhrReportCatalogRespVO> getCatalogPage(MesProEdhrReportCatalogPageReqVO reqVO);

    MesProEdhrReportCatalogRespVO getCatalogDetail(Long id);

    PageResult<MesProEdhrReportDefinitionRespVO> getDefinitionPage(MesProEdhrReportDefinitionPageReqVO reqVO);

    MesProEdhrReportDefinitionRespVO getDefinitionDetail(Long id);

    MesProEdhrReportQueryRespVO runReportQuery(MesProEdhrReportQueryReqVO reqVO);

    MesProEdhrReportExportAuditRespVO recordExportAudit(MesProEdhrReportExportAuditReqVO reqVO);

    PageResult<MesProEdhrReportExportAuditRespVO> getExportAuditPage(MesProEdhrReportExportAuditPageReqVO reqVO);
}
