package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrTravelerRespVO {

    private Long id;

    private String travelerCode;

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long productId;

    private String productCode;

    private String productName;

    private String serialNo;

    private String scopeType;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Long routeProcessId;

    private Integer routeProcessSort;

    private Long processId;

    private String processCode;

    private String processName;

    private String status;

    private String printStatus;

    private String businessKeyHash;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
