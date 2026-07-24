package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import org.springframework.web.multipart.MultipartFile;

public interface MesProRouteWorkbookImportService {

    MesProRouteWorkbookImportResult importWorkbook(MultipartFile file);

}
