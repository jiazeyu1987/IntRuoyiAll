package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class EdhrLocalStateSampleRespVO {

    private Long batchExecutionId;

    private String batchExecutionCode;

    private String sampleState;

    private String detailPath;

    private Map<String, String> routeQuery;
}
