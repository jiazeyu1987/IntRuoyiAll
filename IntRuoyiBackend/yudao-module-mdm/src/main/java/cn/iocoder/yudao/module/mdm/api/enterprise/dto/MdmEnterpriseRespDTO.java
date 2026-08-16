package cn.iocoder.yudao.module.mdm.api.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmEnterpriseRespDTO {

    private Long id;
    private Long tenantId;
    private String enterpriseCode;
    private String name;
    private String type;
    private String status;
    private Integer revision;

}
