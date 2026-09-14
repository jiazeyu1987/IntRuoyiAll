# Verification Report

## Current Result

PASS

`E:\IntRuoyi` 的 `int_main` 前端和后端已完成标准本地重启，固定端口保持为前端 `8081`、后端 `48081`。

## Runtime Evidence

- 后端 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-082313.jar`。
- 后端 Jar SHA256：`A44E10178B4C2F5A428E153E3399F81BA800970BA154990D16263CFBFDB1B9F6`。
- 后端：PID `38500`，端口 `48081`，`/actuator/health` 返回 `UP`。
- 前端：PID `51364`，端口 `8081`，首页返回 HTTP `200`。
- `show-int-ruoyi-local-status.ps1 -Component full -Json` 返回 `status=running`。

## Verification Commands

- `mvn -pl yudao-server -am -DskipTests package` -> PASS。
- `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py -q` -> PASS，15 passed。
- `python -X utf8 -m pytest script\tests\test_restart_int_ruoyi_local_schema.py script\tests\test_restart_ruoyi_frontend_vite_emfile_config.py -q` -> PASS，21 passed。
- `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-http-client-static.spec.js` -> PASS。
- 无 Runner token 的受控注册和心跳探针 -> PASS，业务码 `0`；运行计数 `0`；会话清理影响 `1` 行。

## Residual Risk

后端启动后，DCC 临时上传文件清理定时任务记录了一条“文件不存在”异常。该问题未影响前端 HTTP `200`、后端 health `UP` 或端口监听；本任务未扩大范围修复该历史运行态数据问题。

## Closeout Evidence

- `task_closeout.py --mode preview` -> PASS：默认保留三份任务记录，delete、blocked、warnings 均为空。
- `task_closeout.py --mode apply` -> PASS：未删除任务文件；当前工作区不是 linked worktree，无需 merge 或 worktree removal。

## Ownership Boundary

并发任务在本次操作期间修改了其他任务记录并使当前分支领先远端。本任务未暂存、提交或推送任何并发任务文件。
