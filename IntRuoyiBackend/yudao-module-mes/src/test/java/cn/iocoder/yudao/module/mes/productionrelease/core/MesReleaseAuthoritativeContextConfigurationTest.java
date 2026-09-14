package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesReleaseAuthoritativeContextConfigurationTest {

    @Test
    void registersExactlyOneBlockerPortWhenNoAuthoritativeAdapterIsPresent() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                MesReleaseAuthoritativeContextConfiguration.class)) {
            String[] beanNames = context.getBeanNamesForType(MesReleaseAuthoritativeContextPort.class);

            assertEquals(1, beanNames.length);
            assertInstanceOf(MesReleaseAuthoritativeContextUnavailablePort.class,
                    context.getBean(MesReleaseAuthoritativeContextPort.class));
        }
    }

    @Test
    void registersRealAdapterInsteadOfBlockerWhenAuthoritativeDependenciesArePresent() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerMock(context, MesProEdhrReleaseTransactionMapper.class);
            registerMock(context, MesProEdhrBatchExecutionMapper.class);
            registerMock(context, MesProcessPoolActiveOrderReleaseApplicationMapper.class);
            registerMock(context, MesProEdhrBatchExecutionOriginMapper.class);
            registerMock(context, MesProcessPoolActiveOrderMapper.class);
            registerMock(context, MesProcessPoolActiveOrderCompletionBackfillMapper.class);
            registerMock(context, MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
            registerMock(context, MesProEdhrBatchTraceabilityService.class);
            registerMock(context, MesIndependentBatchPrerequisiteReceiptService.class);
            context.register(MesReleaseAuthoritativeContextPortImpl.class,
                    MesReleaseAuthoritativeContextConfiguration.class);

            context.refresh();

            String[] beanNames = context.getBeanNamesForType(MesReleaseAuthoritativeContextPort.class);
            assertEquals(1, beanNames.length);
            assertInstanceOf(MesReleaseAuthoritativeContextPortImpl.class,
                    context.getBean(MesReleaseAuthoritativeContextPort.class));
        }
    }

    @Test
    void preservesStructuredBlockerUntilAuthoritativeAdaptersAreWired() {
        MesReleaseAuthoritativeContextUnavailablePort port = new MesReleaseAuthoritativeContextUnavailablePort();

        MesReleaseFlowBlockerException exception = assertThrows(MesReleaseFlowBlockerException.class,
                () -> port.require(new MesReleaseFinalizationCommand().setReleaseTransactionId(42L)));

        assertEquals(MesReleaseFlowBlockerType.AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED,
                exception.getFailure().getBlockers().get(0).getBlockerType());
    }

    private <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type) {
        context.registerBean(type, () -> Mockito.mock(type));
    }
}
