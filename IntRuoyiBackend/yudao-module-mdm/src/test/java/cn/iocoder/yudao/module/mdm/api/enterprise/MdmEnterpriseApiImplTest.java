package cn.iocoder.yudao.module.mdm.api.enterprise;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmEnterpriseApiImplTest {

    @Mock
    private MdmEnterpriseService enterpriseService;

    private MdmEnterpriseApiImpl enterpriseApi;

    @BeforeEach
    void setUp() {
        enterpriseApi = new MdmEnterpriseApiImpl();
        ReflectionTestUtils.setField(enterpriseApi, "enterpriseService", enterpriseService);
    }

    @Test
    void getEnabledEnterprisesReturnsCompleteTenantOwnedDtoEvidence() {
        MdmEnterpriseDO enterprise = MdmEnterpriseDO.builder()
                .id(101L)
                .enterpriseCode("COMP-001")
                .name("Owned company")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status(MdmEnterpriseStatusEnum.ENABLE.getStatus())
                .revision(4)
                .build();
        enterprise.setTenantId(11L);
        when(enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()))).thenReturn(List.of(enterprise));

        List<MdmEnterpriseRespDTO> result = enterpriseApi.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals(11L, result.get(0).getTenantId());
        assertEquals("COMP-001", result.get(0).getEnterpriseCode());
        assertEquals("Owned company", result.get(0).getName());
        assertEquals(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), result.get(0).getType());
        assertEquals(MdmEnterpriseStatusEnum.ENABLE.getStatus(), result.get(0).getStatus());
        assertEquals(4, result.get(0).getRevision());
    }

    @Test
    void getEnabledEnterprisesPropagatesWholeBatchFailureWithoutPartialDtos() {
        when(enterpriseService.getEnabledEnterprises(List.of(101L, 202L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenThrow(new ServiceException(MDM_ENTERPRISE_DISABLED));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> enterpriseApi.getEnabledEnterprises(List.of(101L, 202L),
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())));

        assertEquals(MDM_ENTERPRISE_DISABLED.getCode(), exception.getCode());
    }

}
