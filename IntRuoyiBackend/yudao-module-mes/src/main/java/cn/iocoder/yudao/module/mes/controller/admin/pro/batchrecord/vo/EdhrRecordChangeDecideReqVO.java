package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
@Data
@Accessors(chain = true)
public class EdhrRecordChangeDecideReqVO {
    @NotNull private Long changeEventId;
    @NotBlank @Pattern(regexp = "REJECTED|RETURNED") private String decision;
    @NotBlank @Size(max = 500) private String reason;
    @NotBlank private String password;
}
