package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.mockito.Mockito.*;

/** Real detail assembler and production SQL; unrelated materials/PQC/directory services are doubles. */
public final class MesSignatureDetailFixture {
    private MesSignatureDetailFixture() {}

    public static MesTeamLeaderActiveOrderDetailServiceImpl detail(JdbcTemplate jdbc,
            MesProcessPoolActiveOrderMapper orders, MesProProcessPoolEventMapper events,
            ElectronicSignatureQueryService signatures, MesProductionSubmissionReadBinding binding,
            MesProcessPoolActiveOrderReleaseApplicationMapper applications,
            MesProEdhrReleaseTransactionMapper transactions, MesProEdhrOperationAuditEventMapper audits) throws Exception {
        installJsonFunctions(jdbc);
        var config = MesSa06IdentityMapperTest.config("MesProcessPoolActiveOrderDetailReadMapper.xml", "");
        var readMapper = mock(MesProcessPoolActiveOrderDetailReadMapper.class);
        when(readMapper.selectByActiveOrderId(anyLong())).thenAnswer(inv -> jdbc.query(
                config.getMappedStatement(MesProcessPoolActiveOrderDetailReadMapper.class.getName()+".selectByActiveOrderId")
                        .getBoundSql(Map.of("activeOrderId", inv.getArgument(0))).getSql(),
                new BeanPropertyRowMapper<>(MesTeamLeaderActiveOrderDetailReadDO.class), (Long) inv.getArgument(0)));
        var constructor = MesTeamLeaderActiveOrderDetailServiceImpl.class.getConstructors()[0];
        var types = constructor.getParameterTypes();
        var arguments = new Object[types.length];
        for (int i=0; i<types.length; i++) {
            Class<?> type = types[i];
            arguments[i] = type == MesProcessPoolActiveOrderMapper.class ? orders
                    : type == MesProcessPoolActiveOrderDetailReadMapper.class ? readMapper
                    : type == MesProProcessPoolEventMapper.class ? events
                    : type == ElectronicSignatureQueryService.class ? signatures
                    : type == MesSubmissionSignatureIdentityReader.class ? new MesSubmissionSignatureIdentityReader(signatures, events, null, binding)
                    : type == MesProcessPoolActiveOrderReleaseApplicationMapper.class ? applications
                    : type == MesProEdhrReleaseTransactionMapper.class ? transactions
                    : type == MesProEdhrOperationAuditEventMapper.class ? audits : mock(type);
        }
        return (MesTeamLeaderActiveOrderDetailServiceImpl) constructor.newInstance(arguments);
    }

    public static void installJsonFunctions(JdbcTemplate jdbc) {
        String aliases = cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelinePqcGroupSqlTest.class.getName();
        jdbc.execute("CREATE ALIAS IF NOT EXISTS JSON_VALID FOR '"+aliases+".jsonValid'");
        jdbc.execute("CREATE ALIAS IF NOT EXISTS JSON_EXTRACT FOR '"+aliases+".jsonExtract'");
        jdbc.execute("CREATE ALIAS IF NOT EXISTS JSON_UNQUOTE FOR '"+aliases+".jsonUnquote'");
    }
}
