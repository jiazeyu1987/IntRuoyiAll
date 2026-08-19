package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工艺路线员工工序模板导入结果。
 */
@Data
public class MesProRouteProcessTemplateImportResult {

    private Long routeId;

    private String routeCode;

    private String routeName;

    private String importMode;

    private Long routeVersionId;

    private String routeVersionNo;

    private Integer routeProcessCount;

    private List<String> processNames = new ArrayList<>();
}
