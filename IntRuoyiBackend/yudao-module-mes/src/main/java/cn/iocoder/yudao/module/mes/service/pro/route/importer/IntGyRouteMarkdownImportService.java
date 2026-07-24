package cn.iocoder.yudao.module.mes.service.pro.route.importer;

public interface IntGyRouteMarkdownImportService {

    IntGyRouteMarkdownImportResult importMarkdown(String markdown, Integer processStatus,
                                                  String checkProcessCodesByRouteCodeJson);

}
