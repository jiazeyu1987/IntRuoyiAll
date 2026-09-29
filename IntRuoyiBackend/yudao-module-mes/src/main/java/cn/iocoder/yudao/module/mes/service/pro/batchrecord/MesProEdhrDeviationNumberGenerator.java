package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationSequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_SEQUENCE_ALLOCATION_FAILED;

@Component
public class MesProEdhrDeviationNumberGenerator {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT);

    private final MesProEdhrDeviationSequenceMapper sequenceMapper;
    private final Clock clock;

    @Autowired
    public MesProEdhrDeviationNumberGenerator(MesProEdhrDeviationSequenceMapper sequenceMapper) {
        this(sequenceMapper, Clock.systemUTC());
    }

    MesProEdhrDeviationNumberGenerator(MesProEdhrDeviationSequenceMapper sequenceMapper, Clock clock) {
        this.sequenceMapper = sequenceMapper;
        this.clock = clock;
    }

    public String nextCode(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("DEVIATION_TENANT_REQUIRED");
        }
        String yearMonth = YearMonth.now(clock.withZone(BUSINESS_ZONE)).format(YEAR_MONTH_FORMAT);
        if (sequenceMapper.allocateNext(tenantId, yearMonth) <= 0) {
            throw exception(PRO_EDHR_DEVIATION_SEQUENCE_ALLOCATION_FAILED, tenantId, yearMonth);
        }
        Long value = sequenceMapper.selectLastInsertedId();
        if (value == null || value <= 0) {
            throw exception(PRO_EDHR_DEVIATION_SEQUENCE_ALLOCATION_FAILED, tenantId, yearMonth);
        }
        return "PC-" + yearMonth + "-" + String.format(Locale.ROOT, "%04d", value);
    }
}
