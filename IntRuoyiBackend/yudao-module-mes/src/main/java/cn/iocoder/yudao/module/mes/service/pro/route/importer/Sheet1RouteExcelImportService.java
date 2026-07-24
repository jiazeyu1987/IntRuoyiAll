package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import org.springframework.web.multipart.MultipartFile;

public interface Sheet1RouteExcelImportService {

    Sheet1RouteExcelImportResult importExcel(MultipartFile file, Integer processStatus);

}
