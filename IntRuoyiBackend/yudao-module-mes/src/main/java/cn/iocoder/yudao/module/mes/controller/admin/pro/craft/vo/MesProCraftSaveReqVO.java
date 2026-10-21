package cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo;
import com.alibaba.fastjson.JSONArray;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesProCraftSaveReqVO {
    private Long id;
    @NotBlank @Size(max=128) private String name;
    @NotNull @Positive private Long processId;
    @NotNull private JSONArray parameters;
    private String expectedFingerprint;
    private String impactToken;
    @NotBlank @Size(max=512) private String reason;
}
