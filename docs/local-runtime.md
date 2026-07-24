# IntRuoyi Local Runtime Rules

## 触发场景

- 启动、停止、重启或排查本机前端、后端、Vite、Java 服务时，必须先读取本文件。
- 排查 `8081`、`48081` 或其他 worktree 登记端口占用时，必须同时读取 `docs/worktree-restrictions.md`。
- 本文件只约束本机运行态；远端服务器必须读取 `docs/server-access.md`。

## 固定端口

- `int_main` 前端专属端口：`8081`。
- `int_main` 后端专属端口：`48081`。
- 前端本机入口：`http://127.0.0.1:8081` 或 `http://localhost:8081`。
- 后端健康检查：`http://127.0.0.1:48081/actuator/health`。
- 前端本机模式应使用 `IntRuoyiFronted\.env.local`：
  - `VITE_PORT=8081`
  - `VITE_BASE_URL=http://127.0.0.1:48081`
  - `VITE_PROXY_TARGET=http://127.0.0.1:48081`

## 启动前检查

- 启动 `int_main` 前端前，检查 `8081` 占用。
- 启动 `int_main` 后端前，检查 `48081` 占用。
- 如果端口被当前 `int_main` 旧进程占用，可记录进程 ID、命令行和归属依据后停止对应旧进程，再启动。
- 如果端口被未知进程、非 IntRuoyi 进程或非 `int_main` worktree 占用，必须 fail fast，不得强杀或换端口。
- 非 `int_main` worktree 不得使用 `8081/48081`，必须按 `docs/worktree-restrictions.md` 的 slot 规则使用独立端口。

## 禁止做法

- 禁止把 `int_main` 改到随机端口启动。
- 禁止非 `int_main` 使用 `8081/48081`。
- 禁止端口占用时静默换端口、静默跳过服务或宣称启动成功。
- 禁止停止无法确认归属的进程。

## 验证方式

- 记录端口监听检查结果。
- 记录启动命令、工作目录、端口和进程 ID。
- 前端启动后验证 `http://127.0.0.1:8081/`。
- 后端启动后验证 `http://127.0.0.1:48081/actuator/health`。
