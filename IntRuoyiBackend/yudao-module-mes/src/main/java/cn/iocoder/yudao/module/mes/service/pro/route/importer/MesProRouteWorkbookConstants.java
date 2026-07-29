package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.util.List;

final class MesProRouteWorkbookConstants {

    static final String ROUTE_SHEET = "工艺路线";
    static final String PROCESS_SHEET = "路线工序";
    static final String FLOW_SHEET = "流转关系";
    static final String BOUNDARY_SHEET = "边界关系";
    static final String LAYOUT_SHEET = "流转布局";
    static final String PRODUCT_SHEET = "产品绑定";
    static final String BOM_SHEET = "工序BOM";
    static final String SCHEDULE_CONFIG_SHEET = "路线排产配置";
    static final String FLOW_CONFIG_SHEET = "流程用途配置";
    static final String FLOW_PROCESS_CONFIG_SHEET = "工序用途配置";
    static final String BATCH_RECORD_SHEET = "工序表单绑定";

    static final List<String> ROUTE_HEADERS = List.of("路线编码", "路线名称", "状态", "负责人", "说明", "备注");
    static final List<String> PROCESS_HEADERS = List.of("路线编码", "序号", "工序编码", "工序名称",
            "准备时间", "等待时间", "颜色", "关键工序", "质检工序", "备注");
    static final List<String> FLOW_HEADERS = List.of("路线编码", "源工序编码", "目标工序编码", "关系类型");
    static final List<String> BOUNDARY_HEADERS = List.of("路线编码", "边界类型", "工序编码", "序号");
    static final List<String> LAYOUT_HEADERS = List.of("路线编码", "工序编码", "横坐标", "纵坐标", "宽度", "高度");
    static final List<String> PRODUCT_HEADERS = List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
            "生产用时", "时间单位", "备注");
    static final List<String> BOM_HEADERS = List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
            "BOM物料名称", "规格", "用料比例", "备注");
    static final List<String> SCHEDULE_CONFIG_HEADERS = List.of("路线编码", "工序编码", "产能模式", "小时产能",
            "无限产能数量系数", "无限产能基准分钟", "夜班启用", "日历规则ID", "配置版本", "备注");
    static final List<String> FLOW_CONFIG_HEADERS = List.of("路线编码", "用途类型", "启用", "配置版本", "备注");
    static final List<String> FLOW_PROCESS_CONFIG_HEADERS = List.of("路线编码", "用途类型", "工序编码", "启用",
            "执行模式", "生产数量系数", "批记录表单ID", "备注");
    static final List<String> BATCH_RECORD_HEADERS = List.of("路线编码", "用途类型", "工序编码", "批记录表单ID",
            "批记录定义ID", "批记录版本ID", "表单槽位", "表单绑定Key", "表单模板ID",
            "表单模板名称快照", "最后发布模板版本ID", "最后发布模板版本号", "实例范围",
            "共享表单Key", "填写范围JSON", "记录分类", "校验档案", "记录本启用",
            "权限范围ID", "记录分类快照Hash", "必填策略", "必填条件JSON",
            "负责人角色Key", "归档可见性", "槽位配置快照Hash", "候选来源类型",
            "候选来源ID", "候选来源名称", "报表序号", "备注");

    private MesProRouteWorkbookConstants() {
    }
}
