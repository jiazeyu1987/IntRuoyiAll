package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationSequenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationNumberGeneratorTest {

    @Mock
    private MesProEdhrDeviationSequenceMapper sequenceMapper;

    @Test
    void nextCodeUsesShanghaiCalendarMonthAtUtcMonthBoundary() {
        MesProEdhrDeviationNumberGenerator generator = new MesProEdhrDeviationNumberGenerator(
                sequenceMapper, Clock.fixed(Instant.parse("2026-09-30T16:30:00Z"), ZoneOffset.UTC));
        when(sequenceMapper.allocateNext(71L, "202610")).thenReturn(1);
        when(sequenceMapper.selectLastInsertedId()).thenReturn(1L);

        assertEquals("PC-202610-0001", generator.nextCode(71L));

        verify(sequenceMapper).allocateNext(71L, "202610");
    }

    @Test
    void nextCodeAcceptsMysqlDuplicateUpdateCountAndNaturallyExpandsPastFourDigits() {
        MesProEdhrDeviationNumberGenerator generator = new MesProEdhrDeviationNumberGenerator(
                sequenceMapper, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        when(sequenceMapper.allocateNext(71L, "202610")).thenReturn(2);
        when(sequenceMapper.selectLastInsertedId()).thenReturn(10_000L);

        assertEquals("PC-202610-10000", generator.nextCode(71L));
    }
}
