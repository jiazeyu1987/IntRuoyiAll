package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkAutoPersistResult {

    private Long executionId;

    private String trigger;

    private Integer appliedCount;

    private Integer conflictCount;

    private List<Item> items = new ArrayList<>();

    private Long fieldAuditRevisionAfter;

    private String fieldAuditHeadHashAfter;

    private String cellValuesHashAfter;

    @Data
    @Accessors(chain = true)
    public static class Item {

        private Long ruleId;

        private Long ruleVersion;

        private String targetCellKey;

        private String sourceType;

        private String sourceFieldCode;

        private String status;

        private Object value;
    }
}
