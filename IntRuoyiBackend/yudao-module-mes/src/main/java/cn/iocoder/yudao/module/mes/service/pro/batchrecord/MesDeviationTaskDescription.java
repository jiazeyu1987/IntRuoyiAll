package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

/** Human-readable duties come from formal task identity, never from free-text opinions. */
public final class MesDeviationTaskDescription {
    private MesDeviationTaskDescription() { }
    public static String title(String type, String node, boolean hasCorrectiveOwner) {
        if ("DEVIATION_RECEIVE".equals(type)) return "开展偏差调查";
        if ("DEVIATION_CORRECT".equals(type)) return "完成偏差整改";
        if ("DEVIATION_VERIFY".equals(type)) return hasCorrectiveOwner ? "完成偏差整改结果验证" : "完成偏差调查结果验证";
        if ("DEVIATION_CLOSE".equals(type)) return "完成偏差正式关闭";
        if (node == null) throw new IllegalArgumentException("偏差任务缺少正式职责节点");
        return switch (node) {
            case "PREPARER" -> "完成偏差处理记录编制";
            case "VERIFIER" -> "完成纠正措施核验";
            case "DEPARTMENT_OWNER" -> "完成部门负责人确认";
            case "QA" -> "完成 QA 关闭确认";
            case "QUALITY_OWNER" -> "完成质量负责人批准";
            case "MANAGEMENT_REP" -> "完成管理者代表批准";
            default -> throw new IllegalArgumentException("偏差任务职责节点无效");
        };
    }
}
