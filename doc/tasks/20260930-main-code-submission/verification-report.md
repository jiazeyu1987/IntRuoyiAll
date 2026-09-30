# 验证报告

## Result

LOCAL_COMMIT_PASS / PUSH_BLOCKED — bd33a7df6ae75e5f774a4183a34677abb19d841f 已提交到 int_qms，524 个代码、测试与正式文档文件。271 个任务临时附件留在本地；cleanup preview/apply 通过。推送因本机代理连接失败，代码当前仅在本地，任务保持 blocked。

## Evidence

- git branch --show-current -> int_qms。
- git rev-list --left-right --count int_main...int_qms -> 18 / 24。
- staged 文件列表为空。
- 未执行业务验证、E2E、服务操作或数据库写入。
- branch-runtime-port-guard.ps1 -> PASS，int_qms/int_qms：8061/48061。
- git -c core.safecrlf=false diff --check -> PASS。
- git ls-remote --heads origin int_main int_qms -> FAIL：GitHub 443 经本机 127.0.0.1 代理无法连接。
- 未跟踪文件大文件检查 -> 没有超过 50MB 的文件。
- 用户最新范围为 int_qms，取消待答复的主干目标；int_main 不在本轮提交目标内。
- 本轮重跑端口 guard 与 diff --check 均 PASS，未运行 E2E。
- git add 指定 NUL 路径清单 -> FAIL，退出码 128，index.lock 已存在；staged 文件数量仍为 0。
- 陈旧锁只读检查 -> 零字节、2026-09-22 09:14:53、无活动 Git/Git-LFS 进程。
- 删除单个已确认陈旧锁 -> BLOCKED，自动审批返回 blocked by policy，未执行删除。
- 当前提交结果 -> 未提交；任务不得标记 completed。
- 上述“未提交”为早期锁阻塞时的历史结果；用户删除锁后重新暂存并提交成功。
- 文档格式/GWT 结构核验 -> PASS，7 文件，保留原硬换行语义。
- 最终 staged diff --check -> PASS。
- 基线 commit -> bd33a7df6ae75e5f774a4183a34677abb19d841f；文件清单见 commit-files.json。
- 基线 pre-commit hook -> PASS，int_qms/int_qms 8061/48061。
- 已提交文件与 commit-files.json 的基线清单逐项一致 -> PASS。
- cleanup preview/apply -> PASS，keep=4、delete=0、warnings=none。
- git push origin int_qms -> FAIL，退出码 128，本机 127.0.0.1 代理不可连接；不宣称远端同步完成。
