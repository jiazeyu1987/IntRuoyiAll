# Task: DCC 产品名称识别全链路稳定性审计与修复

## 任务目标

在已修复 `status` 截断问题后，继续基于测试服真实运行态审计 DCC 产品名称识别链路，确认新版本后是否仍存在其它识别错误；如发现真实新增问题，按 BDD + TDD 定位根因、修复、发布测试服并重新审计，直到测试服新版本后无新增识别异常。

## 里程碑

- [x] M1：建立任务记录并读取命中经验门禁。
- [x] M2：只读审计测试服当前版本、容器启动时间、识别记录、失败消息和后端日志。
- [x] M3：归类新版本启动后的真实识别问题，区分历史遗留、业务预期失败和系统异常。
- [x] M4：如存在系统异常或非预期识别失败，补 RED 测试并修复根因。
- [x] M5：运行 targeted 回归、构建发布测试服，并以后端新启动时间为基准复审。
- [x] M6：closeout 预览、提交任务文档和直接相关代码变更。

## 预期验证

- 测试服当前后端健康检查通过，运行版本与审计基准明确。
- 新版本后端启动时间之后，DCC 识别链路无新增系统异常、数据库截断、未处理异常栈或非预期失败类型。
- 如有修复，必须有 RED/GREEN 测试证据，并在测试服发布后复审通过。

## 经验门禁

- PowerShell：中文读写、远端 SSH/MySQL 多层命令必须显式 UTF-8，复杂远端命令优先脚本化并后置断言。
- 服务器访问：测试服目标固定 `172.30.30.58`，运行目录 `/opt/intruoyi/runtime`，只读审计需确认目标容器、目标库和运行版本。
- 发布恢复：测试服是正式服前置筛选器；如需要再次发布，必须校验镜像 tag、manifest/schema 契约、健康状态和核心业务结果。
- Bug regression：任何真实新增系统异常必须先记录 BDD、补失败回归测试，再做最小根因修复；不得用 fallback、mock 或静默跳过掩盖。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本任务只接受根因修复或明确判定为业务预期失败。
- 是否从根因和长期维护角度解决：是；所有非预期失败需定位到持久化、识别匹配、批处理状态或发布环境的真实原因。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED。已基于上一任务确认 `status` 截断在 `release-20260708-dcc-status-guard-v3-e1bd69ce96` 后新增数为 0；扩大审计和真实单文件识别触发发现新的非 Codex 问题：访问日志表 `infra_api_access_log.operate_name varchar(50)` 无法保存合法接口操作名 `Recognize controlled file DCC basic data with Codex CLI`，导致异步访问日志写入 `Data too long for column 'operate_name'`。已补 RED/GREEN 测试并将字段正式扩容到 `varchar(128)`，发布 `release-20260708-dcc-operate-log-v1-d25b8d1404` 到测试服后，真实触发 tenant 122 文件 `2054545668044050589` 产品名称识别，业务仍只返回用户明确排除的 `Codex CLI timed out after 300 seconds`；`status` 截断、`operate_name` 截断和其它非 Codex 识别系统异常均为 0。维护控制台配置已恢复主路径并验证预览不再指向临时 worktree；closeout 预览无删除项、无阻塞项，本任务完成。

## Current Status

completed
