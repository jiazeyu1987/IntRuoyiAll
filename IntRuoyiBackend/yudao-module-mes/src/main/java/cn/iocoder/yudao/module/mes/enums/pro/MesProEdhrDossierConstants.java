package cn.iocoder.yudao.module.mes.enums.pro;

import java.util.Set;

/**
 * eDHR 批次卷宗目录常量。
 */
public final class MesProEdhrDossierConstants {

    private MesProEdhrDossierConstants() {
    }

    public static final String ITEM_TYPE_FINAL_INSPECTION = "FINAL_INSPECTION";
    public static final String ITEM_KEY_FINAL_INSPECTION = "FINAL_INSPECTION";
    public static final String ITEM_NAME_FINAL_INSPECTION = "成品检";

    public static final String ITEM_STATUS_PENDING = "PENDING";
    public static final String ITEM_STATUS_COMPLETED = "COMPLETED";
    public static final String ITEM_STATUS_BLOCKED = "BLOCKED";

    public static final String SOURCE_DOC_TYPE_OQC = "OQC";
    public static final String SOURCE_DOC_STATUS_FINISHED = "FINISHED";
    public static final String SOURCE_DOC_RESULT_PASS = "PASS";
    public static final String SOURCE_DOC_RESULT_FAIL = "FAIL";
    public static final Set<String> FINAL_INSPECTION_PASS_RESULTS = Set.of(SOURCE_DOC_RESULT_PASS, "PASSED", "QUALIFIED");

    public static final String BLOCKER_CODE_OQC_RESULT_FAIL = "OQC_RESULT_FAIL";
    public static final String BLOCKER_MESSAGE_OQC_RESULT_FAIL = "OQC检验结果不合格";
}
