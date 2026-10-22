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
    static void stubAuthenticatedResources(MesProFrontlineFeedbackSubmitServiceImpl service,
            MesProductionAuthenticatedSignatureService productionSignatures) {
        var system = Mockito.mock(cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi.class);
        var employee = Mockito.mock(cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi.class);
        Mockito.lenient().when(system.identityDomain()).thenReturn("SYSTEM_USER");
        Mockito.lenient().when(employee.identityDomain()).thenReturn("MES_EMPLOYEE_PROFILE");
        Mockito.lenient().when(system.lockAndRevalidate(any(), any())).thenAnswer(i -> i.getArgument(0));
        Mockito.lenient().when(employee.lockAndRevalidate(any(), any())).thenAnswer(i -> i.getArgument(0));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"signatureIdentities",List.of(system,employee));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"signatureAuthority",
                Mockito.mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"signatureClock",
                java.time.Clock.fixed(java.time.Instant.parse("2026-10-10T01:00:00Z"),java.time.ZoneOffset.UTC));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"productionSignatures",productionSignatures);
        var evidence=Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService.class);
        Mockito.lenient().when(evidence.isValidForEvent(any())).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"productionReplayEvidence",evidence);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"productionReplayEvents",
                Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper.class));
    }

    /** These business-rule unit tests start at the authenticated executor; facade/auth tests are separate. */
    static cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO submit(
            MesProFrontlineFeedbackSubmitServiceImpl service,
            cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO request) {
        Long operator=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        // Invalid input tests must reach the executor's original context validation first.
        var signer=new cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner(1L,
                request.getSignatureIdentityDomain()==null?"SYSTEM_USER":request.getSignatureIdentityDomain(),
                request.getSignatureEmployeeId()==null?1L:request.getSignatureEmployeeId(),operator==null?1L:operator,
                "b8bd392c-08e8-4a44-94b4-bf58c0448888",1L,
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(request.getProcessPoolSubmissionIdempotencyKey()));
        var events=(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper)
                org.springframework.test.util.ReflectionTestUtils.getField(service,"productionReplayEvents");
        var event=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO();
        event.setId(801L);event.setRawPayload(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Map.of(MesProductionSubmitRequestIdentity.RAW_KEY,java.util.Map.of(
                        "version",MesProductionSubmitRequestIdentity.VERSION,"hash",MesProductionSubmitRequestIdentity.hash(request)))));
        Mockito.lenient().when(events.selectById(Mockito.anyLong())).thenReturn(event);
        return service.submitAuthenticated(request,signer);
    }

    private MesProFrontlineFeedbackSubmitSnapshotTestSupport() {
    }

    static void stubAuditIdentity(Object service) {
        var profiles = Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper.class);
        var users = Mockito.mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        // Fixture follows each real authorization result instead of accepting client display text.
        var resolver = Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineAuditIdentity.class);
        Mockito.lenient().when(resolver.production(any(), any())).thenAnswer(invocation -> {
            MesFrontlineSubmitIdentityTrace identity = invocation.getArgument(0);
            Long id = identity.actualEmployeeId();
            Mockito.when(profiles.selectList(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO>>any())).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO.builder()
                            .id(id + 100000).systemUserId(id).enabled(true).build()));
            Mockito.when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                    .setId(id).setStatus(0).setUsername("employee." + id).setNickname("正式员工" + id));
            return new cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineAuditIdentity(profiles, users)
                    .production(identity, "SYSTEM_USER");
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
