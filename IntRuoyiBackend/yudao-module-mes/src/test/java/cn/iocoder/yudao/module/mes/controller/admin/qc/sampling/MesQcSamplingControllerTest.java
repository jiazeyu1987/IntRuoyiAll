package cn.iocoder.yudao.module.mes.controller.admin.qc.sampling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MesQcSamplingControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var factory = new ProxyFactory(new MesQcSamplingController());
        factory.setProxyTargetClass(true);
        factory.addAdvice(new MethodValidationInterceptor());
        mvc = MockMvcBuilders.standaloneSetup(factory.getProxy())
                .setControllerAdvice(new GlobalExceptionHandler("mes-qc-sampling-test", null)).build();
    }

    @Test
    void lookupBindsTwoParametersAndSerializesStandardResult() throws Exception {
        mvc.perform(get("/mes/qc/sampling/lookup").param("totalQuantity", "3000").param("inspectionLevel", "II"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sampleCode").value("K"))
                .andExpect(jsonPath("$.data.nominalSampleSize").value(125))
                .andExpect(jsonPath("$.data.minQuantity").value(1201))
                .andExpect(jsonPath("$.data.maxQuantity").value(3200));
        mvc.perform(get("/mes/qc/sampling/table"))
                .andExpect(jsonPath("$.data.length()").value(15))
                .andExpect(jsonPath("$.data[0].samples.III.sampleCode").value("B"))
                .andExpect(jsonPath("$.data[14].samples.III.nominalSampleSize").value(2000));
    }

    @Test
    void invalidOrMissingParametersNeverReturnSuccessfulLookup() throws Exception {
        for (String quantity : new String[]{"1", "0", "-1", "1.5", "9223372036854775808"}) {
            mvc.perform(get("/mes/qc/sampling/lookup").param("totalQuantity", quantity).param("inspectionLevel", "II"))
                    .andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(get("/mes/qc/sampling/lookup").param("totalQuantity", "3000").param("inspectionLevel", "IV"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/mes/qc/sampling/lookup").param("totalQuantity", "3000"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
