package cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo;

import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.*;

@Data
public class MesSharedChangeReqVO {
    @NotBlank @Pattern(regexp="PROCESS|CRAFT|DEVICE") private String subjectType;
    @NotBlank @Pattern(regexp="UPDATE|REPLACE") private String action;
    @NotNull @Positive private Long sourceId;
    @Positive private Long targetId;
    @NotBlank private String expectedFingerprint;
    @NotBlank @Size(max=64) private String requestId;
    @Size(max=1000) private String reason;
    private String impactToken;
    @Valid private MesProProcessSaveReqVO process;
    @Valid private List<@NotNull MesProProcessContentSaveReqVO> processContents;
    @Valid private MesDvMachinerySaveReqVO device;
    @Valid private MesProCraftSaveReqVO craft;
    @Valid @NotNull private List<NodeSelection> nodes = new ArrayList<>();

    @Data
    public static class NodeSelection {
        @NotNull private Long routeId;
        @NotNull private Long routeProcessId;
        private Long deviceCraftId;
        private boolean withoutParameters;
        @Valid @NotNull private List<CraftSelection> crafts = new ArrayList<>();
    }
    @Data
    public static class CraftSelection {
        @NotNull @Positive private Long craftId;
        @NotEmpty private List<@NotNull @Positive Long> deviceIds;
    }
}
