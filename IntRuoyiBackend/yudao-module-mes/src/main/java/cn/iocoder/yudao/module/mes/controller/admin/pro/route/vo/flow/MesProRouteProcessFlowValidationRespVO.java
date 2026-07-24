package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MesProRouteProcessFlowValidationRespVO {

    private Boolean valid;

    private String validationStatus;

    private Long graphVersion;

    private List<MesProRouteProcessFlowValidationMessageRespVO> validationMessages = new ArrayList<>();

    private List<List<Long>> cyclePaths = new ArrayList<>();

    private List<Long> invalidRouteProcessIds = new ArrayList<>();

    private List<Long> invalidEdgeIds = new ArrayList<>();

    private Map<Long, Long> routeProcessIdMap = new HashMap<>();

}
