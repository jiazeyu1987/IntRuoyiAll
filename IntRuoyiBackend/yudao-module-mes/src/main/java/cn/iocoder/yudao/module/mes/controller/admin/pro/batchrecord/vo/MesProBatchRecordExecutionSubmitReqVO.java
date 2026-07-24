package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionSubmitReqVO {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotNull(message = "workTaskId 不能为空")
    private Long workTaskId;

    @NotBlank(message = "password 不能为空")
    private String password;

    private String comment;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;

    private List<ReviewAssigneeSelection> reviewAssigneeSelections;

    @Data
    @Accessors(chain = true)
    public static class ReviewAssigneeSelection {

        @NotBlank(message = "signatureCellKey 不能为空")
        private String signatureCellKey;

        @NotNull(message = "selectedUserId 不能为空")
        private Long selectedUserId;
    }
}
