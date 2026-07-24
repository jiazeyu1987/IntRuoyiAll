package cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 电子签名统一签名记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SignatureGovernanceRecordPageReqVO extends PageParam {

    @Schema(description = "来源编码列表", example = "FILE,BATCH_RECORD,SHOWROOM")
    private List<String> sourceCodes;

    @Schema(description = "业务记录关键字", example = "BR-20260714")
    private String keyword;

    @Schema(description = "签名人用户编号", example = "101")
    private Long signerUserId;

    @Schema(description = "签名人关键字", example = "张三")
    private String signerKeyword;

    @Schema(description = "动作编码", example = "APPROVE")
    private String actionCode;

    @Schema(description = "证据 Hash", example = "9f86d081")
    private String evidenceHash;

    @Schema(description = "签名时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] signedAt;

}
