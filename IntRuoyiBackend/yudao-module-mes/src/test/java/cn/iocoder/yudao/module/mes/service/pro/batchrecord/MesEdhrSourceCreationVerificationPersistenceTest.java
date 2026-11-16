package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceOwnerIntegrity.sha;

/** Real SYSTEM writer/store/MyBatis/H2 support protocol fixture, explicitly TEST_ONLY from
 * construction. It is never the MES material/completion producer and cannot close joint #6.
 * No mocked core, manual JsonUtils.init, BaseDbUnitTest, mapper retry or external SQL resources.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MesEdhrSourceCreationVerificationPersistenceTest.Application.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:ac02_binding_r3;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:/sql/mes_edhr_source_creation_verifier_tables.sql",
        "spring.sql.init.data-locations=", "spring.sql.init.continue-on-error=false", "spring.sql.init.encoding=UTF-8",
        "yudao.info.base-package=cn.iocoder.yudao.ac02.binding.r3.scan_none",
        "mybatis-plus.global-config.db-config.id-type=AUTO", "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0", "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.configuration.local-cache-scope=STATEMENT"
})
class MesEdhrSourceCreationVerificationPersistenceTest {
    private static final long T = 76103L, A = 7610301L, C = 7610302L, B = 7610303L;
    private static final String URL = "jdbc:h2:mem:ac02_binding_r3;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String PV = "AC02_BINDING_R3_TEST_ONLY_V1";
    private static final String RID = "AC02_BINDING_R3_TEST_ACTIVATION";
    private static final String PH = "d9974029cb1619a1c0f183387a8ea5d58e19e2d1f8c04a5d91e16bcae83d745b";
    private static final String AH = "8c5f41dbb3874fb478ba1b6ada179081820ed6986ee4c0481cc072f902d3895d";
    // Exact R3 text bytes embedded, no additional (thirteenth) resource.
    private static final String SEED = "SU5TRVJUIElOVE8gZ3hwX2F1ZGl0X2xlZGdlcl9zZXF1ZW5jZSh0ZW5hbnRfaWQsbmV4dF9sZWRnZXJfc2VxdWVuY2UpIFZBTFVFUyg3NjEwMywxKTsKSU5TRVJUIElOVE8gZ3hwX2F1ZGl0X3BvbGljeV9vcGVyYXRpb24odGVuYW50X2lkLHBvbGljeV92ZXJzaW9uLG9wZXJhdGlvbl9pZCxzb3VyY2VfdHlwZSxzb3VyY2VfbG9jYXRvcixkb21haW4sc3ViamVjdF90eXBlLGFjdGlvbl90eXBlLHJlYXNvbl9wb2xpY3ksc2lnbmF0dXJlX3BvbGljeSxzdGF0ZV9wb2xpY3kscmV0ZW50aW9uX2NsYXNzLHRlc3RfaWRzLG93bmVyLGFwcGxpY2FiaWxpdHksYWN0aXZlKSBWQUxVRVMoNzYxMDMsJ0FDMDJfQklORElOR19SM19URVNUX09OTFlfVjEnLCdtZXMuYWN0aXZlLW9yZGVyLmNvbXBsZXRlJywnU0VSVklDRV9NRVRIT0QnLCdbImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5wcm9jZXNzcG9vbC50ZWFtLk1lc1RlYW1MZWFkZXJBY3RpdmVPcmRlckNvbXBsZXRpb25TZXJ2aWNlSW1wbCNjb21wbGV0ZSIsImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5wcm9jZXNzcG9vbC50ZWFtLk1lc1RlYW1MZWFkZXJBY3RpdmVPcmRlckNvbXBsZXRpb25TZXJ2aWNlSW1wbCNjb21wbGV0ZUZvclJlbGVhc2UiLCJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8ucHJvY2Vzc3Bvb2wudGVhbS5NZXNDb21wbGV0aW9uQWZmZWN0ZWRSb3dBdWRpdCNhcHBlbmQiXScsJ01FUycsJ0FDVElWRV9PUkRFUl9DT01QTEVUSU9OJywnQ09NUExFVEUnLCdTWVNURU0nLCdOT05FJywnQUJTRU5UX1RPX1BSRVNFTlQnLCdHWFBfTUVTX0NPTVBMRVRJT04nLCdbIkFDMDJfQklORElOR19SM19URVNUX0NPTVBMRVRJT04iXScsJ21lcy1vd25lcicsJ0dYUCcsVFJVRSk7CklOU0VSVCBJTlRPIGd4cF9hdWRpdF9wb2xpY3lfb3BlcmF0aW9uKHRlbmFudF9pZCxwb2xpY3lfdmVyc2lvbixvcGVyYXRpb25faWQsc291cmNlX3R5cGUsc291cmNlX2xvY2F0b3IsZG9tYWluLHN1YmplY3RfdHlwZSxhY3Rpb25fdHlwZSxyZWFzb25fcG9saWN5LHNpZ25hdHVyZV9wb2xpY3ksc3RhdGVfcG9saWN5LHJldGVudGlvbl9jbGFzcyx0ZXN0X2lkcyxvd25lcixhcHBsaWNhYmlsaXR5LGFjdGl2ZSkgVkFMVUVTKDc2MTAzLCdBQzAyX0JJTkRJTkdfUjNfVEVTVF9PTkxZX1YxJywnbWVzLmVkaHIubWF0ZXJpYWwtZ2F0ZS5yZWNlaXB0LmNyZWF0ZScsJ1NFUlZJQ0VfTUVUSE9EJywnWyJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8uYmF0Y2hyZWNvcmQuTWVzUmVsZWFzZU1hdGVyaWFsR2F0ZVJlY2VpcHRQb3J0SW1wbCNwZXJzaXN0UmVhZHkiXScsJ01FUycsJ0VESFJfTUFURVJJQUxfR0FURV9SRUNFSVBUJywnQ1JFQVRFJywnU1lTVEVNJywnTk9ORScsJ0FCU0VOVF9UT19QUkVTRU5UJywnR1hQX01FU19SRUxFQVNFJywnWyJBQzAyX0JJTkRJTkdfUjNfVEVTVF9NQVRFUklBTCJdJywnbWVzLW93bmVyJywnR1hQJyxUUlVFKTsKSU5TRVJUIElOVE8gZ3hwX2F1ZGl0X3BvbGljeV9vcGVyYXRpb24odGVuYW50X2lkLHBvbGljeV92ZXJzaW9uLG9wZXJhdGlvbl9pZCxzb3VyY2VfdHlwZSxzb3VyY2VfbG9jYXRvcixkb21haW4sc3ViamVjdF90eXBlLGFjdGlvbl90eXBlLHJlYXNvbl9wb2xpY3ksc2lnbmF0dXJlX3BvbGljeSxzdGF0ZV9wb2xpY3kscmV0ZW50aW9uX2NsYXNzLHRlc3RfaWRzLG93bmVyLGFwcGxpY2FiaWxpdHksYWN0aXZlKSBWQUxVRVMoNzYxMDMsJ0FDMDJfQklORElOR19SM19URVNUX09OTFlfVjEnLCdtZXMuaW5kZXBlbmRlbnQtYmF0Y2gtcHJlcmVxdWlzaXRlLXJlY2VpcHQuaXNzdWUnLCdTRVJWSUNFX01FVEhPRCcsJ1siY24uaW9jb2Rlci55dWRhby5tb2R1bGUubWVzLnNlcnZpY2UucHJvLmJhdGNocmVjb3JkLk1lc0luZGVwZW5kZW50QmF0Y2hQcmVyZXF1aXNpdGVSZWNlaXB0U2VydmljZUltcGwjaXNzdWUiXScsJ01FUycsJ0lOREVQRU5ERU5UX0JBVENIX1BSRVJFUVVJU0lURV9SRUNFSVBUJywnQ1JFQVRFJywnVVNFUl9SRVFVSVJFRCcsJ05PTkUnLCdBQlNFTlRfVE9fUFJFU0VOVCcsJ0dYUF9NRVNfUkVDRUlQVCcsJ1siQUMwMl9CSU5ESU5HX1IzX1RFU1RfSVNTVUUiXScsJ21lcy1vd25lcicsJ0dYUCcsVFJVRSk7CklOU0VSVCBJTlRPIGd4cF9hdWRpdF9wb2xpY3lfb3BlcmF0aW9uKHRlbmFudF9pZCxwb2xpY3lfdmVyc2lvbixvcGVyYXRpb25faWQsc291cmNlX3R5cGUsc291cmNlX2xvY2F0b3IsZG9tYWluLHN1YmplY3RfdHlwZSxhY3Rpb25fdHlwZSxyZWFzb25fcG9saWN5LHNpZ25hdHVyZV9wb2xpY3ksc3RhdGVfcG9saWN5LHJldGVudGlvbl9jbGFzcyx0ZXN0X2lkcyxvd25lcixhcHBsaWNhYmlsaXR5LGFjdGl2ZSkgVkFMVUVTKDc2MTAzLCdBQzAyX0JJTkRJTkdfUjNfVEVTVF9PTkxZX1YxJywnbWVzLmluZGVwZW5kZW50LWJhdGNoLXByZXJlcXVpc2l0ZS1yZWNlaXB0LnJldm9rZScsJ1NFUlZJQ0VfTUVUSE9EJywnWyJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8uYmF0Y2hyZWNvcmQuTWVzSW5kZXBlbmRlbnRCYXRjaFByZXJlcXVpc2l0ZVJlY2VpcHRTZXJ2aWNlSW1wbCNyZXZva2UiXScsJ01FUycsJ0lOREVQRU5ERU5UX0JBVENIX1BSRVJFUVVJU0lURV9SRUNFSVBUJywnUkVWT0tFJywnVVNFUl9SRVFVSVJFRCcsJ05PTkUnLCdQUkVTRU5UX1RPX1BSRVNFTlQnLCdHWFBfTUVTX1JFQ0VJUFQnLCdbIkFDMDJfQklORElOR19SM19URVNUX1JFVk9LRSJdJywnbWVzLW93bmVyJywnR1hQJyxUUlVFKTsKSU5TRVJUIElOVE8gZ3hwX2F1ZGl0X3BvbGljeV9vcGVyYXRpb24odGVuYW50X2lkLHBvbGljeV92ZXJzaW9uLG9wZXJhdGlvbl9pZCxzb3VyY2VfdHlwZSxzb3VyY2VfbG9jYXRvcixkb21haW4sc3ViamVjdF90eXBlLGFjdGlvbl90eXBlLHJlYXNvbl9wb2xpY3ksc2lnbmF0dXJlX3BvbGljeSxzdGF0ZV9wb2xpY3kscmV0ZW50aW9uX2NsYXNzLHRlc3RfaWRzLG93bmVyLGFwcGxpY2FiaWxpdHksYWN0aXZlKSBWQUxVRVMoNzYxMDMsJ0FDMDJfQklORElOR19SM19URVNUX09OTFlfVjEnLCdtZXMubWFya2V0LXJlbGVhc2UubWFuYWdlci1zdGFnZS5pbml0aWFsaXplJywnU0VSVklDRV9NRVRIT0QnLCdbImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5wcm9kdWN0aW9ucmVsZWFzZS5tYW5hZ2VyLk1lc1Byb2R1Y3Rpb25SZWxlYXNlTWFuYWdlclN0YWdlSW5pdGlhbGl6ZXJJbXBsI2luaXRpYWxpemVNYW5hZ2VyUmVsZWFzZVN0YWdlIl0nLCdNRVMnLCdSRUxFQVNFX1RSQU5TQUNUSU9OJywnSU5JVElBTElaRV9NQU5BR0VSX1NUQUdFJywnU1lTVEVNJywnTk9ORScsJ0FCU0VOVF9PUl9QUkVTRU5UX1RPX1BSRVNFTlQnLCdHWFBfTUVTX1JFTEVBU0UnLCdbIkFDMDJfQklORElOR19SM19URVNUX1JFTEFURURfTUFOQUdFRCJdJywnbWVzLW93bmVyJywnR1hQJyxUUlVFKTsKSU5TRVJUIElOVE8gZ3hwX2F1ZGl0X3BvbGljeV9vcGVyYXRpb24odGVuYW50X2lkLHBvbGljeV92ZXJzaW9uLG9wZXJhdGlvbl9pZCxzb3VyY2VfdHlwZSxzb3VyY2VfbG9jYXRvcixkb21haW4sc3ViamVjdF90eXBlLGFjdGlvbl90eXBlLHJlYXNvbl9wb2xpY3ksc2lnbmF0dXJlX3BvbGljeSxzdGF0ZV9wb2xpY3kscmV0ZW50aW9uX2NsYXNzLHRlc3RfaWRzLG93bmVyLGFwcGxpY2FiaWxpdHksYWN0aXZlKSBWQUxVRVMoNzYxMDMsJ0FDMDJfQklORElOR19SM19URVNUX09OTFlfVjEnLCdtZXMubWFya2V0LXJlbGVhc2Uuc3VibWl0JywnU0VSVklDRV9NRVRIT0QnLCdbImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5iYXRjaHJlY29yZC5NZXNQcm9FZGhyUmVsZWFzZVNlcnZpY2VJbXBsI3N1Ym1pdCIsImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5iYXRjaHJlY29yZC5NZXNQcm9FZGhyUmVsZWFzZVNlcnZpY2VJbXBsI3N1Ym1pdEZvckFwcHJvdmFsIl0nLCdNRVMnLCdSRUxFQVNFX1RSQU5TQUNUSU9OJywnU1VCTUlUJywnU1lTVEVNX1dJVEhfT1BUSU9OQUxfVEVYVCcsJ05PTkUnLCdQUkVTRU5UX1RPX1BSRVNFTlQnLCdHWFBfTUVTX1JFTEVBU0UnLCdbIkFDMDJfQklORElOR19SM19URVNUX1JFTEFURURfU1VCTUlUIl0nLCdtZXMtb3duZXInLCdHWFAnLFRSVUUpOwpJTlNFUlQgSU5UTyBneHBfYXVkaXRfcG9saWN5X2FjdGl2YXRpb24odGVuYW50X2lkLHBvbGljeV92ZXJzaW9uLHBvbGljeV9oYXNoLHByZXZpb3VzX2FjdGl2YXRpb25faGFzaCxyZXF1ZXN0X2lkLGFjdG9yX2lkLGFwcHJvdmFsX3JlZmVyZW5jZSxhY3RpdmF0ZWRfYXRfdXRjLGVmZmVjdGl2ZV9hZnRlcl9zZXF1ZW5jZSxjYW5vbmljYWxfYWN0aXZhdGlvbl9qc29uLGFjdGl2YXRpb25faGFzaCkgVkFMVUVTKDc2MTAzLCdBQzAyX0JJTkRJTkdfUjNfVEVTVF9PTkxZX1YxJywnZDk5NzQwMjljYjE2MTlhMWMwZjE4MzM4N2E4ZWE1ZDU4ZTE5ZTJkMWY4YzA0YTVkOTFlMTZiY2FlODNkNzQ1YicsTlVMTCwnQUMwMl9CSU5ESU5HX1IzX1RFU1RfQUNUSVZBVElPTicsNzYxMDMwMSwnVEVTVF9PTkxZX0FDMDJfQklORElOR19SM19SRVNPVVJDRV9SRVZJRVdfQ0FORElEQVRFJyxUSU1FU1RBTVAgJzIwMjYtMTAtMTAgMDA6MDA6MDAnLDAsJ3siYXBwcm92YWxSZWZlcmVuY2UiOiJURVNUX09OTFlfQUMwMl9CSU5ESU5HX1IzX1JFU09VUkNFX1JFVklFV19DQU5ESURBVEUiLCJlZmZlY3RpdmVBZnRlclNlcXVlbmNlIjowLCJwb2xpY3lIYXNoIjoiZDk5NzQwMjljYjE2MTlhMWMwZjE4MzM4N2E4ZWE1ZDU4ZTE5ZTJkMWY4YzA0YTVkOTFlMTZiY2FlODNkNzQ1YiIsInBvbGljeVZlcnNpb24iOiJBQzAyX0JJTkRJTkdfUjNfVEVTVF9PTkxZX1YxIiwicHJldmlvdXNBY3RpdmF0aW9uSGFzaCI6bnVsbCwicmVxdWVzdElkIjoiQUMwMl9CSU5ESU5HX1IzX1RFU1RfQUNUSVZBVElPTiIsInRlbmFudElkIjo3NjEwM30nLCc4YzVmNDFkYmIzODc0ZmI0NzhiYTFiNmFkYTE3OTA4MTgyMGVkNjk4NmVlNGMwNDgxY2MwNzJmOTAyZDM4OTVkJyk7";
    private static final String CLEANUP = "REVMRVRFIEZST00gZ3hwX2F1ZGl0X2V2ZW50X3JlbGF0aW9uIFdIRVJFIHRlbmFudF9pZD03NjEwMzsKREVMRVRFIEZST00gbWVzX2VkaHJfc291cmNlX2NyZWF0aW9uX2JpbmRpbmcgV0hFUkUgdGVuYW50X2lkPTc2MTAzOwpERUxFVEUgRlJPTSBneHBfYXVkaXRfZXZlbnQgV0hFUkUgdGVuYW50X2lkPTc2MTAzOwpERUxFVEUgRlJPTSBtZXNfaW5kZXBlbmRlbnRfYmF0Y2hfcHJlcmVxdWlzaXRlX3JlY2VpcHQgV0hFUkUgdGVuYW50X2lkPTc2MTAzOwpERUxFVEUgRlJPTSBtZXNfcHJvX2VkaHJfbWF0ZXJpYWxfZ2F0ZV9yZWNlaXB0IFdIRVJFIHRlbmFudF9pZD03NjEwMzsKREVMRVRFIEZST00gbWVzX3Byb19wcm9jZXNzX3Bvb2xfYWN0aXZlX29yZGVyX2NvbXBsZXRpb25fcmVjZWlwdCBXSEVSRSB0ZW5hbnRfaWQ9NzYxMDM7CkRFTEVURSBGUk9NIGd4cF9hdWRpdF9wb2xpY3lfYWN0aXZhdGlvbiBXSEVSRSB0ZW5hbnRfaWQ9NzYxMDMgQU5EIHBvbGljeV92ZXJzaW9uPSdBQzAyX0JJTkRJTkdfUjNfVEVTVF9PTkxZX1YxJzsKREVMRVRFIEZST00gZ3hwX2F1ZGl0X3BvbGljeV9vcGVyYXRpb24gV0hFUkUgdGVuYW50X2lkPTc2MTAzIEFORCBwb2xpY3lfdmVyc2lvbj0nQUMwMl9CSU5ESU5HX1IzX1RFU1RfT05MWV9WMSc7CkRFTEVURSBGUk9NIGd4cF9hdWRpdF9sZWRnZXJfc2VxdWVuY2UgV0hFUkUgdGVuYW50X2lkPTc2MTAzOw==";
    private static final String POLICY = "eyJmaXh0dXJlS2luZCI6IkFDMDJfQklORElOR19SM19URVNUX09OTFlfUE9MSUNZIiwicG9saWN5VmVyc2lvbiI6IkFDMDJfQklORElOR19SM19URVNUX09OTFlfVjEiLCJvcGVyYXRpb25zIjpbeyJvcGVyYXRpb25JZCI6Im1lcy5hY3RpdmUtb3JkZXIuY29tcGxldGUiLCJzb3VyY2VUeXBlIjoiU0VSVklDRV9NRVRIT0QiLCJzb3VyY2VMb2NhdG9ycyI6WyJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8ucHJvY2Vzc3Bvb2wudGVhbS5NZXNUZWFtTGVhZGVyQWN0aXZlT3JkZXJDb21wbGV0aW9uU2VydmljZUltcGwjY29tcGxldGUiLCJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8ucHJvY2Vzc3Bvb2wudGVhbS5NZXNUZWFtTGVhZGVyQWN0aXZlT3JkZXJDb21wbGV0aW9uU2VydmljZUltcGwjY29tcGxldGVGb3JSZWxlYXNlIiwiY24uaW9jb2Rlci55dWRhby5tb2R1bGUubWVzLnNlcnZpY2UucHJvLnByb2Nlc3Nwb29sLnRlYW0uTWVzQ29tcGxldGlvbkFmZmVjdGVkUm93QXVkaXQjYXBwZW5kIl0sImRvbWFpbiI6Ik1FUyIsInN1YmplY3RUeXBlIjoiQUNUSVZFX09SREVSX0NPTVBMRVRJT04iLCJhY3Rpb25UeXBlIjoiQ09NUExFVEUiLCJyZWFzb25Qb2xpY3kiOiJTWVNURU0iLCJzaWduYXR1cmVQb2xpY3kiOiJOT05FIiwic3RhdGVQb2xpY3kiOiJBQlNFTlRfVE9fUFJFU0VOVCIsInNuYXBzaG90UHJvZmlsZSI6IkNPTVBMRVRJT04iLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfQ09NUExFVElPTiIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfQ09NUExFVElPTiJdLCJvd25lclJvbGUiOiJtZXMtb3duZXIiLCJhcHBsaWNhYmlsaXR5IjoiR1hQIn0seyJvcGVyYXRpb25JZCI6Im1lcy5lZGhyLm1hdGVyaWFsLWdhdGUucmVjZWlwdC5jcmVhdGUiLCJzb3VyY2VUeXBlIjoiU0VSVklDRV9NRVRIT0QiLCJzb3VyY2VMb2NhdG9ycyI6WyJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8uYmF0Y2hyZWNvcmQuTWVzUmVsZWFzZU1hdGVyaWFsR2F0ZVJlY2VpcHRQb3J0SW1wbCNwZXJzaXN0UmVhZHkiXSwiZG9tYWluIjoiTUVTIiwic3ViamVjdFR5cGUiOiJFREhSX01BVEVSSUFMX0dBVEVfUkVDRUlQVCIsImFjdGlvblR5cGUiOiJDUkVBVEUiLCJyZWFzb25Qb2xpY3kiOiJTWVNURU0iLCJzaWduYXR1cmVQb2xpY3kiOiJOT05FIiwic3RhdGVQb2xpY3kiOiJBQlNFTlRfVE9fUFJFU0VOVCIsInNuYXBzaG90UHJvZmlsZSI6IkFUVEFDSE1FTlQiLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfUkVMRUFTRSIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfTUFURVJJQUwiXSwib3duZXJSb2xlIjoibWVzLW93bmVyIiwiYXBwbGljYWJpbGl0eSI6IkdYUCJ9LHsib3BlcmF0aW9uSWQiOiJtZXMuaW5kZXBlbmRlbnQtYmF0Y2gtcHJlcmVxdWlzaXRlLXJlY2VpcHQuaXNzdWUiLCJzb3VyY2VUeXBlIjoiU0VSVklDRV9NRVRIT0QiLCJzb3VyY2VMb2NhdG9ycyI6WyJjbi5pb2NvZGVyLnl1ZGFvLm1vZHVsZS5tZXMuc2VydmljZS5wcm8uYmF0Y2hyZWNvcmQuTWVzSW5kZXBlbmRlbnRCYXRjaFByZXJlcXVpc2l0ZVJlY2VpcHRTZXJ2aWNlSW1wbCNpc3N1ZSJdLCJkb21haW4iOiJNRVMiLCJzdWJqZWN0VHlwZSI6IklOREVQRU5ERU5UX0JBVENIX1BSRVJFUVVJU0lURV9SRUNFSVBUIiwiYWN0aW9uVHlwZSI6IkNSRUFURSIsInJlYXNvblBvbGljeSI6IlVTRVJfUkVRVUlSRUQiLCJzaWduYXR1cmVQb2xpY3kiOiJOT05FIiwic3RhdGVQb2xpY3kiOiJBQlNFTlRfVE9fUFJFU0VOVCIsInNuYXBzaG90UHJvZmlsZSI6IlBST0RVQ1RJT04iLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfUkVDRUlQVCIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfSVNTVUUiXSwib3duZXJSb2xlIjoibWVzLW93bmVyIiwiYXBwbGljYWJpbGl0eSI6IkdYUCJ9LHsib3BlcmF0aW9uSWQiOiJtZXMuaW5kZXBlbmRlbnQtYmF0Y2gtcHJlcmVxdWlzaXRlLXJlY2VpcHQucmV2b2tlIiwic291cmNlVHlwZSI6IlNFUlZJQ0VfTUVUSE9EIiwic291cmNlTG9jYXRvcnMiOlsiY24uaW9jb2Rlci55dWRhby5tb2R1bGUubWVzLnNlcnZpY2UucHJvLmJhdGNocmVjb3JkLk1lc0luZGVwZW5kZW50QmF0Y2hQcmVyZXF1aXNpdGVSZWNlaXB0U2VydmljZUltcGwjcmV2b2tlIl0sImRvbWFpbiI6Ik1FUyIsInN1YmplY3RUeXBlIjoiSU5ERVBFTkRFTlRfQkFUQ0hfUFJFUkVRVUlTSVRFX1JFQ0VJUFQiLCJhY3Rpb25UeXBlIjoiUkVWT0tFIiwicmVhc29uUG9saWN5IjoiVVNFUl9SRVFVSVJFRCIsInNpZ25hdHVyZVBvbGljeSI6Ik5PTkUiLCJzdGF0ZVBvbGljeSI6IlBSRVNFTlRfVE9fUFJFU0VOVCIsInNuYXBzaG90UHJvZmlsZSI6IlBST0RVQ1RJT04iLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfUkVDRUlQVCIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfUkVWT0tFIl0sIm93bmVyUm9sZSI6Im1lcy1vd25lciIsImFwcGxpY2FiaWxpdHkiOiJHWFAifSx7Im9wZXJhdGlvbklkIjoibWVzLm1hcmtldC1yZWxlYXNlLm1hbmFnZXItc3RhZ2UuaW5pdGlhbGl6ZSIsInNvdXJjZVR5cGUiOiJTRVJWSUNFX01FVEhPRCIsInNvdXJjZUxvY2F0b3JzIjpbImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5wcm9kdWN0aW9ucmVsZWFzZS5tYW5hZ2VyLk1lc1Byb2R1Y3Rpb25SZWxlYXNlTWFuYWdlclN0YWdlSW5pdGlhbGl6ZXJJbXBsI2luaXRpYWxpemVNYW5hZ2VyUmVsZWFzZVN0YWdlIl0sImRvbWFpbiI6Ik1FUyIsInN1YmplY3RUeXBlIjoiUkVMRUFTRV9UUkFOU0FDVElPTiIsImFjdGlvblR5cGUiOiJJTklUSUFMSVpFX01BTkFHRVJfU1RBR0UiLCJyZWFzb25Qb2xpY3kiOiJTWVNURU0iLCJzaWduYXR1cmVQb2xpY3kiOiJOT05FIiwic3RhdGVQb2xpY3kiOiJBQlNFTlRfT1JfUFJFU0VOVF9UT19QUkVTRU5UIiwic25hcHNob3RQcm9maWxlIjoiTUFSS0VUX1JFTEVBU0UiLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfUkVMRUFTRSIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfUkVMQVRFRF9NQU5BR0VEIl0sIm93bmVyUm9sZSI6Im1lcy1vd25lciIsImFwcGxpY2FiaWxpdHkiOiJHWFAifSx7Im9wZXJhdGlvbklkIjoibWVzLm1hcmtldC1yZWxlYXNlLnN1Ym1pdCIsInNvdXJjZVR5cGUiOiJTRVJWSUNFX01FVEhPRCIsInNvdXJjZUxvY2F0b3JzIjpbImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5iYXRjaHJlY29yZC5NZXNQcm9FZGhyUmVsZWFzZVNlcnZpY2VJbXBsI3N1Ym1pdCIsImNuLmlvY29kZXIueXVkYW8ubW9kdWxlLm1lcy5zZXJ2aWNlLnByby5iYXRjaHJlY29yZC5NZXNQcm9FZGhyUmVsZWFzZVNlcnZpY2VJbXBsI3N1Ym1pdEZvckFwcHJvdmFsIl0sImRvbWFpbiI6Ik1FUyIsInN1YmplY3RUeXBlIjoiUkVMRUFTRV9UUkFOU0FDVElPTiIsImFjdGlvblR5cGUiOiJTVUJNSVQiLCJyZWFzb25Qb2xpY3kiOiJTWVNURU1fV0lUSF9PUFRJT05BTF9URVhUIiwic2lnbmF0dXJlUG9saWN5IjoiTk9ORSIsInN0YXRlUG9saWN5IjoiUFJFU0VOVF9UT19QUkVTRU5UIiwic25hcHNob3RQcm9maWxlIjoiTUFSS0VUX1JFTEVBU0UiLCJyZXRlbnRpb25DbGFzcyI6IkdYUF9NRVNfUkVMRUFTRSIsInRlc3RJZHMiOlsiQUMwMl9CSU5ESU5HX1IzX1RFU1RfUkVMQVRFRF9TVUJNSVQiXSwib3duZXJSb2xlIjoibWVzLW93bmVyIiwiYXBwbGljYWJpbGl0eSI6IkdYUCJ9XX0=";
    private static final List<String> TABLES = List.of("gxp_audit_event_relation", "mes_edhr_source_creation_binding", "gxp_audit_event",
            "mes_independent_batch_prerequisite_receipt", "mes_pro_edhr_material_gate_receipt", "mes_pro_process_pool_active_order_completion_receipt",
            "gxp_audit_policy_activation", "gxp_audit_policy_operation", "gxp_audit_ledger_sequence");

    @Resource private ApplicationContext context;
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private ObjectMapper objectMapper;
    @Resource private JsonUtils jsonUtils;
    @Resource private GxpAuditService writer;
    @Resource private GxpCreationAuditVerificationService shared;
    @Resource private MesEdhrSourceCreationBindingStore store;
    @Resource private MesEdhrSourceCreationVerificationService reader;
    @Resource private MesEdhrSourceOwnerIntegrity owner;
    @Resource private MesEdhrSourceCreationProtocolRegistry registry;
    @Resource private MesIndependentBatchPrerequisiteReceiptMapper independent;
    @Resource private MesProEdhrMaterialGateReceiptMapper materials;
    @Resource private MesProcessPoolActiveOrderCompletionReceiptMapper completions;
    @Resource private MesEdhrSourceCreationBindingMapper bindingMapper;
    @Resource private GxpAuditEventMapper events;
    @Resource private GxpAuditEventRelationMapper relations;
    @Resource private GxpAuditLedgerSequenceMapper ledger;
    @Resource private GxpAuditPolicyOperationMapper operations;
    @Resource private GxpAuditPolicyActivationMapper activations;
    private JdbcTemplate jdbc;
    private final Map<String, Set<Long>> ownedIds = new LinkedHashMap<>();
    private final Map<String, List<Map<String,Object>>> seedRows = new LinkedHashMap<>();
    private final Map<String, List<Map<String,Object>>> ownedRows = new LinkedHashMap<>();

    @BeforeAll static void freshProcess() {
        System.out.println("AC02_PROCESS class=" + MesEdhrSourceCreationVerificationPersistenceTest.class.getName()
                + " pid=" + ProcessHandle.current().pid() + " jvmStartTime="
                + java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
    }
    @BeforeEach void seedExactEightRows() throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(1, context.getBeansOfType(DataSource.class).size()); assertInstanceOf(DriverManagerDataSource.class, dataSource);
        assertEquals(URL, ((DriverManagerDataSource) dataSource).getUrl());
        try (var connection = dataSource.getConnection()) { assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:ac02_binding_r3")); }
        TenantContextHolder.setTenantId(T); TenantContextHolder.setIgnore(false); login(A);
        jdbc = new JdbcTemplate(dataSource); assertSame(objectMapper, JsonUtils.getObjectMapper()); assertNotNull(jsonUtils);
        verifyFrozenMetadata();
        assertFreshEmpty(); ownedIds.clear(); seedRows.clear(); ownedRows.clear();
        for(String table:TABLES) {ownedIds.put(table,new LinkedHashSet<>());ownedRows.put(table,new ArrayList<>());}
        assertEquals("9c5a230164e0c8685d1ea6a0ff44be26efdda26cd0075e3c4494a439acf8071b", sha(text(SEED)));
        tx().executeWithoutResult(status -> {
            for(String sql:statements(SEED)) insertCheckedSeed(sql);
        });
        try(var connection=dataSource.getConnection()) {
            var fresh=new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
            verifyDurableSeeds(fresh);
            for(String table:TABLES.subList(0,6)) assertTrue(snapshot(fresh,table).isEmpty(),table);
        }
        assertEquals(6L, count("gxp_audit_policy_operation")); assertEquals(1L, count("gxp_audit_policy_activation")); assertEquals(1L, count("gxp_audit_ledger_sequence"));
        assertEquals(1L, ledger.selectById(T).getNextLedgerSequence());
        assertEquals(PH, sha(text(POLICY))); var activation = activations.selectLatest(T);
        assertEquals(PV, activation.getPolicyVersion()); assertEquals(RID, activation.getRequestId()); assertEquals(PH, activation.getPolicyHash());
        assertEquals(AH, activation.getActivationHash()); assertEquals(AH, sha(activation.getCanonicalActivationJson()));
        assertEquals("TEST_ONLY_AC02_BINDING_R3_RESOURCE_REVIEW_CANDIDATE", activation.getApprovalReference());
    }
    @AfterEach void checkedOwnedCleanup() throws Exception {
        try {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(URL, ((DriverManagerDataSource) dataSource).getUrl());
            assertEquals("52038f217549350361b18429b720036730bdd9b295d73fadb4d95de16f46af58", sha(text(CLEANUP)));
            TransactionTemplate clean = tx(); clean.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            clean.executeWithoutResult(status -> {
                assertGlobalOwnership(jdbc);
                List<String> sql=statements(CLEANUP); assertEquals(9,sql.size());
                for(int i=0;i<TABLES.size();i++) {
                    String table=TABLES.get(i);var ids=ids(snapshot(jdbc,table),table);
                    assertEquals(ownedIds.get(table),new LinkedHashSet<>(ids),"owned actual IDs "+table);
                    assertEquals(ids.size(),new HashSet<>(ids).size(),"actual ID multiset "+table);
                    assertEquals(ids.size(),jdbc.update(sql.get(i)),"deleted actual ID multiset "+table);
                }
            });
            // Cleanup transaction has committed. This is a new physical JDBC connection/session,
            // outside the cleanup transaction; emptiness cannot be its own uncommitted observation.
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); assertFreshEmpty();
        } finally { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }
    }
    private static String text(String base64) { return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8); }
    private static List<String> statements(String base64) { return Arrays.stream(text(base64).split(";\\s*")).filter(s -> !s.isBlank()).toList(); }
    private TransactionTemplate tx() { return new TransactionTemplate(transactionManager); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", Long.class, T); }
    private void login(long id) {
        var user = new LoginUser(); user.setId(id); user.setTenantId(T); user.setUserType(2);
        user.setInfo(Map.of("username", "TEST_ONLY_" + id, LoginUser.INFO_KEY_NICKNAME, "TEST_ONLY_" + id));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
    private record SupportMaterial(MesProEdhrMaterialGateReceiptDO row, GxpAuditCommand command,
            MesEdhrSourceCreationBindingStore.StoredBindingFact binding, GxpAuditAppendResult event) {}
    private SupportMaterial writeSupportMaterial(String suffix, long batch) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive()); writer.acquireLedgerLock();
        var row = new MesProEdhrMaterialGateReceiptDO(); row.setTenantId(T); row.setBatchExecutionId(batch); row.setVersion(1);
        row.setReceiptId("TEST_ONLY_R3_" + suffix); row.setIssuedBy(A); row.setGateStatus("MATERIALS_READY"); row.setAuditEventId("TEST_ONLY_R3_PROTOCOL_FIXTURE");
        row.setMaterialTypeKeysJson("[\"FINISHED_PRODUCT_INSPECTION_RECORD\",\"FINISHED_PRODUCT_INSPECTION_REPORT\",\"INCOMING_INSPECTION_REPORT\",\"STERILIZATION_REPORT\"]");
        row.setManifestHash("a".repeat(64)); row.setSourceSnapshotHash("b".repeat(64)); row.setMaterialVersionSetHash("c".repeat(64));
        row.setReceiptHash(sha(T + "|" + batch + "|" + row.getReceiptId() + "|MATERIALS_READY|" + row.getMaterialTypeKeysJson() + "|"
                + row.getManifestHash() + "|" + row.getSourceSnapshotHash() + "|" + row.getMaterialVersionSetHash() + "|" + A + "|" + row.getAuditEventId() + "|1"));
        assertEquals(1, materials.insert(row)); assertNotNull(row.getId()); assertTrue(row.getId() > 0);
        var types = owner.materialTypes(row);
        ObjectNode request = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1").put("requestKind", "MATERIAL_READY_REQUEST_V1")
                .put("tenantId", T).put("batchExecutionId", batch).put("version", 1).put("issuedBy", A)
                .put("manifestHash", row.getManifestHash()).put("sourceSnapshotHash", row.getSourceSnapshotHash()).put("materialVersionSetHash", row.getMaterialVersionSetHash());
        request.set("materialTypeKeys", types); var encoded = MesEdhrSourceCreationCodec.encodeMaterialReadyRequestV1(request);
        ObjectNode state = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1").put("stateKind", "MATERIAL_READY_STATE_V1")
                .put("sourceRowId", row.getId()).put("receiptId", row.getReceiptId()).put("gateStatus", row.getGateStatus())
                .put("materialTypeKeysJsonRaw", row.getMaterialTypeKeysJson()).put("legacyOwnerAuditToken", row.getAuditEventId())
                .put("receiptHash", row.getReceiptHash()).put("requestHashVersion", "MATERIAL_READY_REQUEST_V1").put("requestHash", encoded.sha256());
        state.set("request", MesEdhrSourceCreationCodec.decodeMaterialReadyRequestV1(encoded.utf8Bytes()));
        String key = MesEdhrSourceCreationCodec.materialCreateAuditKey(T, batch, 1);
        var command = GxpAuditCommand.builder().eventSchemaVersion(2).operationId("mes.edhr.material-gate.receipt.create")
                .subjectId("MATERIAL_GATE_RECEIPT:" + row.getReceiptId()).subjectVersion("1")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").objectVersion("1").canonicalJson("{}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("MATERIALS_READY").objectVersion("1")
                        .canonicalJson(new String(MesEdhrSourceCreationCodec.encodeMaterialReadyStateV1(state).utf8Bytes(), StandardCharsets.UTF_8)).build())
                .reason("四材料门禁就绪回执创建").reasonCode("MES_EDHR_MATERIALS_READY").reasonSource("SYSTEM")
                .idempotencyKey(key).requestId(key).resultStatus("SUCCESS").sourceType("SERVICE_METHOD")
                .sourceLocator("cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesReleaseMaterialGateReceiptPortImpl#persistReady")
                .links(List.of(new GxpAuditRelation("SOURCE", "EDHR_MATERIAL_GATE_RECEIPT", row.getId().toString(), "1", row.getReceiptHash())))
                .evidences(List.of(new GxpAuditEvidence("MATERIAL_GATE_RECEIPT", row.getReceiptId(), "1", row.getReceiptHash(), "CREATION"))).build();
        var event = writer.append(command);
        var binding = store.insert(new MesEdhrSourceCreationBindingStore.BindingFact(T, "MATERIAL_READY_V1", row.getId(), row.getReceiptId(), "1", row.getReceiptHash(),
                "MATERIAL_READY_REQUEST_V1", encoded.sha256(), event.eventId(), event.eventHash()));
        assertTrue(binding.id() > 0); assertEquals(event.eventHash(), shared.requireVerifiedEvent(T, event.eventId()).eventHash());
        registerCommitOwnership("mes_pro_edhr_material_gate_receipt",Set.of(row.getId()));
        registerCommitOwnership("gxp_audit_event",Set.of(event.eventId()));
        registerCommitOwnership("mes_edhr_source_creation_binding",Set.of(binding.id()));
        var relationRows=jdbc.queryForList("SELECT * FROM gxp_audit_event_relation WHERE tenant_id=? AND event_id=? ORDER BY id",T,event.eventId());
        assertEquals(1,relationRows.size());
        registerCommitOwnership("gxp_audit_event_relation",new LinkedHashSet<>(ids(relationRows,"gxp_audit_event_relation")));
        return new SupportMaterial(row, command, binding, event);
    }
    @Test void exactAssemblyUsesRealProductionJacksonProxyAndAllNineMappers() {
        assertSame(objectMapper, JsonUtils.getObjectMapper()); assertTrue(AopUtils.isAopProxy(writer)); assertTrue(AopUtils.isAopProxy(store));
        assertInstanceOf(org.springframework.jdbc.datasource.DataSourceTransactionManager.class, transactionManager);
        assertEquals("9007199254740990", JsonUtils.toJsonString(9007199254740990L));
        assertEquals("\"9007199254740991\"", JsonUtils.toJsonString(9007199254740991L));
        assertEquals("\"-9007199254740991\"", JsonUtils.toJsonString(-9007199254740991L));
        assertEquals("\"9223372036854775807\"", JsonUtils.toJsonString(Long.MAX_VALUE));
        assertEquals("9007199254740989", JsonUtils.toJsonString(9007199254740989L));
        assertEquals("\"9007199254740992\"", JsonUtils.toJsonString(9007199254740992L));
        assertEquals("\"9007199254740993\"", JsonUtils.toJsonString(9007199254740993L));
        assertEquals("-9007199254740989", JsonUtils.toJsonString(-9007199254740989L));
        assertEquals("-9007199254740990", JsonUtils.toJsonString(-9007199254740990L));
        assertEquals("\"-9007199254740992\"", JsonUtils.toJsonString(-9007199254740992L));
        assertEquals("\"-9007199254740993\"", JsonUtils.toJsonString(-9007199254740993L));
        assertEquals("\"-9223372036854775808\"", JsonUtils.toJsonString(Long.MIN_VALUE));
        long millis = LocalDateTime.of(2026, 10, 10, 0, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(Long.toString(millis), JsonUtils.toJsonString(LocalDateTime.of(2026, 10, 10, 0, 0)));
        tx().executeWithoutResult(s -> {
            assertNotNull(ledger.selectByTenantIdForUpdate(T)); assertNotNull(operations.selectByPolicyVersionForUpdate(T, PV, "mes.edhr.material-gate.receipt.create"));
            assertNotNull(activations.selectLatestForUpdate(T)); assertEquals(0L, events.selectMaxLedgerSequence(T));
            assertTrue(relations.selectEventIdsByTargets(T, List.of("EDHR_MATERIAL_GATE_RECEIPT"), "999").isEmpty());
            assertNull(independent.selectByReceiptId(T, "TEST_ONLY_ABSENT")); assertNull(materials.selectByReceiptId(T, 999L, "TEST_ONLY_ABSENT"));
            assertNull(completions.selectByIdAndTenantId(999L, T)); assertTrue(bindingMapper.selectByCreationEvent(T, 999L).isEmpty());
        });
    }
    @Test void realWriterAndStoreRequireSameOuterPhysicalTransaction() {
        assertThrows(IllegalTransactionStateException.class, writer::acquireLedgerLock);
        assertThrows(IllegalTransactionStateException.class, () -> writer.append(GxpAuditCommand.builder().build()));
        assertThrows(IllegalTransactionStateException.class, () -> store.insert(new MesEdhrSourceCreationBindingStore.BindingFact(
                T, "MATERIAL_READY_V1", 1L, "TEST_ONLY", "1", "a".repeat(64), "MATERIAL_READY_REQUEST_V1", "b".repeat(64), 1L, "c".repeat(64))));
        assertEquals(0L, count("gxp_audit_event")); assertEquals(1L, ledger.selectById(T).getNextLedgerSequence());
    }
    @Test void commitThenNewReaderObjectsKeepOriginalCreatorAndAllBindingFacts() {
        SupportMaterial support = tx().execute(s -> writeSupportMaterial("COMMIT", 9007199254740991L)); assertNotNull(support);
        login(B); tx().executeWithoutResult(s -> {
            var newReader = new MesEdhrSourceCreationVerificationService(materials, completions, owner,
                    new MesEdhrSourceCreationBindingStore(bindingMapper), shared, new MesEdhrSourceCreationProtocolRegistry());
            var proof = newReader.requireMaterialCreation(T, support.row().getBatchExecutionId(), support.row().getReceiptId());
            assertEquals("MATERIAL_READY_V1", proof.ownerProtocol()); assertEquals(support.event().eventId(), proof.creationEventId());
            assertEquals(support.event().eventHash(), proof.creationEventHash()); assertEquals(support.row().getReceiptHash(), proof.sourceHash());
            assertEquals(support.row().getId(), proof.sourceRowId()); assertEquals(support.row().getReceiptId(), proof.sourceReceiptId()); assertEquals("1", proof.sourceVersion());
            assertEquals(List.of(support.binding()), store.readBySourceRow(T, "MATERIAL_READY_V1", support.row().getId()));
            assertEquals(List.of(support.binding()), store.readByReceiptVersion(T, "MATERIAL_READY_V1", support.row().getReceiptId(), "1"));
            assertEquals(List.of(support.binding()), store.readByCreationEvent(T, support.event().eventId()));
            assertEquals(A, events.selectById(support.event().eventId()).getActorId());
        });
    }
    @Test void explicitRollbackRemovesSourceEventRelationsBindingAndLedgerAdvance() {
        var baseline=freshSnapshot();
        var failure = new IllegalStateException("TEST_ONLY_ROLLBACK");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> tx().executeWithoutResult(s -> { writeSupportMaterial("ROLLBACK", 101L); throw failure; })));
        assertEquals(baseline,freshSnapshot(),"explicit rollback exact full typed fresh-session baseline");
        for (String table : TABLES.subList(0, 6)) assertEquals(0L, count(table));
        assertEquals(1L, ledger.selectById(T).getNextLedgerSequence());
    }
    @ParameterizedTest @ValueSource(strings = {"sourceRow", "receiptVersion", "creationEvent"})
    void eachIndependentUniqueConstraintForcesWholeParticipantRollback(String constraint) {
        var support = tx().execute(s -> writeSupportMaterial("UNIQUE", 102L)); assertNotNull(support);
        var baseline=freshSnapshot();
        assertThrows(UnexpectedRollbackException.class, () -> tx().executeWithoutResult(status -> {
            var fresh=writeSupportMaterial("UNIQUE_PENDING_"+constraint,104L);
            assertEquals(2L,count("mes_pro_edhr_material_gate_receipt"));assertEquals(2L,count("gxp_audit_event"));
            assertEquals(2L,count("gxp_audit_event_relation"));assertEquals(2L,count("mes_edhr_source_creation_binding"));
            assertEquals(3L,ledger.selectById(T).getNextLedgerSequence());
            var original=support.binding().fact(); var f=fresh.binding().fact();
            var bad = new MesEdhrSourceCreationBindingStore.BindingFact(T,f.ownerProtocol(),
                    constraint.equals("sourceRow")?original.sourceRowId():f.sourceRowId()+100,
                    constraint.equals("receiptVersion")?original.sourceReceiptId():"TEST_ONLY_CONFLICT_"+constraint,
                    f.sourceVersion(),f.sourceHash(),f.requestHashVersion(),f.requestHash(),
                    constraint.equals("creationEvent")?original.creationEventId():f.creationEventId()+100,f.creationEventHash());
            assertThrows(org.springframework.dao.DuplicateKeyException.class,()->store.insert(bad));
        }));
        assertEquals(baseline,freshSnapshot(),"unique-triggered rollback all nine tables exact typed baseline");
        assertEquals(1L,count("mes_edhr_source_creation_binding"));assertEquals(1L,count("gxp_audit_event"));
        assertEquals(2L,ledger.selectById(T).getNextLedgerSequence());
    }
    @Test void committedReplayKeepsSameEventAndNoExtraLedgerOrPhysicalRelations() {
        var support = tx().execute(s -> writeSupportMaterial("REPLAY", 103L)); assertNotNull(support);
        var result = tx().execute(s -> writer.append(support.command())); assertNotNull(result);
        assertTrue(result.replayed()); assertEquals(support.event().eventId(), result.eventId()); assertEquals(support.event().eventHash(), result.eventHash());
        assertEquals(1L, count("gxp_audit_event")); assertEquals(1L, count("gxp_audit_event_relation")); assertEquals(2L, ledger.selectById(T).getNextLedgerSequence());
    }
    private static final List<String> FROZEN_COLUMNS = List.of(
            "gxp_audit_event|id|-5|64|0|0",
            "gxp_audit_event|tenant_id|-5|64|0|0",
            "gxp_audit_event|ledger_sequence|-5|64|0|0",
            "gxp_audit_event|operation_id|12|128|0|0",
            "gxp_audit_event|domain|12|64|0|0",
            "gxp_audit_event|subject_type|12|64|0|0",
            "gxp_audit_event|subject_id|12|256|0|0",
            "gxp_audit_event|subject_version|12|128|0|0",
            "gxp_audit_event|action|12|64|0|0",
            "gxp_audit_event|reason|12|500|0|0",
            "gxp_audit_event|actor_id|-5|64|0|0",
            "gxp_audit_event|actor_username|12|64|0|0",
            "gxp_audit_event|actor_display_name|12|128|0|0",
            "gxp_audit_event|server_occurred_at|93|26|6|0",
            "gxp_audit_event|before_state|12|32|0|0",
            "gxp_audit_event|before_object_version|12|128|0|1",
            "gxp_audit_event|before_state_json|2005|2147483647|0|1",
            "gxp_audit_event|after_state|12|32|0|0",
            "gxp_audit_event|after_object_version|12|128|0|1",
            "gxp_audit_event|after_state_json|2005|2147483647|0|1",
            "gxp_audit_event|policy_version|12|128|0|0",
            "gxp_audit_event|idempotency_key|12|128|0|0",
            "gxp_audit_event|request_id|12|128|0|1",
            "gxp_audit_event|signature_record_id|12|128|0|1",
            "gxp_audit_event|signature_content_hash|12|128|0|1",
            "gxp_audit_event|idempotency_payload_hash|12|64|0|0",
            "gxp_audit_event|canonical_event_json|2005|2147483647|0|0",
            "gxp_audit_event|previous_event_hash|12|64|0|1",
            "gxp_audit_event|event_hash|12|64|0|0",
            "gxp_audit_event|algorithm|12|32|0|0",
            "gxp_audit_event|event_schema_version|5|16|0|1",
            "gxp_audit_event|canonicalization_version|12|32|0|1",
            "gxp_audit_event|result_status|12|16|0|1",
            "gxp_audit_event|reason_code|12|64|0|1",
            "gxp_audit_event|reason_source|12|16|0|1",
            "gxp_audit_event|transaction_id|12|64|0|1",
            "gxp_audit_event|authenticated_actor_json|2005|2147483647|0|1",
            "gxp_audit_event|performed_by_json|2005|2147483647|0|1",
            "gxp_audit_event|source_type|12|64|0|1",
            "gxp_audit_event|source_locator|12|512|0|1",
            "gxp_audit_event|trace_id|12|128|0|1",
            "gxp_audit_event|error_code|12|128|0|1",
            "gxp_audit_event|attempted_operation_id|12|128|0|1",
            "gxp_audit_event|relation_manifest_json|2005|2147483647|0|1",
            "gxp_audit_event|evidence_manifest_json|2005|2147483647|0|1",
            "gxp_audit_event|state_payload_hash|12|64|0|1",
            "gxp_audit_event|creator|12|64|0|1",
            "gxp_audit_event|create_time|93|26|6|0",
            "gxp_audit_event|updater|12|64|0|1",
            "gxp_audit_event|update_time|93|26|6|0",
            "gxp_audit_event|deleted|16|1|0|0",
            "gxp_audit_ledger_sequence|tenant_id|-5|64|0|0",
            "gxp_audit_ledger_sequence|next_ledger_sequence|-5|64|0|0",
            "gxp_audit_ledger_sequence|create_time|93|26|6|0",
            "gxp_audit_ledger_sequence|update_time|93|26|6|0",
            "gxp_audit_policy_operation|id|-5|64|0|0",
            "gxp_audit_policy_operation|tenant_id|-5|64|0|0",
            "gxp_audit_policy_operation|policy_version|12|128|0|0",
            "gxp_audit_policy_operation|operation_id|12|128|0|0",
            "gxp_audit_policy_operation|source_type|12|64|0|0",
            "gxp_audit_policy_operation|source_locator|12|512|0|0",
            "gxp_audit_policy_operation|domain|12|64|0|0",
            "gxp_audit_policy_operation|subject_type|12|64|0|0",
            "gxp_audit_policy_operation|action_type|12|64|0|0",
            "gxp_audit_policy_operation|reason_policy|12|64|0|0",
            "gxp_audit_policy_operation|signature_policy|12|64|0|0",
            "gxp_audit_policy_operation|state_policy|12|64|0|0",
            "gxp_audit_policy_operation|retention_class|12|64|0|0",
            "gxp_audit_policy_operation|test_ids|12|512|0|0",
            "gxp_audit_policy_operation|owner|12|128|0|0",
            "gxp_audit_policy_operation|applicability|12|64|0|0",
            "gxp_audit_policy_operation|active|16|1|0|0",
            "gxp_audit_policy_operation|creator|12|64|0|1",
            "gxp_audit_policy_operation|create_time|93|26|6|0",
            "gxp_audit_policy_operation|updater|12|64|0|1",
            "gxp_audit_policy_operation|update_time|93|26|6|0",
            "gxp_audit_policy_operation|deleted|16|1|0|0",
            "gxp_audit_event_relation|id|-5|64|0|0",
            "gxp_audit_event_relation|tenant_id|-5|64|0|0",
            "gxp_audit_event_relation|event_id|-5|64|0|0",
            "gxp_audit_event_relation|relation_type|12|32|0|0",
            "gxp_audit_event_relation|target_type|12|64|0|0",
            "gxp_audit_event_relation|target_id|12|128|0|0",
            "gxp_audit_event_relation|target_version|12|128|0|1",
            "gxp_audit_event_relation|target_hash|12|64|0|1",
            "gxp_audit_event_relation|created_at_utc|93|26|6|0",
            "gxp_audit_policy_activation|id|-5|64|0|0",
            "gxp_audit_policy_activation|tenant_id|-5|64|0|0",
            "gxp_audit_policy_activation|policy_version|12|128|0|0",
            "gxp_audit_policy_activation|policy_hash|12|64|0|0",
            "gxp_audit_policy_activation|previous_activation_hash|12|64|0|1",
            "gxp_audit_policy_activation|request_id|12|128|0|0",
            "gxp_audit_policy_activation|actor_id|-5|64|0|0",
            "gxp_audit_policy_activation|approval_reference|12|256|0|0",
            "gxp_audit_policy_activation|activated_at_utc|93|26|6|0",
            "gxp_audit_policy_activation|effective_after_sequence|-5|64|0|0",
            "gxp_audit_policy_activation|canonical_activation_json|2005|2147483647|0|0",
            "gxp_audit_policy_activation|activation_hash|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|receipt_id|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|tenant_id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|entry_type|12|32|0|0",
            "mes_independent_batch_prerequisite_receipt|work_order_id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|work_order_code|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|route_id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|route_version_id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|route_version|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|batch_code|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|source_relation_id|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|source_relation_version|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|source_relation_snapshot_hash|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|source_object_type|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|source_object_id|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|material_source_type|12|64|0|0",
            "mes_independent_batch_prerequisite_receipt|material_source_id|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|source_context_hash|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|source_snapshot_hash|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|business_reason|12|512|0|0",
            "mes_independent_batch_prerequisite_receipt|issuer_system|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|issuer_user_id|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|issuer_user_role|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|issued_at|93|19|0|0",
            "mes_independent_batch_prerequisite_receipt|expires_at|93|19|0|0",
            "mes_independent_batch_prerequisite_receipt|revoked_at|93|19|0|1",
            "mes_independent_batch_prerequisite_receipt|revocation_reason|12|512|0|1",
            "mes_independent_batch_prerequisite_receipt|credential_version|-5|64|0|0",
            "mes_independent_batch_prerequisite_receipt|status|12|16|0|0",
            "mes_independent_batch_prerequisite_receipt|canonical_payload|2005|2147483647|0|0",
            "mes_independent_batch_prerequisite_receipt|source_evidence_json|2005|2147483647|0|0",
            "mes_independent_batch_prerequisite_receipt|receipt_hash|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|payload_hash|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|signature|1|64|0|0",
            "mes_independent_batch_prerequisite_receipt|audit_event_id|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|idempotency_key|12|128|0|0",
            "mes_independent_batch_prerequisite_receipt|creator|12|64|0|1",
            "mes_independent_batch_prerequisite_receipt|create_time|93|19|0|0",
            "mes_independent_batch_prerequisite_receipt|updater|12|64|0|1",
            "mes_independent_batch_prerequisite_receipt|update_time|93|19|0|0",
            "mes_independent_batch_prerequisite_receipt|deleted|16|1|0|0",
            "mes_pro_edhr_material_gate_receipt|id|-5|64|0|0",
            "mes_pro_edhr_material_gate_receipt|receipt_id|12|128|0|0",
            "mes_pro_edhr_material_gate_receipt|tenant_id|-5|64|0|0",
            "mes_pro_edhr_material_gate_receipt|batch_execution_id|-5|64|0|0",
            "mes_pro_edhr_material_gate_receipt|gate_status|12|32|0|0",
            "mes_pro_edhr_material_gate_receipt|material_type_keys_json|12|512|0|0",
            "mes_pro_edhr_material_gate_receipt|manifest_hash|1|64|0|0",
            "mes_pro_edhr_material_gate_receipt|source_snapshot_hash|1|64|0|0",
            "mes_pro_edhr_material_gate_receipt|material_version_set_hash|1|64|0|0",
            "mes_pro_edhr_material_gate_receipt|receipt_hash|1|64|0|0",
            "mes_pro_edhr_material_gate_receipt|issued_by|-5|64|0|0",
            "mes_pro_edhr_material_gate_receipt|audit_event_id|12|128|0|0",
            "mes_pro_edhr_material_gate_receipt|version|4|32|0|0",
            "mes_pro_edhr_material_gate_receipt|creator|12|64|0|1",
            "mes_pro_edhr_material_gate_receipt|create_time|93|19|0|0",
            "mes_pro_edhr_material_gate_receipt|updater|12|64|0|1",
            "mes_pro_edhr_material_gate_receipt|update_time|93|19|0|0",
            "mes_pro_edhr_material_gate_receipt|deleted|16|1|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|active_order_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|work_order_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|batch_code|12|128|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|route_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|route_version_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|leader_user_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|request_idempotency_key|12|128|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|request_payload_hash|1|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|source_snapshot_hash|1|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|formal_source_snapshot_json|2005|2147483647|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|signature_snapshot_json|2005|2147483647|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|receipt_hash|1|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|expected_version|4|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|completed_version|4|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|receipt_status|12|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|completion_status|12|32|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|batch_record_status|12|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|process_inspection_status|12|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|batch_record_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|process_inspection_id|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|loss_report_status|12|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|has_actual_loss|16|1|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|loss_quantity|3|24|6|0",
            "mes_pro_process_pool_active_order_completion_receipt|loss_record_id|-5|64|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|zero_loss_confirmation_snapshot|2005|2147483647|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|loss_condition_facts_json|2005|2147483647|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|batch_record_source_ids_json|2005|2147483647|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|process_inspection_source_ids_json|2005|2147483647|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|loss_source_hash|1|64|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|provision_handoff|12|32|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|completed_at|93|19|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|completed_by|-5|64|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|creator|12|64|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|create_time|93|19|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|updater|12|64|0|1",
            "mes_pro_process_pool_active_order_completion_receipt|update_time|93|19|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|deleted|16|1|0|0",
            "mes_pro_process_pool_active_order_completion_receipt|tenant_id|-5|64|0|0",
            "mes_edhr_source_creation_binding|id|-5|64|0|0",
            "mes_edhr_source_creation_binding|tenant_id|-5|64|0|0",
            "mes_edhr_source_creation_binding|owner_protocol|12|64|0|0",
            "mes_edhr_source_creation_binding|source_row_id|-5|64|0|0",
            "mes_edhr_source_creation_binding|source_receipt_id|12|128|0|0",
            "mes_edhr_source_creation_binding|source_version|12|64|0|0",
            "mes_edhr_source_creation_binding|source_hash|1|64|0|0",
            "mes_edhr_source_creation_binding|request_hash_version|12|32|0|0",
            "mes_edhr_source_creation_binding|request_hash|1|64|0|0",
            "mes_edhr_source_creation_binding|creation_event_id|-5|64|0|0",
            "mes_edhr_source_creation_binding|creation_event_hash|1|64|0|0");
    private static final List<String> FROZEN_INDEXES = List.of(
            "gxp_audit_event|id|true",
            "gxp_audit_event|tenant_id,ledger_sequence|true",
            "gxp_audit_event|tenant_id,idempotency_key|true",
            "gxp_audit_ledger_sequence|tenant_id|true",
            "gxp_audit_policy_operation|id|true",
            "gxp_audit_policy_operation|tenant_id,operation_id,policy_version|true",
            "gxp_audit_event_relation|id|true",
            "gxp_audit_event_relation|tenant_id,event_id,relation_type,target_type,target_id|true",
            "gxp_audit_policy_activation|id|true",
            "gxp_audit_policy_activation|tenant_id,request_id|true",
            "gxp_audit_policy_activation|tenant_id,activation_hash|true",
            "mes_independent_batch_prerequisite_receipt|id|true",
            "mes_independent_batch_prerequisite_receipt|tenant_id,receipt_id,deleted|true",
            "mes_independent_batch_prerequisite_receipt|tenant_id,entry_type,idempotency_key,deleted|true",
            "mes_pro_edhr_material_gate_receipt|id|true",
            "mes_pro_edhr_material_gate_receipt|tenant_id,receipt_id,deleted|true",
            "mes_pro_edhr_material_gate_receipt|tenant_id,batch_execution_id,version,deleted|true",
            "mes_pro_process_pool_active_order_completion_receipt|id|true",
            "mes_pro_process_pool_active_order_completion_receipt|tenant_id,active_order_id,deleted|true",
            "mes_pro_process_pool_active_order_completion_receipt|tenant_id,request_idempotency_key,deleted|true",
            "mes_edhr_source_creation_binding|id|true",
            "mes_edhr_source_creation_binding|tenant_id,owner_protocol,source_row_id|true",
            "mes_edhr_source_creation_binding|tenant_id,owner_protocol,source_receipt_id,source_version|true",
            "mes_edhr_source_creation_binding|tenant_id,creation_event_id|true",
            "mes_independent_batch_prerequisite_receipt|tenant_id,source_relation_id,source_relation_version|false",
            "mes_independent_batch_prerequisite_receipt|tenant_id,source_snapshot_hash|false",
            "mes_independent_batch_prerequisite_receipt|tenant_id,status,expires_at|false",
            "mes_pro_edhr_material_gate_receipt|tenant_id,batch_execution_id,version,deleted|false",
            "mes_pro_process_pool_active_order_completion_receipt|tenant_id,source_snapshot_hash|false",
            "mes_pro_process_pool_active_order_completion_receipt|tenant_id,work_order_id|false");
    private void verifyFrozenMetadata() throws Exception {
        try(var connection=dataSource.getConnection()) {
            var metadata=connection.getMetaData();var actualTables=new TreeSet<String>();
            try(var rows=metadata.getTables(null,"public","%",null)) {while(rows.next()){assertEquals("BASE TABLE",rows.getString("TABLE_TYPE"));assertTrue(actualTables.add(rows.getString("TABLE_NAME")));}}
            assertEquals(new TreeSet<>(TABLES),actualTables,"actual closed nine-table schema before seed");
            var actualColumns=new ArrayList<String>();var actualIndexes=new ArrayList<String>();
            var actualOrdinaryNames=new TreeSet<String>();
            for(String table:TABLES) {
                try(var columns=metadata.getColumns(null,"public",table,"%")) {
                    while(columns.next())actualColumns.add(table+"|"+columns.getString("COLUMN_NAME")+"|"+columns.getInt("DATA_TYPE")+"|"+columns.getInt("COLUMN_SIZE")+"|"+columns.getInt("DECIMAL_DIGITS")+"|"+columns.getInt("NULLABLE"));
                }
                var primary=new TreeMap<Short,String>();
                try(var keys=metadata.getPrimaryKeys(null,"public",table)){while(keys.next())assertNull(primary.put(keys.getShort("KEY_SEQ"),keys.getString("COLUMN_NAME")));}
                assertEquals(List.of(table.equals("gxp_audit_ledger_sequence")?"tenant_id":"id"),new ArrayList<>(primary.values()),"primary key "+table);
                var indexes=new LinkedHashMap<String,TreeMap<Short,String>>();var unique=new LinkedHashMap<String,Boolean>();
                try(var rows=metadata.getIndexInfo(null,"public",table,false,false)) {
                    while(rows.next()) {
                        assertNotEquals(java.sql.DatabaseMetaData.tableIndexStatistic,rows.getShort("TYPE"),"unexpected statistics row");
                        String name=rows.getString("INDEX_NAME");assertNotNull(name);String column=rows.getString("COLUMN_NAME");assertNotNull(column);
                        assertNull(indexes.computeIfAbsent(name,k->new TreeMap<>()).put(rows.getShort("ORDINAL_POSITION"),column));
                        if(rows.getBoolean("NON_UNIQUE"))actualOrdinaryNames.add(name);
                        Boolean previous=unique.put(name,!rows.getBoolean("NON_UNIQUE"));if(previous!=null)assertEquals(previous,unique.get(name));
                    }
                }
                for(var index:indexes.entrySet())actualIndexes.add(table+"|"+String.join(",",index.getValue().values())+"|"+unique.get(index.getKey()));
            }
            assertEquals(new TreeSet<>(FROZEN_COLUMNS),new TreeSet<>(actualColumns),"every loaded column type/length/precision/nullable");
            assertEquals(FROZEN_COLUMNS.size(),actualColumns.size(),"no duplicate metadata columns");
            var expectedIndexes=new ArrayList<>(FROZEN_INDEXES);Collections.sort(expectedIndexes);Collections.sort(actualIndexes);
            assertEquals(expectedIndexes,actualIndexes,"closed PK/unique/ordinary index multiset");
            assertEquals(Set.of("idx_independent_receipt_source_relation","idx_independent_receipt_source_snapshot","idx_independent_receipt_status","idx_mes_edhr_material_gate_receipt_batch","idx_mes_pp_completion_receipt_source_hash","idx_mes_pp_completion_receipt_work_order"),actualOrdinaryNames,"exact six ordinary index names");
        }
    }
    private static String pk(String table) {return table.equals("gxp_audit_ledger_sequence")?"tenant_id":"id";}
    private List<Map<String,Object>> snapshot(JdbcTemplate source,String table) {
        assertTrue(TABLES.contains(table));
        return source.query("SELECT * FROM "+table+" ORDER BY "+pk(table),(rs,index)->{
            var row=new LinkedHashMap<String,Object>();var metadata=rs.getMetaData();
            for(int i=1;i<=metadata.getColumnCount();i++) {
                Object value=rs.getObject(i);
                if(value instanceof java.sql.Clob clob)value=clob.getSubString(1,Math.toIntExact(clob.length()));
                row.put(metadata.getColumnLabel(i),value);
            }
            return row;
        });
    }
    private List<Long> ids(List<Map<String,Object>> rows,String table) {return rows.stream().map(row->((Number)row.get(pk(table))).longValue()).toList();}
    private Map<String,List<Map<String,Object>>> freshSnapshot() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        try(var connection=dataSource.getConnection()) {
            var fresh=new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
            var result=new LinkedHashMap<String,List<Map<String,Object>>>();
            for(String table:TABLES)result.put(table,snapshot(fresh,table));return result;
        } catch(java.sql.SQLException failure) {throw new IllegalStateException("TEST_ONLY_FRESH_SESSION_FAILED",failure);}
    }
    private void assertFreshEmpty() {
        for(var entry:freshSnapshot().entrySet())assertTrue(entry.getValue().isEmpty(),"post-commit global empty "+entry.getKey());
    }
    private void registerCommitOwnership(String table,Set<Long> actualIds) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
        assertFalse(actualIds.isEmpty());for(Long id:actualIds)assertTrue(id!=null&&id>0);
        var insertedRows=snapshot(jdbc,table).stream().filter(row->actualIds.contains(((Number)row.get(pk(table))).longValue())).toList();
        assertEquals(actualIds,new LinkedHashSet<>(ids(insertedRows,table)));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){for(Long id:actualIds)assertTrue(ownedIds.get(table).add(id),"duplicate insertion registration "+table+":"+id);ownedRows.get(table).addAll(insertedRows);}
        });
    }
    private void insertCheckedSeed(String sql) {
        var match=java.util.regex.Pattern.compile("INSERT INTO (\\w+)\\(([^)]+)\\) VALUES\\((.*)\\);?",java.util.regex.Pattern.DOTALL).matcher(sql.trim());
        assertTrue(match.matches(),"frozen seed statement syntax");String table=match.group(1);assertTrue(TABLES.contains(table));
        var columns=Arrays.asList(match.group(2).split(","));
        List<String> tokens=splitSeedValues(match.group(3).replaceFirst(";$",""));assertEquals(columns.size(),tokens.size());
        var frozenExpected=new LinkedHashMap<String,Object>();
        for(int i=0;i<columns.size();i++)frozenExpected.put(columns.get(i).trim(),seedValue(tokens.get(i).trim()));
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
        if(table.equals("gxp_audit_ledger_sequence"))assertEquals(1,jdbc.update(sql));
        else assertEquals(1,jdbc.update(connection->connection.prepareStatement(sql,new String[]{"id"}),holder));
        long id=table.equals("gxp_audit_ledger_sequence")?T:Objects.requireNonNull(holder.getKey()).longValue();assertTrue(id>0);
        var rows=jdbc.query("SELECT * FROM "+table+" WHERE "+pk(table)+"=?",(rs,index)->{
            var row=new LinkedHashMap<String,Object>();var md=rs.getMetaData();for(int i=1;i<=md.getColumnCount();i++){Object v=rs.getObject(i);if(v instanceof java.sql.Clob clob)v=clob.getSubString(1,Math.toIntExact(clob.length()));row.put(md.getColumnLabel(i),v);}return row;
        },id);
        assertEquals(1,rows.size());var actual=rows.get(0);assertEquals(id,((Number)actual.get(pk(table))).longValue());
        for(var expected:frozenExpected.entrySet())assertEquals(expected.getValue(),normalizeSeedValue(actual.get(expected.getKey())),"exact frozen seed field "+table+"."+expected.getKey());
        if(table.equals("gxp_audit_policy_operation")) {assertEquals("",actual.get("creator"));assertEquals("",actual.get("updater"));assertEquals(false,actual.get("deleted"));assertNotNull(actual.get("create_time"));assertNotNull(actual.get("update_time"));}
        if(table.equals("gxp_audit_ledger_sequence")){assertNotNull(actual.get("create_time"));assertNotNull(actual.get("update_time"));}
        seedRows.computeIfAbsent(table,k->new ArrayList<>()).add(actual);registerCommitOwnership(table,Set.of(id));
    }
    private static Object normalizeSeedValue(Object value){if(value instanceof Number number)return number.longValue();return value;}
    private static Object seedValue(String value) {
        if(value.equals("NULL"))return null;if(value.equals("TRUE"))return true;if(value.equals("FALSE"))return false;
        if(value.startsWith("TIMESTAMP '"))return java.sql.Timestamp.valueOf(value.substring(11,value.length()-1));
        if(value.startsWith("'")&&value.endsWith("'"))return value.substring(1,value.length()-1).replace("''","'");
        return Long.valueOf(value);
    }
    private static List<String> splitSeedValues(String raw) {
        var values=new ArrayList<String>();var value=new StringBuilder();boolean quoted=false;
        for(int i=0;i<raw.length();i++){char c=raw.charAt(i);if(c=='\''){if(quoted&&i+1<raw.length()&&raw.charAt(i+1)=='\''){value.append(c).append(c);i++;continue;}quoted=!quoted;}if(c==','&&!quoted){values.add(value.toString());value.setLength(0);}else value.append(c);}
        assertFalse(quoted);values.add(value.toString());return values;
    }
    private void verifyDurableSeeds(JdbcTemplate fresh) {
        for(String table:TABLES.subList(6,9)) {
            var expected=new ArrayList<>(seedRows.get(table));expected.sort(Comparator.comparingLong(row->((Number)row.get(pk(table))).longValue()));
            assertEquals(expected,snapshot(fresh,table),"committed seed all columns including generated ID/default metadata "+table);
            assertEquals(ownedIds.get(table),new LinkedHashSet<>(ids(snapshot(fresh,table),table)));
        }
        assertEquals(PH,sha(text(POLICY)));var activation=snapshot(fresh,"gxp_audit_policy_activation").get(0);
        assertEquals(PV,activation.get("policy_version"));assertEquals(RID,activation.get("request_id"));assertEquals(PH,activation.get("policy_hash"));
        assertEquals(AH,activation.get("activation_hash"));assertEquals(AH,sha((String)activation.get("canonical_activation_json")));
    }
    private void assertGlobalOwnership(JdbcTemplate source) {
        for(String table:TABLES) {
            var rows=snapshot(source,table);var actualIds=ids(rows,table);assertEquals(actualIds.size(),new HashSet<>(actualIds).size());
            if(!table.equals("gxp_audit_ledger_sequence")){var inserted=new ArrayList<>(ownedRows.get(table));inserted.sort(Comparator.comparingLong(row->((Number)row.get("id")).longValue()));assertEquals(inserted,rows,"exact committed insertion ownership full typed fields "+table);}
            assertEquals(ownedIds.get(table),new LinkedHashSet<>(actualIds),"no foreign or unregistered actual IDs "+table);
            for(var row:rows) {
                assertEquals(T,((Number)row.get("tenant_id")).longValue(),"no foreign tenant "+table);
                if(row.containsKey("policy_version"))assertEquals(PV,row.get("policy_version"),"no foreign policy "+table);
                if(table.equals("gxp_audit_policy_activation"))assertEquals(RID,row.get("request_id"));
                if(table.equals("mes_pro_edhr_material_gate_receipt"))assertTrue(((String)row.get("receipt_id")).startsWith("TEST_ONLY_R3_"));
            }
        }
        // Policy/activation rows are immutable seed ownership, not merely matching tenant/PV.
        for(String table:List.of("gxp_audit_policy_operation","gxp_audit_policy_activation")) {
            var expected=new ArrayList<>(seedRows.get(table));expected.sort(Comparator.comparingLong(row->((Number)row.get("id")).longValue()));assertEquals(expected,snapshot(source,table));
        }
        var eventIds=ownedIds.get("gxp_audit_event");
        for(var relation:snapshot(source,"gxp_audit_event_relation"))assertTrue(eventIds.contains(((Number)relation.get("event_id")).longValue()));
        for(var binding:snapshot(source,"mes_edhr_source_creation_binding")){assertTrue(eventIds.contains(((Number)binding.get("creation_event_id")).longValue()));assertTrue(ownedIds.get("mes_pro_edhr_material_gate_receipt").contains(((Number)binding.get("source_row_id")).longValue()));}
    }
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.ImportAutoConfiguration({YudaoDataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationTestConfiguration.class, YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class, MybatisPlusJoinAutoConfiguration.class,
            JacksonAutoConfiguration.class, YudaoJacksonAutoConfiguration.class})
    @Import({SpringUtil.class, GxpAuditServiceImpl.class,
            GxpCreationAuditVerificationService.class, MesEdhrSourceCreationProtocolRegistry.class, MesEdhrSourceOwnerIntegrity.class,
            MesEdhrSourceCreationBindingStore.class, MesEdhrSourceCreationVerificationService.class})
    static class Application {
        @Bean cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper managedAssociations() {
            return org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper.class);
        }
        @Bean cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationMapper submissionAssociations() {
            return org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationMapper.class);
        }
        @Bean DataSource dataSource() {
            var source = new DriverManagerDataSource(URL, "sa", ""); source.setDriverClassName("org.h2.Driver"); return source;
        }
        private static <T> MapperFactoryBean<T> mapper(Class<T> type, SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<T>(type); bean.setSqlSessionFactory(factory); return bean;
        }
        @Bean MapperFactoryBean<GxpAuditEventMapper> gxpAuditEventMapper(SqlSessionFactory f) { return mapper(GxpAuditEventMapper.class, f); }
        @Bean MapperFactoryBean<GxpAuditLedgerSequenceMapper> gxpAuditLedgerSequenceMapper(SqlSessionFactory f) { return mapper(GxpAuditLedgerSequenceMapper.class, f); }
        @Bean MapperFactoryBean<GxpAuditPolicyOperationMapper> gxpAuditPolicyOperationMapper(SqlSessionFactory f) { return mapper(GxpAuditPolicyOperationMapper.class, f); }
        @Bean MapperFactoryBean<GxpAuditPolicyActivationMapper> gxpAuditPolicyActivationMapper(SqlSessionFactory f) { return mapper(GxpAuditPolicyActivationMapper.class, f); }
        @Bean MapperFactoryBean<GxpAuditEventRelationMapper> gxpAuditEventRelationMapper(SqlSessionFactory f) { return mapper(GxpAuditEventRelationMapper.class, f); }
        @Bean MapperFactoryBean<MesIndependentBatchPrerequisiteReceiptMapper> mesIndependentBatchPrerequisiteReceiptMapper(SqlSessionFactory f) { return mapper(MesIndependentBatchPrerequisiteReceiptMapper.class, f); }
        @Bean MapperFactoryBean<MesProEdhrMaterialGateReceiptMapper> mesProEdhrMaterialGateReceiptMapper(SqlSessionFactory f) { return mapper(MesProEdhrMaterialGateReceiptMapper.class, f); }
        @Bean MapperFactoryBean<MesProcessPoolActiveOrderCompletionReceiptMapper> mesProcessPoolActiveOrderCompletionReceiptMapper(SqlSessionFactory f) { return mapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class, f); }
        @Bean MapperFactoryBean<MesEdhrSourceCreationBindingMapper> mesEdhrSourceCreationBindingMapper(SqlSessionFactory f) { return mapper(MesEdhrSourceCreationBindingMapper.class, f); }
    }
}
