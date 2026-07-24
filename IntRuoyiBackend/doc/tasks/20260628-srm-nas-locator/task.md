# 任务：SRM NAS定位 后端与 SQL 实现

## 任务目标

- 为 SRM 新增 `NAS定位` 后端接口、刷新任务、快照落库与文件下载能力。
- 新增 SRM 菜单 SQL、快照任务表、快照条目表和相关契约测试。
- 保证搜索只读取最新成功快照，刷新成功后稳定服务搜索与下载。

## 当前状态

COMPLETED

## Current Status

Completed

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-admin-scheduler-workbench-role-grant\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成；本任务已闭环完成。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 真正触发 NAS 刷新或真实 E2E 前，必须先记录 `GREEN: experience-preflight -> PASS`。
  - SQL、Markdown、Java 源码和日志文件的中文读写必须保持 UTF-8。
  - 本轮只允许本机测试租户做真实写入验证，不得操作服务器、正式环境或生产租户数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是。用户于 `2026-06-29` 明确批准“只做可读的,不可读的不做”；当目录遍历遇到 `access denied` 时显式跳过该目录并继续索引可读内容，其它异常仍 fail-fast。风险是快照只覆盖当前账号可读内容；若后续恢复为“整共享必须全读”的规则，可回退此目录级跳过逻辑。
- `是否从根因和长期维护角度解决`：是。后端直接复用现有 NAS 浏览服务，补齐单会话遍历、路径规范化修复、动态菜单契约、快照保留策略和流式下载，不走二次 HTTP 转发或本地磁盘中转。
- `是否存在临时补丁或绕过`：否。本期 readable-only 行为是用户批准后的正式范围，不是未授权临时绕过。

## BDD 场景

- `BDD: 没有成功快照时 page/download 都显式暴露前置缺失 -> Given 当前租户没有任何成功快照 / When 调用 page 或 download / Then 接口必须返回明确失败信息，不返回默认空成功。`
- `BDD: 同租户刷新任务不允许并发运行 -> Given 当前租户已有 RUNNING 状态刷新任务 / When 用户再次调用 refresh / Then 系统必须阻断第二次刷新并返回显式错误。`
- `BDD: 刷新成功后目录与文件都落库且搜索仅返回 FILE -> Given 当前 NAS 共享可正常遍历 / When 刷新任务成功 / Then 目录与文件都应写入快照表，page 搜索只返回 FILE 记录。`
- `BDD: 刷新中遇到不可读目录时仅跳过该目录并保留可读快照 -> Given 用户已批准 readable-only 范围且当前账号对部分目录无读取权限 / When 刷新任务继续遍历 / Then 系统应跳过 `access denied` 目录、保留其它可读目录与文件的快照，其它异常仍需显式失败。`
- `BDD: 下载必须校验缓存记录类型和受保护共享配置 -> Given 下载请求按缓存 id 命中记录 / When 记录不存在、记录类型为 DIRECTORY 或当前共享配置漂移 / Then 接口必须立即失败；只有 FILE 且共享正确时才允许流式下载。`

## 里程碑

1. M1：补后端任务文档、执行日志和 evidence 骨架。`COMPLETED`
2. M2：先写 SQL 契约测试与后端 RED 单测。`COMPLETED`
3. M3：实现 SQL migration、DO/Mapper/Service/Controller。`COMPLETED`
4. M4：跑后端测试与真实验证，回填证据。`COMPLETED`

## 预期验证

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest" test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前结论

- 已新增 `GET /admin-api/srm/nas-locator/status`、`GET /page`、`POST /refresh`、`GET /download` 四个正式接口。
- 已新增 T6 migration、菜单 `991100-991103`、刷新任务表 `srm_nas_locator_refresh_task` 和快照条目表 `srm_nas_locator_entry`。
- `path_hash = SHA-256(path)` 已作为正式唯一键方案落地，解决 MySQL `utf8mb4 + varchar(1000)` 唯一索引超长问题。
- `NasBrowserService` 已新增 `executeInSession(...)` 与 `writeFileTo(...)`；`SrmNasLocatorServiceImpl` 刷新时复用单 SMB 会话，下载时直接流式输出。
- 路径规范化已修复为不裁剪真实路径段字符，解决尾部特殊空白字符目录的路径不存在问题。
- 当前正式行为为：跳过系统/隐藏目录和 `access denied` 目录，仅索引当前账号可读内容；其它异常仍 fail-fast。
- 当前真实成功快照：测试租户 `tenant_id=122` 最新任务 `id=9`，`status=SUCCESS`，`directory_count=5079`，`file_count=33966`。

## 最终验证结果

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest" test` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- 真实数据库回读 -> PASS，测试租户最新成功任务 `id=9 / SUCCESS / 5079 directories / 33966 files`
- 真实下载接口 -> PASS，浏览器收到 UTF-8 可读文件名附件

## 当前阻塞

- 无。当前范围内的后端接口、菜单、SQL 和真实快照链路均已完成验证。
