package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrRehearsalReadinessResult {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String ITEM_STATUS_PASS = "PASS";
    public static final String ITEM_STATUS_BLOCKER = "BLOCKER";
    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_BLOCKER = "BLOCKER";

    private String overallStatus;

    private List<Item> items = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class Item {

        private String code;

        private String status;

        private String severity;

        private String roleKey;

        private Long subjectId;

        private String message;

        private String suggestion;
    }
}
