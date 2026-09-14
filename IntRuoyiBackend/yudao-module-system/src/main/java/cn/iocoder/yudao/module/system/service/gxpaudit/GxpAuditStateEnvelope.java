package cn.iocoder.yudao.module.system.service.gxpaudit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GxpAuditStateEnvelope {

    private String state;
    private String objectVersion;
    private String canonicalJson;

}
