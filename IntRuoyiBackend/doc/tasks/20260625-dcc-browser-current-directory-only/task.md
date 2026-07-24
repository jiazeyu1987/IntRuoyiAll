# 任务：DCC 受控浏览目录仅显示当前层

## 任务目标

在保持 `DccControlledFilePageReqVO.directoryId` 与 `includeDescendantDirectories` 接口字段不变的前提下，明确 DCC 受控浏览默认语义为“仅当前目录”；当 `includeDescendantDirectories=true` 时仍保留递归子目录能力，防止误伤其他既有调用方。

## 里程碑

- [x] M1：创建任务文档，记录经验门禁、设计约束检查与 BDD 场景。
- [x] M2：先补后端 RED 回归，锁定递归与非递归目录语义。
- [x] M3：最小修改浏览查询逻辑，仅在显式请求时递归子目录。
- [x] M4：运行后端定向单测并补齐执行证据。

## 预期验证

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

已完成。

## 最终验证结果

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS

## 前一任务检查

- 后端最近任务文档中无当前 DCC 浏览链路未完成阻塞项；允许继续本任务。
- 当前后端仓库存在其他未归属脏改动；本任务只修改 DCC 浏览查询测试、查询实现与本任务文档，不覆盖其他改动。

## 经验门禁

- `docs/experience-index.md`：本任务仅做本机源码与定向单测，不执行真实 E2E、数据库写入、服务器联调或高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只收敛正式浏览语义，不引入默认递归的隐式兼容分支。
- `是否从根因和长期维护角度解决`：是。根因是后端在有 `directoryId` 时默认递归收集子目录；本次改为仅在显式 `includeDescendantDirectories=true` 时才递归。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 非递归目录浏览只查当前目录 -> Given 请求携带 directoryId 且未传 includeDescendantDirectories 或传 false When 查询受控浏览列表 Then 仅返回当前目录文件，不返回子目录文件。`
- `BDD: 显式递归目录浏览继续包含子目录 -> Given 请求携带 directoryId 且 includeDescendantDirectories=true When 查询受控浏览列表 Then 返回当前目录及其子目录文件。`
- `BDD: 最新版本聚合不受目录语义影响 -> Given 浏览页请求 latestVersionOnly=true When 查询仅当前目录文件 Then 仍按文件 master 聚合为最新版本一行。`

## Cleanup Keep

- `doc/tasks/20260625-dcc-browser-current-directory-only/task.md`
- `doc/tasks/20260625-dcc-browser-current-directory-only/execution-log.md`
