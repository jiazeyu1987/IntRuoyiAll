package cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmEnterpriseRespVO {

    private Long id;
    private String enterpriseCode;
    private String name;
    private String type;
    private String status;
    private Integer revision;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
