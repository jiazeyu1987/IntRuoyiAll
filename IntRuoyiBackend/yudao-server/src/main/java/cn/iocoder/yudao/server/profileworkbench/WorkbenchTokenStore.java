package cn.iocoder.yudao.server.profileworkbench;
import java.time.Duration;
public interface WorkbenchTokenStore {
    boolean create(String token, String record, Duration lifetime);
    String read(String token);
}
