package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MesProRouteProcessFlowValidationMessageRespVO {

    private String level;

    private String code;

    private String message;

    private List<Long> routeProcessIds = new ArrayList<>();

    private List<Long> edgeIds = new ArrayList<>();

}
