package cn.iocoder.yudao.module.ai.framework.ai.core.model.siliconflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link SiliconFlowImageOptions} builder defaults regression test.
 */
public class SiliconFlowImageOptionsTest {

    @Test
    public void testBuilderKeepsDefaultValues() {
        SiliconFlowImageOptions options = SiliconFlowImageOptions.builder().build();

        assertEquals(1, options.getN());
        assertEquals(25, options.getNumInferenceSteps());
        assertEquals(0.75F, options.getGuidanceScale());
        assertNotNull(options.getSeed());
    }

}
