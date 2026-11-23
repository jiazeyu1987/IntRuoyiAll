package cn.iocoder.yudao.module.system.controller.admin.tenant;

import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.iocoder.yudao.framework.mybatis.core.type.StringListTypeHandler;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantSaveReqVOValidationTest {

    @Test
    void validatesTenantTextBoundariesForCreationAndUpdate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Long id : new Long[]{null, 123L}) {
                var request = validTenant(id);
                request.setName("租".repeat(30));
                request.setContactName("人".repeat(30));
                assertTrue(validator.validate(request).isEmpty());

                request.setName("租".repeat(31));
                var violations = validator.validate(request);
                assertEquals(1, violations.size());
                assertEquals("租户名长度不能超过30个字符", violations.iterator().next().getMessage());

                request.setName("正常租户");
                request.setContactName("人".repeat(31));
                violations = validator.validate(request);
                assertEquals(1, violations.size());
                assertEquals("联系人长度不能超过30个字符", violations.iterator().next().getMessage());
            }
        }
    }

    @Test
    void validatesPackageTextBoundariesForCreationAndUpdate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Long id : new Long[]{null, 123L}) {
                var request = new TenantPackageSaveReqVO().setId(id).setStatus(0).setMenuIds(Set.of(1L))
                        .setName("套".repeat(30)).setRemark("注".repeat(256));
                assertTrue(validator.validate(request).isEmpty());
                request.setRemark(null);
                assertTrue(validator.validate(request).isEmpty());
                request.setRemark("");
                assertTrue(validator.validate(request).isEmpty());

                request.setName("套".repeat(31));
                var violations = validator.validate(request);
                assertEquals(1, violations.size());
                assertEquals("套餐名长度不能超过30个字符", violations.iterator().next().getMessage());

                request.setName("正常套餐").setRemark("注".repeat(257));
                violations = validator.validate(request);
                assertEquals(1, violations.size());
                assertEquals("备注长度不能超过256个字符", violations.iterator().next().getMessage());
            }
        }
    }

    @Test
    void validatesWebsitesAgainstActualStoredLengthIncludingCommas() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Long id : new Long[]{null, 123L}) {
                var request = validTenant(id);
                for (List<String> websites : java.util.Arrays.<List<String>>asList(null, List.of(),
                        List.of("a".repeat(1024)), List.of("a".repeat(511), "b".repeat(512)))) {
                    request.setWebsites(websites);
                    assertTrue(validator.validate(request).isEmpty());
                }
                for (List<String> websites : List.of(List.of("a".repeat(1025)),
                        List.of("a".repeat(512), "b".repeat(512)))) {
                    request.setWebsites(websites);
                    var statement = mock(PreparedStatement.class);
                    new StringListTypeHandler().setParameter(statement, 1, websites, null);
                    verify(statement).setString(1, String.join(",", websites));
                    assertEquals(1025, String.join(",", websites).length());
                    var violations = validator.validate(request);
                    assertEquals(1, violations.size());
                    assertEquals("绑定域名总长度不能超过1024个字符（含分隔逗号）",
                            violations.iterator().next().getMessage());
                }
            }
        }
    }

    private TenantSaveReqVO validTenant(Long id) {
        return new TenantSaveReqVO().setId(id).setName("正常租户").setContactName("联系人")
                .setStatus(0).setPackageId(1L).setAccountCount(5000)
                .setExpireTime(LocalDateTime.of(2099, 1, 1, 0, 0))
                .setUsername("testadmin").setPassword("ValidationTest123");
    }

    @Test
    void validatesContactMobileForCreationAndUpdate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Long id : new Long[]{null, 123L}) {
                var request = new TenantSaveReqVO();
                request.setId(id);
                for (String mobile : new String[]{null, "", "13800138000", "15601691300"}) {
                    request.setContactMobile(mobile);
                    assertTrue(validator.validateProperty(request, "contactMobile").isEmpty());
                }
                for (String mobile : new String[]{"12312312312313", "1380013800", "138001380000",
                        "abcdefghijk", "           ", "12800138000", "+8613800138000", "008613800138000"}) {
                    request.setContactMobile(mobile);
                    var violations = validator.validateProperty(request, "contactMobile");
                    assertEquals(1, violations.size(), mobile);
                    assertEquals("联系手机格式不正确，请输入11位手机号码",
                            violations.iterator().next().getMessage());
                }
            }
        }
    }
}
