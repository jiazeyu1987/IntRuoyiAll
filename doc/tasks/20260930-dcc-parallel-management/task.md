# DCC 多线程分工与主管理 Review

## Task Goal

作为主管理，建立可直接交给四个独立 Codex 线程的任务书、共同契约、文件归属、验证计划、交付模板与 Review 门禁。当前轮交付任务包与计划 Review，不在未锁定共同基线时启动生产代码修复。

## Milestones

1. 核对需求v1.3、当前分支与既存改动，定位共享文件。
2. 编写docs/dcc-parallel-delivery中的主管理入口及A/B/C/D任务书。
3. 只读复核模块源码、既有测试、依赖与任务边界；Review修正计划冲突。
4. 结构验证、记录当前授权与未执行动作，ready_for_closeout后cleanup。

## Expected Verification

- 四份任务书分别包含目标、允许修改范围、排除范围、BDD、验证命令、跨线程依赖、交付格式和复制用启动指令。
- 需求AC-01..AC-26全部有责任线程与验证范围，主管理负责集成证据。
- 公共大文件和公共DO/VO/API必须唯一归属；独立worktree不能被误写成数据隔离。
- 任务包保留当前实际分支、HEAD及关键源码UTF-8/LF规范化SHA256，不把工作区当成HEAD。
- 核对所有既有测试路径真实存在，新测试明确标为计划新增；不运行业务测试或E2E。
- 结构与内容核验通过后记录ready_for_closeout，cleanup仅当前任务。

## Current Status

blocked — 任务包交付、计划Review、结构验证及cleanup preview/apply通过；仅仓库提交门禁因未获本轮提交推送授权未完成。正式开发共同基线仍待用户选择；未启动生产代码修复。

## 设计约束检查

- 用户本轮明确要求分配线程并主管理Review，已授权针对任务的并行只读审查；只读子任务不改文件、不测试、不运行服务。
- 本轮未授权Git提交推送、数据库写入、生产服务重启、发布或真实页面E2E，不执行。
- 实际分支int_qms，目录名含int_main不能用作分支依据。HEAD a9bcb6d36d96145ddc1252f111347b644b328deb；派发前385条Git状态项，其中236个tracked改动、149个untracked条目。条目数不是精确文件数。
- 单纯从HEAD创建worktree无法包含现有未提交DCC实现；各开发线程需从共同正式基线开始。主管理已提出基线选择问题，等待文本答复，不把未回复当作授权。
- 此任务是分工与文档交付，仅结构验证，不适用生产代码RED/GREEN；后续各模块生产改动必须先BDD、RED、GREEN。

## Cleanup Keep

- doc/tasks/20260930-dcc-parallel-management/task.md
- doc/tasks/20260930-dcc-parallel-management/execution-log.md
- doc/tasks/20260930-dcc-parallel-management/verification-report.md
