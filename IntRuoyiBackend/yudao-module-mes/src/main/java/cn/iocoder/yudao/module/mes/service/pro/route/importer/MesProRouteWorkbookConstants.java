package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.util.List;

final class MesProRouteWorkbookConstants {

    static final String ROUTE_SHEET = "工艺路线";
    static final String PROCESS_SHEET = "路线工序";
    static final String FLOW_SHEET = "流转关系";
    static final String PRODUCT_SHEET = "产品绑定";
    static final String BOM_SHEET = "工序BOM";

    static final List<String> ROUTE_HEADERS = List.of("路线编码", "路线名称", "状态", "负责人", "说明", "备注");
    static final List<String> PROCESS_HEADERS = List.of("路线编码", "序号", "工序编码", "工序名称",
            "准备时间", "等待时间", "颜色", "关键工序", "质检工序", "备注");
    static final List<String> FLOW_HEADERS = List.of("路线编码", "源工序编码", "目标工序编码", "关系类型");
    static final List<String> PRODUCT_HEADERS = List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
            "生产用时", "时间单位", "备注");
    static final List<String> BOM_HEADERS = List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
            "BOM物料名称", "规格", "用料比例", "备注");

    private MesProRouteWorkbookConstants() {
    }
}
