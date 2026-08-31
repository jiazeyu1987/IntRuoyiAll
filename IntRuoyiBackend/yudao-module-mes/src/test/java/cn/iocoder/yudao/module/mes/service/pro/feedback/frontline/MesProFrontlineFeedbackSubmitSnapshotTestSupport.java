package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshotContent;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityTrace;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

final class MesProFrontlineFeedbackSubmitSnapshotTestSupport {

    private MesProFrontlineFeedbackSubmitSnapshotTestSupport() {
    }

    static void stubAuthorization(MesFrontlineSubmitAuthorizationService authorizationService) {
        Mockito.lenient().when(authorizationService.authorize(any())).thenAnswer(invocation -> {
            MesFrontlineSubmitIdentityCommand command = invocation.getArgument(0);
            MesFrontlineSessionSnapshotContent content = new MesFrontlineSessionSnapshotContent(
                    1L, command.loginUserId(), command.routeId(), command.routeProcessId(), command.processId(),
                    command.workstationId(), List.of(), List.of(),
                    List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗")),
                    List.of(
                            new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null,
                                    java.math.BigDecimal.ONE),
                            new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null,
                                    java.math.BigDecimal.ONE)),
                    null);
            MesFrontlineSessionSnapshot snapshot = new MesFrontlineSessionSnapshot(
                    command.frontlineSessionSnapshotId(), command.frontlineSessionSnapshotHash(), content);
            return new MesFrontlineSubmitIdentityTrace(command.loginUserId(), command.actualEmployeeId(),
                    command.signatureEmployeeId(), command.deviceId(), command.workstationId(), command.routeId(),
                    command.routeProcessId(), command.processId(), command.templateNo(),
                    command.frontlineSessionSnapshotId(), command.frontlineSessionSnapshotHash(), snapshot);
        });
    }

}
