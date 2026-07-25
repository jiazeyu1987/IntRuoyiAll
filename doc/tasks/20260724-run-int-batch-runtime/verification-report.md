# Verification Report

## Result

pass

## Evidence

- 当前 profile：`int_batch`。
- 前端：`http://127.0.0.1:8041/`，监听 PID `30620`（`node.exe`），HTTP `200`。
- 后端：`http://127.0.0.1:48041/actuator/health`，监听 PID `25760`（Java），响应 `{"status":"UP"}`。
- MySQL：沿用 `E:\IntRuoyi` 的已验证本机连接方式，使用 Docker 端口 `127.0.0.1:23306`、数据库 `ruoyi-vue-pro`；未使用会拒绝认证的本机 `127.0.0.1:3306`。
- Redis：使用 Docker 端口 `127.0.0.1:26379`。
- 后端启动日志：`.runtime\20260724-run-int-batch-runtime\backend-23306.out.log` 已记录 `项目启动成功！`。
- 经验沉淀：`docs/local-runtime.md#2026-07-25-本机-docker-mysql-连接门禁` 记录了后续后端启动必须优先使用 Docker MySQL `23306` 和 Redis `26379` 的门禁，`docs/experience-index.md` 已增加关键词路由。

## Impact

前后端均已运行并完成本机可用性验证。服务按用户请求保持运行。
