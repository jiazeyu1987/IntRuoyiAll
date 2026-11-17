package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationCorrectiveReqVO {
    @NotNull @Positive private Long deviationId;
    @NotNull @Positive private Integer expectedContentVersion;
    @NotBlank private String expectedContentHash;
    @NotBlank @Size(max = 4000) private String result;
    @NotNull private LocalDateTime completedAt;
    @Size(max = 10) private List<@NotBlank @Size(max = 2000) String> evidenceUrls;
}
