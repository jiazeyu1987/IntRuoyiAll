package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileMessageJobReplayReqVO {

    @NotEmpty(message = "message job ids cannot be empty")
    private List<Long> jobIds;
}
