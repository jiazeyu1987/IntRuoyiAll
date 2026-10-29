package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class EdhrBatchIntegrityChunkRespVO {
    private Long upperId;
    private Long nextId;
    private int checked;
    private String status;
    private List<BatchError> errors = new ArrayList<>();
    public record BatchError(Long batchId, String batchExecutionCode, String message) {}
}
