package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.dept.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real unified + DCC producer, actual Mapper transactions/TIMESTAMP6 and both real verifiers.
 * Account reauthentication/image/file-byte directory and audit delivery are explicit external ports. */
class DccDelayedSignatureRoundtripTest {
    private JdbcTemplate jdbc;private TransactionTemplate tx;
    private DccControlledFileMapper files;private DccControlledFileSignatureMapper dccSignatures;private ElectronicSignatureRecordMapper unified;
    private DccSignatureVerificationServiceImpl producer;private DccElectronicSignatureManagementServiceImpl dccVerifier;private ElectronicSignatureQueryServiceImpl unifiedVerifier;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(UserTypeEnum.ADMIN.getValue()),new org.springframework.mock.web.MockHttpServletRequest());
        var ds=new DriverManagerDataSource("jdbc:h2:mem:g01_roundtrip_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);table(DccControlledFileDO.class,"dcc_controlled_file");table(DccControlledFileSignatureDO.class,"dcc_controlled_file_signature");table(ElectronicSignatureRecordDO.class,"system_electronic_signature");
        jdbc.execute("ALTER TABLE system_electronic_signature ALTER COLUMN subject_id VARCHAR(2048)");jdbc.execute("ALTER TABLE system_electronic_signature ALTER COLUMN reason VARCHAR(500)");
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);factory.setConfiguration(config);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));var session=factory.getObject();
        for(var mapper:List.of(DccControlledFileMapper.class,DccControlledFileSignatureMapper.class,ElectronicSignatureRecordMapper.class))session.getConfiguration().addMapper(mapper);
        var template=new SqlSessionTemplate(session);files=template.getMapper(DccControlledFileMapper.class);dccSignatures=template.getMapper(DccControlledFileSignatureMapper.class);unified=template.getMapper(ElectronicSignatureRecordMapper.class);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));files.insert(DccControlledFileDO.builder().id(20L).tenantId(1L).requesterId(99L).versionNo("B/1").fileNumber("SOP-01")
                .sourceFileId(110L).processInstanceId("R").effectiveMode("DELAYED").build());
        var properties=new DccSignatureEvidenceProperties();properties.setKeyVersion("G01-unit");properties.setHmacSecret("task-owned-isolated-hmac-key");
        var bytePort=mock(FileService.class);when(bytePort.getFile(110L)).thenReturn(FileDO.builder().id(110L).configId(7L).path("g01-source.pdf").build());when(bytePort.getFileContent(7L,"g01-source.pdf")).thenReturn(new byte[]{1,2,3});
        var evidence=new DccControlledFileSignatureEvidenceServiceImpl();wire(evidence,"controlledFileMapper",files,"fileService",bytePort,"signatureEvidenceProperties",properties);
        var images=mock(DccElectronicSignatureImageService.class);var image=DccElectronicSignatureImageSnapshot.builder().imageId(501L).versionNo(1).fileId(1501L).fileUrl("g01-sign.png")
                .sha256("image-sha").contentType("image/png").fileSize(50L).imageStatus("ACTIVE").verifiedStatus("VALID").content(new byte[]{7}).build();when(images.requireActiveSnapshot(99L)).thenReturn(image);when(images.verifySignatureSnapshot(any(DccControlledFileSignatureDO.class))).thenReturn(image);
        var adapter=new DccControlledFileSignatureSubjectAdapter();wire(adapter,"signatureEvidenceService",evidence);
        var signatureService=new ElectronicSignatureServiceImpl();var accounts=mock(AdminUserApi.class);var ledger=mock(GxpAuditService.class);when(ledger.append(any())).thenReturn(new GxpAuditAppendResult(900L,1L,"a".repeat(64),false));
        wire(signatureService,"signatureRecordMapper",unified,"adminUserApi",accounts,"gxpAuditService",ledger,"subjectAdapters",List.of(adapter));
        producer=new DccSignatureVerificationServiceImpl();var users=mock(AdminUserService.class);when(users.getUser(99L)).thenReturn(new AdminUserDO().setId(99L).setUsername("compiler").setNickname("真实编制人").setDeptId(7L).setPostIds(Set.of(9L)));
        var depts=mock(DeptService.class);when(depts.getDept(7L)).thenReturn(new DeptDO().setId(7L).setName("研发"));var posts=mock(PostService.class);when(posts.getPostList(Set.of(9L))).thenReturn(List.of(new PostDO().setId(9L).setName("普通编制岗位")));
        var permissions=mock(PermissionService.class);when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(10L));var roles=mock(RoleService.class);when(roles.getRoleList(Set.of(10L))).thenReturn(List.of(new RoleDO().setId(10L).setName("普通上传用户")));
        wire(producer,"adminUserService",users,"deptService",depts,"postService",posts,"permissionService",permissions,"roleService",roles,
                "electronicSignatureAuthorizationService",mock(DccElectronicSignatureAuthorizationService.class),"signatureEvidenceService",evidence,"signatureImageService",images,
                "signatureMapper",dccSignatures,"electronicSignatureService",signatureService,"unifiedRecords",unified);
        dccVerifier=new DccElectronicSignatureManagementServiceImpl();wire(dccVerifier,"signatureMapper",dccSignatures,"controlledFileMapper",files,"signatureEvidenceProperties",properties,"fileService",bytePort,"signatureImageService",images,
                "signatureBindingService",mock(DccControlledFileSignatureBindingService.class));
        unifiedVerifier=new ElectronicSignatureQueryServiceImpl();wire(unifiedVerifier,"signatureRecordMapper",unified);
    }
    @AfterEach void clear(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @Test void unifiedDccAndFileReadbackPreserveSameActualMicrosecondSAndBothVerify(){
        var signed=tx.execute(status->producer.verifyPasswordAndCreateDelayedConfirmationSignature(99L,20L,"T","R",2,LocalDateTime.of(2099,1,1,12,0),"credential","真实衔接关系确认"));
        var u=unified.selectList().get(0);var d=dccSignatures.selectById(signed.getSignatureId());assertEquals(0,u.getSignedAt().getNano()%1000);assertEquals(u.getSignedAt(),signed.getSignedAt());assertEquals(u.getSignedAt(),d.getSignedAt());
        jdbc.update("UPDATE dcc_controlled_file SET delayed_effective_time=?,delayed_effective_confirmed_at=?,delayed_effective_signature_id=?,delayed_effective_process_instance_id='R',controlled_time=? WHERE id=20",d.getSelectedEffectiveTime(),d.getSignedAt(),d.getId(),d.getSignedAt());
        var file=files.selectById(20L);assertEquals(u.getSignedAt(),file.getDelayedEffectiveConfirmedAt());assertEquals(u.getSignedAt(),file.getControlledTime());
        assertEquals("VALID",unifiedVerifier.verifyEvidence(u.getId()).verificationStatus());assertEquals("VALID",dccVerifier.verifySignatureEvidence(d.getId()).getVerificationStatus());
        jdbc.update("UPDATE dcc_controlled_file_signature SET selected_effective_time=? WHERE id=?",d.getSelectedEffectiveTime().plusSeconds(1),d.getId());assertEquals("INVALID",dccVerifier.verifySignatureEvidence(d.getId()).getVerificationStatus());
        jdbc.update("UPDATE system_electronic_signature SET signed_at=? WHERE id=?",u.getSignedAt().plusNanos(1000),u.getId());assertEquals("MISMATCH",unifiedVerifier.verifyEvidence(u.getId()).verificationStatus());
    }
    @Test void lateFailureRollsBackRealUnifiedAndDccSignaturesAndRetrySucceeds(){
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{producer.verifyPasswordAndCreateDelayedConfirmationSignature(99L,20L,"T","R",2,LocalDateTime.of(2099,1,1,12,0),"credential","真实衔接确认");throw new IllegalStateException("task-owned late workflow failure");}));
        assertEquals(0,unified.selectList().size());assertEquals(0,dccSignatures.selectList().size());
        assertNotNull(tx.execute(s->producer.verifyPasswordAndCreateDelayedConfirmationSignature(99L,20L,"T","R",2,LocalDateTime.of(2099,1,1,12,0),"credential","真实衔接确认")));
    }
    private void table(Class<?> type,String name){var columns=new LinkedHashMap<String,String>();for(Class<?> c=type;c!=null;c=c.getSuperclass())for(var field:c.getDeclaredFields()){
        if(Modifier.isStatic(field.getModifiers()) || java.util.Collection.class.isAssignableFrom(field.getType()))continue;
        String column=field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase();String sql=field.getType()==LocalDateTime.class?"TIMESTAMP(6)":field.getType()==LocalDate.class?"DATE":field.getType()==Long.class?"BIGINT":field.getType()==Integer.class?"INT":field.getType()==Boolean.class?"BOOLEAN":"VARCHAR(20000)";
        columns.putIfAbsent(column,column.equals("id")?"BIGINT AUTO_INCREMENT PRIMARY KEY":column.equals("deleted")?"BOOLEAN DEFAULT FALSE":sql);
    }jdbc.execute("CREATE TABLE "+name+"("+columns.entrySet().stream().map(e->e.getKey()+" "+e.getValue()).collect(java.util.stream.Collectors.joining(","))+")");}
    private void wire(Object bean,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
}
