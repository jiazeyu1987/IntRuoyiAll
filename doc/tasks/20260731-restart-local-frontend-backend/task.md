# 20260731 Restart Local Frontend Backend

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端运行态。

## Milestones

- [x] 读取并记录本地运行、任务收尾与适用经验门禁
- [x] 检查 `8081` / `48081` 端口当前占用与进程归属
- [x] 安全停止已确认归属的旧后端进程
- [ ] 启动后端 `48081` 与前端 `8081`
- [ ] 验证后端 health 与前端 HTTP 可访问

## Expected Verification

- 后端健康检查 `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`
- 前端入口 `http://127.0.0.1:8081/` 返回 HTTP 200
- 记录新旧进程 PID、命令行归属、启动命令与阻塞项

## Applicable Gates

- `docs/local-runtime.md`: `int_main` 固定使用前端 `8081`、后端 `48081`；端口被当前 `int_main` 旧进程占用时可记录归属后停止，未知进程必须 fail fast。
- `docs/local-runtime.md`: 后端重启必须验证 health `UP`，前端重启必须验证入口 HTTP 200，不得用单端成功冒充前后端完成。
- `docs/local-runtime.md`: 禁止随机换端口、强杀未知进程、静默跳过服务或声明未验证的启动成功。

## Current Status

blocked

## Blocker

- 标准 full 重启脚本连续两次阻塞在后端 Maven 打包阶段：`mvn -pl yudao-server -am -DskipTests package` 均停在 `yudao-module-infra` 的 javac class 写入阶段。
- `jcmd Thread.print` 显示 Maven JVM 主线程处于 `RUNNABLE`，调用栈停在 `sun.nio.ch.FileDispatcherImpl.write0` / `com.sun.tools.javac.jvm.ClassWriter.writeClass`，重复指向 Windows 文件写入阻塞。
- 已停止本任务启动的卡住 Maven / 重启脚本进程，避免继续占用构建目录。
- 当前 `48081` 未监听；`8081` 仍为重启前旧前端进程，full 脚本未执行到前端重启阶段。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，本任务仅执行标准本地运行态重启，不变更代码或配置
- `是否存在临时补丁或绕过`：否
