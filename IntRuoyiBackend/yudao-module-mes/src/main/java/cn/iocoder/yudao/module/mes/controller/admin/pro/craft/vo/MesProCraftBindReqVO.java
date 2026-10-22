package cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProCraftBindReqVO extends MesProCraftBindingTargetReqVO {
    @NotEmpty private List<@NotNull @Valid Binding> bindings;
    @Data
    public static class Binding {
        @NotNull @Positive private Long craftId;
        @NotEmpty private List<@NotNull @Positive Long> deviceIds;
    }
}
