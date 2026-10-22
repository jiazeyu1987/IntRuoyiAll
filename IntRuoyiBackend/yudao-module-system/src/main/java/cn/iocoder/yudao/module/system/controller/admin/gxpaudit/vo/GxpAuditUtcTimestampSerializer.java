package cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Serializes ledger timestamps whose persisted LocalDateTime contract is UTC. */
public final class GxpAuditUtcTimestampSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeNumber(value.toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
