# Verification Report

## 最终结论（覆盖下面首次检查结果）

BLOCKED：后端UP、代码已提交；前端初次HTTP200之后，最终20秒请求及延长60秒复查均超时。8081仍由PID49104监听，但不能认定前端可用。下文HTTP200仅为初次检查历史证据。


- 用户授权的前后端基线提交：094d183e3，共147文件（后端76，前端71），临时测试目录未提交。
- staged diff --check：PASS；提交钩子 branch runtime port guard：PASS。
- 重启静态测试：python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py script/tests/test_restart_int_ruoyi_local_schema.py -q，43 passed。
- 后端预构建：mvn.cmd -pl yudao-server -am '-DskipTests' package，31模块 BUILD SUCCESS，03:01。
- 标准启动函数内完整构建：BUILD SUCCESS，24.978秒；保留测试编译、未执行完整单元测试或 E2E。
- 启动 schema 只读探针、MySQL/Redis 路由和 MinIO依赖检查 PASS；任务未执行迁移写入。
- 前端 http://127.0.0.1:8081/ HTTP 200；PID 49104，2026-09-22 12:58:15创建，当前前端路径且 strictPort。
- 后端 http://127.0.0.1:48081/actuator/health status UP；PID 61216，2026-09-22 12:58:12创建，归属当前仓库。
- 当前启动日志 12:59:43：Started YudaoServerApplication in 89.982 seconds。
- 运行包 output/runtime/int_main/backend-runtime-control-20260922-125805.jar；修改时间早于进程创建时间。
- 运行包与构建包 SHA256相同：171EC3D310D5DC9D7F021DE9542CBC75DEBD4B7CABF81C4E99C047119170286C。
- 并行8083前端仍为PID44944（12:39:17创建），未被本任务停止。
- 首次停止旧后端后短暂端口释放延迟导致严格检查阻塞；增加仅针对已确认停止 PID 的有界释放等待后恢复启动。
- 未获本轮推送授权，未执行 git push。根 AGENTS 明确授权要求优先于 docs/task-closeout-rules 的自动推送要求；远程同步门禁不宣称完成。
- restart-retry.log 被启动进程继承句柄占用，作为重启构建证据显式保留，不停止健康服务以删除日志；实际服务stdout/stderr位于稳定output/runtime目录。
- Cleanup preview/apply：PASS，5个本任务临时附件已清理；运行目标完成，项目远程同步门禁未完成。
