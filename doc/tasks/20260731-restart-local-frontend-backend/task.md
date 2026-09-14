# 20260731 Restart Local Frontend Backend

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端运行态。

## Milestones

- [x] 读取并记录本地运行、任务收尾与适用经验门禁
- [x] 检查 `8081` / `48081` 端口当前占用与进程归属
- [x] 安全停止已确认归属的旧后端进程
- [x] 启动后端 `48081` 与前端 `8081`
- [x] 验证后端 health 与前端 HTTP 可访问

## Expected Verification

- 后端健康检查 `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`
- 前端入口 `http://127.0.0.1:8081/` 返回 HTTP 200
- 记录新旧进程 PID、命令行归属、启动命令与阻塞项

## Applicable Gates

- `docs/local-runtime.md`: `int_main` 固定使用前端 `8081`、后端 `48081`；端口被当前 `int_main` 旧进程占用时可记录归属后停止，未知进程必须 fail fast。
- `docs/local-runtime.md`: 后端重启必须验证 health `UP`，前端重启必须验证入口 HTTP 200，不得用单端成功冒充前后端完成。
- `docs/local-runtime.md`: 禁止随机换端口、强杀未知进程、静默跳过服务或声明未验证的启动成功。

## Current Status

ready_for_closeout

## Completion Evidence

- `2026-07-31` 再次收到“启动前后端”请求后恢复本任务。
- `2026-07-31` 当前再次收到“启动前后端”请求时，`8081` 与 `48081` 均未监听，恢复启动与验证里程碑。
- 后端 `48081` 由当前主工作区稳定运行 Jar 监听，health 返回 `UP`。
- 前端通过标准本地重启脚本启动在 `8081`，入口与 `/@vite/client` 均返回 HTTP 200。
- Vite 冷启动依赖预构建受共享磁盘高 I/O 影响耗时较长，预构建完成后标准前端进程复启成功，重复验证响应稳定。
- 本次标准 full 启动首次因 `int-ruoyi-mysql` 未运行而 fail fast；恢复既有 `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 容器后，标准 full 启动成功。
- 当前后端 PID `37212` 监听 `48081` 且 health 为 `UP`；前端 PID `14800` 监听 `8081` 且入口返回 HTTP 200。

## Closeout Blocker

- `int_main` 存在多个无关并行任务的源码、测试和任务文档改动，且本任务执行期间 HEAD 出现新的并行提交。
- 按任务所有权与共享分支并发门禁，不能把这些改动作为本任务基线提交，也不能在无法区分并行任务边界时执行宽泛暂存、提交和推送。
- 启动目标与运行态验证已完成；仅 Git 提交/推送收尾保持阻塞，因此状态保留为 `ready_for_closeout`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，本任务仅执行标准本地运行态重启，不变更代码或配置
- `是否存在临时补丁或绕过`：否
