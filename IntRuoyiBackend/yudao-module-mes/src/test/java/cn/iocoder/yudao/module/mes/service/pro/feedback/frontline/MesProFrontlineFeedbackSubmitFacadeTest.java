package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.*;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureAuthenticationFacade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesProFrontlineFeedbackSubmitFacadeTest {
    @Test void clientCannotInjectAFormalSignatureToBypassAuthentication() {
        var auth=mock(ElectronicSignatureAuthenticationFacade.class);
        var executor=mock(MesProFrontlineFeedbackSubmitServiceImpl.class);
        var request=new MesProFrontlineFeedbackSubmitReqVO();request.setSignatureId(71L);
        assertThrows(IllegalArgumentException.class,()->new MesProFrontlineFeedbackSubmitFacade(auth,executor).submit(request));
        verifyNoInteractions(auth,executor);
    }
    @Test void authenticationAndDomainExecutionAreDifferentSpringTransactionBoundaries() throws Exception {
        var facade=MesProFrontlineFeedbackSubmitFacade.class.getMethod("submit",MesProFrontlineFeedbackSubmitReqVO.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        var executor=MesProFrontlineFeedbackSubmitServiceImpl.class.getMethod("submitAuthenticated",MesProFrontlineFeedbackSubmitReqVO.class,cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertEquals(org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED,facade.propagation());
        assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED,executor.propagation());
    }
}
