package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshotContent;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityTrace;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

final class MesProFrontlineFeedbackSubmitSnapshotTestSupport {

    private MesProFrontlineFeedbackSubmitSnapshotTestSupport() {
    }

    static void stubAuthorization(MesFrontlineSubmitAuthorizationService authorizationService) {
        stubAuthorization(authorizationService, List.of(
                new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null,
                        java.math.BigDecimal.ONE),
                new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null,
                        java.math.BigDecimal.ONE)));
    }

    static void stubAuthorization(MesFrontlineSubmitAuthorizationService authorizationService,
                                  List<MesFrontlineProcessMaterial> materials) {
        Mockito.lenient().doAnswer(invocation -> {
            MesFrontlineSubmitIdentityCommand command = invocation.getArgument(0);
            MesFrontlineSessionSnapshotContent content = new MesFrontlineSessionSnapshotContent(
                    1L, command.loginUserId(), command.routeId(), command.routeProcessId(), command.processId(),
                    command.workstationId(), List.of(), List.of(
                    new MesFrontlineTeamDeviceOption(501L, "PT-A-03", "压力泵", "ACTIVE",
                            "DEFAULT", "SINGLE", List.of())),
                    List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗")),
                    materials,
                    null);
            MesFrontlineSessionSnapshot snapshot = new MesFrontlineSessionSnapshot(
                    command.frontlineSessionSnapshotId(), command.frontlineSessionSnapshotHash(), content);
            return new MesFrontlineSubmitIdentityTrace(command.loginUserId(), command.actualEmployeeId(),
                    command.signatureEmployeeId(), command.deviceId(), command.workstationId(), command.routeId(),
                    command.routeProcessId(), command.processId(), command.templateNo(),
                    command.frontlineSessionSnapshotId(), command.frontlineSessionSnapshotHash(), snapshot);
        }).when(authorizationService).authorize(any());
    }

    static void stubAuthorizationWithInputEvidence(MesFrontlineSubmitAuthorizationService authorizationService) {
        List<MesFrontlineProcessMaterial> materials = new java.util.ArrayList<>();
        materials.add(new MesFrontlineProcessMaterial(503L, "A003", "输入原料", null,
                MesFrontlineProcessMaterial.ROLE_INPUT, null, List.of("LOT-001", "LOT-002"),
                new java.math.BigDecimal("12"), new java.math.BigDecimal("10"),
                new java.math.BigDecimal("10"), List.of(101L, 102L), List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                List.of(1001L, 1002L),
                "input-source-hash"));
        materials.add(new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null,
                java.math.BigDecimal.ONE));
        materials.add(new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null,
                java.math.BigDecimal.ONE));
        stubAuthorization(authorizationService, List.copyOf(materials));
    }

}
