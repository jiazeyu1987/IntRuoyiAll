package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import org.springframework.web.multipart.MultipartFile;

/**
 * 工艺路线员工工序模板服务。
 */
public interface MesProRouteProcessTemplateService {

    byte[] exportTemplate(Long routeId);

    MesProRouteProcessTemplateImportResult importTemplate(MultipartFile file, String importMode);
}
