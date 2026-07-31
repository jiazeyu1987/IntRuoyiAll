# 验证报告

## 验证结论

`int_main` 本地后端已正常运行，无需重复启动。

## 运行证据

- 工作区：`E:\IntRuoyi`
- 后端目录：`E:\IntRuoyi\IntRuoyiBackend`
- 监听端口：`48081`
- 监听 PID：`46388`
- 启动时间：`2026-07-27 20:33:12`
- 启动 Jar：`E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`
- 运行参数：`--server.port=48081 --spring.profiles.active=local`

## 健康检查

- 请求：`http://127.0.0.1:48081/actuator/health`
- 结果：`status=UP`

## 约束核对

- 未停止未知进程。
- 未切换端口。
- 未修改数据源或运行配置。
- 未引入 fallback、降级或临时绕过。

