package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSessionSnapshotContent;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityTrace;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceParameterOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

final class MesProFrontlineFeedbackSubmitSnapshotTestSupport {

    private MesProFrontlineFeedbackSubmitSnapshotTestSupport() {
    }

    static void stubAuditIdentity(Object service) {
        var profiles = Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper.class);
        var users = Mockito.mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        // Fixture follows each real authorization result instead of accepting client display text.
        var resolver = Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineAuditIdentity.class);
        Mockito.lenient().when(resolver.production(any())).thenAnswer(invocation -> {
            MesFrontlineSubmitIdentityTrace identity = invocation.getArgument(0);
            Long id = identity.actualEmployeeId();
            Mockito.when(profiles.selectList(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO>>any())).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO.builder()
                            .id(id + 100000).systemUserId(id).enabled(true).build()));
            Mockito.when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                    .setId(id).setStatus(0).setUsername("employee." + id).setNickname("正式员工" + id));
            return new cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineAuditIdentity(profiles, users)
                    .production(identity);
        });
        org.springframework.test.util.ReflectionTestUtils.setField(service, "auditIdentity", resolver);
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
                            "DEFAULT", "SINGLE", List.of(new MesFrontlineDeviceParameterOption(
                            "pressure", "压力", "MPa", new java.math.BigDecimal("20"),
                            new java.math.BigDecimal("40"), new java.math.BigDecimal("30"),
                            "DECIMAL", "20-40MPa", List.of(), null, 0)))),
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
                MesFrontlineProcessMaterial.ROLE_INPUT, null, List.of(),
                null, null, null, List.of(), List.of(), List.of(), null));
        materials.add(new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null,
                java.math.BigDecimal.ONE));
        materials.add(new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null,
                java.math.BigDecimal.ONE));
        stubAuthorization(authorizationService, List.copyOf(materials));
    }

}
