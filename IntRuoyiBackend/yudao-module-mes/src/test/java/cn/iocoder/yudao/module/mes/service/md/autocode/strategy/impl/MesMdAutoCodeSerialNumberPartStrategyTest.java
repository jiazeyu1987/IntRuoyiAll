package cn.iocoder.yudao.module.mes.service.md.autocode.strategy.impl;

import cn.iocoder.yudao.framework.test.core.ut.BaseRedisUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.MesMdAutoCodePartDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.MesMdAutoCodeRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.MesMdAutoCodeRuleDO;
import cn.iocoder.yudao.module.mes.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.mes.dal.mysql.md.autocode.MesMdAutoCodeRecordMapper;
import cn.iocoder.yudao.module.mes.dal.redis.md.autocode.MesMdAutoCodeRedisDAO;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeCycleMethodEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.strategy.MesMdAutoCodeContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesMdAutoCodeSerialNumberPartStrategy} 的单元测试
 *
 * @author 瑛泰源码
 */
@Import({MesMdAutoCodeSerialNumberPartStrategy.class, MesMdAutoCodeRedisDAO.class})
public class MesMdAutoCodeSerialNumberPartStrategyTest extends BaseRedisUnitTest {

    @Resource
    private MesMdAutoCodeSerialNumberPartStrategy strategy;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @MockitoBean
    private MesMdAutoCodeRecordMapper recordMapper;
    @MockitoBean
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper processMapper;

    @BeforeEach
    public void resetAutoCodeCounters() {
        Set<String> keys = stringRedisTemplate.keys(RedisKeyConstants.AUTO_CODE + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    public void testGenerate_noCycle() {
        // 准备参数
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(1L);
        MesMdAutoCodeContext context = new MesMdAutoCodeContext().setRule(rule);

        // 调用
        String result = strategy.generate(part, context);
        // 断言
        assertEquals("0001", result);
        assertEquals(1, context.getSerialNo());
    }

    @Test
    public void testGenerate_withCycleByDay() {
        // 准备参数
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(1)
                .setCycleFlag(true).setCycleMethod(MesMdAutoCodeCycleMethodEnum.DAY.getMethod());
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(2L);
        MesMdAutoCodeContext context = new MesMdAutoCodeContext().setRule(rule);

        // 调用
        String result = strategy.generate(part, context);
        // 断言
        assertEquals("0001", result);
        assertEquals(1, context.getSerialNo());
    }

    @Test
    public void testGenerate_withCycleByInputChar() {
        // 准备参数
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(1)
                .setCycleFlag(true).setCycleMethod(MesMdAutoCodeCycleMethodEnum.INPUT_CHAR.getMethod());
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(3L);
        MesMdAutoCodeContext context = new MesMdAutoCodeContext().setRule(rule).setInputChar("A");

        // 调用
        String result = strategy.generate(part, context);
        // 断言
        assertEquals("0001", result);
        assertEquals(1, context.getSerialNo());
    }

    @Test
    public void testGenerate_withStep() {
        // 准备参数
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(5).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(4L);
        MesMdAutoCodeContext context1 = new MesMdAutoCodeContext().setRule(rule);
        MesMdAutoCodeContext context2 = new MesMdAutoCodeContext().setRule(rule);

        // 调用
        String result1 = strategy.generate(part, context1);
        String result2 = strategy.generate(part, context2);
        // 断言
        assertEquals("0001", result1);
        assertEquals(1, context1.getSerialNo());
        assertEquals("0006", result2);
        assertEquals(6, context2.getSerialNo());
    }

    @Test
    public void testGenerate_multipleCallsIncrement() {
        // 准备参数
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(5L);
        MesMdAutoCodeContext context1 = new MesMdAutoCodeContext().setRule(rule);
        MesMdAutoCodeContext context2 = new MesMdAutoCodeContext().setRule(rule);

        // 调用
        String result1 = strategy.generate(part, context1);
        String result2 = strategy.generate(part, context2);
        // 断言
        assertEquals("0001", result1);
        assertEquals("0002", result2);
    }

    @Test
    public void testGenerate_shouldResumeFromLatestRecordedSerialWhenRedisKeyMissing() {
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1).setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(6L);
        MesMdAutoCodeContext context1 = new MesMdAutoCodeContext().setRule(rule);
        MesMdAutoCodeContext context2 = new MesMdAutoCodeContext().setRule(rule);
        when(recordMapper.selectLatestSerialRecord(eq(6L), isNull(), eq(false), isNull(), isNull()))
                .thenReturn(MesMdAutoCodeRecordDO.builder().id(8L).ruleId(6L).serialNo(7L).result("PT-0007").build());

        String result1 = strategy.generate(part, context1);
        String result2 = strategy.generate(part, context2);

        assertEquals("0008", result1);
        assertEquals(8, context1.getSerialNo());
        assertEquals("0009", result2);
        assertEquals(9, context2.getSerialNo());
        verify(recordMapper, atLeastOnce()).selectLatestSerialRecord(eq(6L), isNull(), eq(false), isNull(), isNull());
    }

    @Test
    public void routeCodes_resumeBeyondDeletedHistoryAndExpandPastFourDigits() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(21L);
        try {
            MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1)
                    .setSerialStep(1).setCycleFlag(false);
            MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(99001L).setCode("PRO_ROUTE_CODE");
            when(recordMapper.selectRouteCodeHighWatermark(21L)).thenReturn(9999L);
            assertEquals("10000", strategy.generate(part, new MesMdAutoCodeContext().setRule(rule)));
            assertEquals("10001", strategy.generate(part, new MesMdAutoCodeContext().setRule(rule)));
            verify(recordMapper, times(2)).selectRouteCodeHighWatermark(21L);
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    public void routeCodes_concurrentCallsNeverReuseSerial() throws Exception {
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1)
                .setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(99002L).setCode("PRO_ROUTE_CODE");
        when(recordMapper.selectRouteCodeHighWatermark(21L)).thenReturn(200L);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            java.util.List<java.util.concurrent.Callable<String>> calls = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) calls.add(() -> {
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(21L);
                try {
                    return strategy.generate(part, new MesMdAutoCodeContext().setRule(rule));
                } finally {
                    cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
                }
            });
            Set<String> codes = new java.util.HashSet<>();
            for (java.util.concurrent.Future<String> future : executor.invokeAll(calls)) codes.add(future.get());
            assertEquals(64, codes.size());
            assertTrue(codes.stream().allMatch(code -> Long.parseLong(code) > 200L));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void routeCodes_missingHistoryResultFailsExplicitly() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(21L);
        try {
            MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1)
                    .setSerialStep(1).setCycleFlag(false);
            MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(99003L).setCode("PRO_ROUTE_CODE");
            when(recordMapper.selectRouteCodeHighWatermark(21L)).thenReturn(null);
            assertThrows(IllegalStateException.class,
                    () -> strategy.generate(part, new MesMdAutoCodeContext().setRule(rule)));
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    public void processGxSequence_isTenantScopedAndContinuesPastFourDigits() {
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1)
                .setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO firstRule = new MesMdAutoCodeRuleDO().setId(101L).setCode("PRO_PROCESS_GX_CODE");
        MesMdAutoCodeRuleDO secondRule = new MesMdAutoCodeRuleDO().setId(102L).setCode("PRO_PROCESS_GX_CODE");
        when(processMapper.selectProcessCodeHighWatermark(1L)).thenReturn(9999L);
        when(processMapper.selectProcessCodeHighWatermark(2L)).thenReturn(0L);
        try {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            assertEquals("10000", strategy.generate(part, new MesMdAutoCodeContext().setRule(firstRule)));
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(2L);
            assertEquals("0001", strategy.generate(part, new MesMdAutoCodeContext().setRule(secondRule)));
            assertEquals("0002", strategy.generate(part, new MesMdAutoCodeContext().setRule(secondRule)));
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            assertEquals("10001", strategy.generate(part, new MesMdAutoCodeContext().setRule(firstRule)));
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    public void processGxSequence_concurrentRequestsAllocateDistinctNumbers() throws Exception {
        when(processMapper.selectProcessCodeHighWatermark(3L)).thenReturn(0L);
        MesMdAutoCodePartDO part = new MesMdAutoCodePartDO().setLength(4).setSerialStartNo(1)
                .setSerialStep(1).setCycleFlag(false);
        MesMdAutoCodeRuleDO rule = new MesMdAutoCodeRuleDO().setId(103L).setCode("PRO_PROCESS_GX_CODE");
        var executor = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<String>>();
            for (int i = 0; i < 40; i++) {
                tasks.add(() -> {
                    cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(3L);
                    try { return strategy.generate(part, new MesMdAutoCodeContext().setRule(rule)); }
                    finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
                });
            }
            Set<String> codes = new java.util.HashSet<>();
            for (var result : executor.invokeAll(tasks)) codes.add(result.get());
            assertEquals(40, codes.size());
            assertTrue(codes.contains("0001"));
            assertTrue(codes.contains("0040"));
        } finally { executor.shutdownNow(); }
    }

}
