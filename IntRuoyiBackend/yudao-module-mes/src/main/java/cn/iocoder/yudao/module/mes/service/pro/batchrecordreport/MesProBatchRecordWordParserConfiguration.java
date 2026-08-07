package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.wordparser.DefaultSharedWordDocumentParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MesProBatchRecordWordParserConfiguration {

    @Bean
    public MesProBatchRecordDocParser mesProBatchRecordDocParser() {
        return new MesProBatchRecordDocParser(new DefaultSharedWordDocumentParser());
    }

    @Bean
    public MesProBatchRecordRouteCRecognizer mesProBatchRecordRouteCRecognizer() {
        return new MesProBatchRecordRouteCRecognizer(new DefaultSharedWordDocumentParser());
    }
}
