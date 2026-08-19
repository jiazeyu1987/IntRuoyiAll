package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.util.List;
import java.util.Set;

/**
 * 工艺路线员工工序模板契约。
 */
public final class MesProRouteProcessTemplateConstants {

    public static final String SHEET_NAME = "工序模板";
    public static final String IMPORT_MODE_REBUILD = "REBUILD";
    public static final String IMPORT_MODE_UPGRADE = "UPGRADE";
    public static final int HEADER_ROW_INDEX = 4;
    public static final int DATA_START_ROW_INDEX = 5;
    public static final List<String> HEADERS = List.of("工序名称", "产能", "设备编号", "是否关键工序");
    public static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "编号", "工序编号", "工作站编号", "工序负责人", "质量控制要求", "开始配置", "工序开始",
            "批记录表单", "表单槽位");

    private MesProRouteProcessTemplateConstants() {
    }
}
