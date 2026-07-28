package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionGoldenFingerBulkVoidRespVO {

    private Integer matchedCount;

    private Integer voidedCount;

    private Integer skippedCount;

    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {

        private Long batchExecutionId;

        private String batchExecutionCode;

        private Integer status;

        private String result;

        private String message;

        private Long changeEventId;
    }
}
