# 正式展厅 release install failed 排障修复

## 任务目标

- 定位正式站点提示 `SHOWROOM_RELEASE_INSTALL_FAILED` 的根因。
- 修复导致浏览器端仍运行旧 release 的安装失败问题。
- 验证正式站点可安装并运行目标 release `20260707T203144Z-be276b74dfa8-70f3eea512e2`。

## 里程碑

- [x] M1 建立任务记录并完成只读复现。
- [ ] M2 定位失败资源或安装步骤。
- [ ] M3 按根因修复并完成验证。
- [ ] M4 更新任务文档并按验证结果提交。

## 经验门禁

- 正式服排障：已读取 `docs/server-access.md`，当前先做 HTTP/日志/静态资源只读排障。
- 发布/回滚边界：已读取 `docs/release-backup-restore.md`，未定位根因前不执行正式服写入、重启或回滚。
- PowerShell 编码：已读取 `docs/powershell-memory.md`，命令输出与任务文档按 UTF-8 处理。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，先定位具体安装失败点。
- 是否存在临时补丁或绕过：否。

## 预期验证

- 可复现或解释浏览器端 `SHOWROOM_RELEASE_INSTALL_FAILED`。
- 目标 release manifest、documents、assets 均可访问或明确缺失项。
- 修复后正式站点真实访问不再提示仍运行旧 release。

## Current Status

- in_progress


## 经验门禁补充

- 已读取 `docs/experience-index.md`，本任务命中正式服排障、发布验证、PowerShell 编码、真实浏览器验证门禁。
- 已记录本轮用户需求与关键命令到 `docs/request-command-log.md`。
