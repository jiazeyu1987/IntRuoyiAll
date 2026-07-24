package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import lombok.Data;

import java.util.List;

@Data
public class IntGyRouteMarkdownImportResult {

    private Integer routeCount;

    private Integer processCreatedCount;

    private Integer processReusedCount;

    private Integer routeProcessCount;

    private List<String> routeCodes;

}
