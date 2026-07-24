package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DccPaperDistributionIssueReqVO {

    @NotEmpty(message = "纸质接收人不能为空")
    private List<Long> recipientUserIds;

}
