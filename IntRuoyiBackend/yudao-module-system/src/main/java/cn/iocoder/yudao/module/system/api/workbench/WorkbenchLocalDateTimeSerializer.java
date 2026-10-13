package cn.iocoder.yudao.module.system.api.workbench;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Workbench HTTP dates retain local wall time and microseconds independently of global epoch serialization. */
public final class WorkbenchLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
    public WorkbenchLocalDateTimeSerializer() { super(LocalDateTime.class); }
    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value));
    }
}
