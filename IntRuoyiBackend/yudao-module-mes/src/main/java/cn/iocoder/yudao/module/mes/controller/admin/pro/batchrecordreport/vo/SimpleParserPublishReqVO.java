package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SimpleParserPublishReqVO {
    @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String requestId;
    @NotBlank @Size(max=128) private String routeName;
    @NotNull private Map<String, Object> recognitionJson;
    @Valid @Size(max=200) private List<ProcessSelection> processSelections = List.of();
    @Size(max=64) private String previewToken;
    @Data public static class ProcessSelection {
        @NotNull @Min(0) private Integer processIndex;
        @Positive private Long processId;
        @Valid private List<CraftSelection> craftSelections = List.of();
    }
    @Data public static class CraftSelection {
        @NotNull @Min(0) private Integer groupIndex;
        @NotNull @Positive private Long craftId;
    }
}
