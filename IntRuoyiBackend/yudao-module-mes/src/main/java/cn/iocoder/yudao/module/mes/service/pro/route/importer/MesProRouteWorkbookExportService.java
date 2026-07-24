package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;

public interface MesProRouteWorkbookExportService {

    byte[] exportWorkbook(MesProRoutePageReqVO pageReqVO);

}
