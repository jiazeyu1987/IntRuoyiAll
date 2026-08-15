package cn.iocoder.yudao.module.mes.productionrelease.core;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesReleaseFlowBlocker {

    private MesReleaseFlowBlockerType blockerType;
    private String objectType;
    private String objectId;
    private String objectCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeProcessId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long processId;

    private String fieldCode;
    private String cellKey;
    private String reason;
    private String suggestion;
}
