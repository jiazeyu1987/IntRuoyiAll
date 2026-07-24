package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import lombok.Data;

import java.util.List;

@Data
public class MesProRouteWorkbookImportResult {

    private int routeCount;
    private int routeProcessCount;
    private int routeProductCount;
    private int routeProductBomCount;
    private List<String> routeCodes;

}
