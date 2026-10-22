# DCC 访问日志事件用户名称补全

## Current Status
ready_for_closeout

## Task Goal

修复受控文件日志定向查询中，访问日志 `user_id` 为空但关联访问事件 `user_id` 有值时，操作人名称退化为数字 ID 的问题。

## Scope

- 生产代码：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/log/DccControlledFileLogQueryServiceImpl.java`
- 测试代码：`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/log/DccControlledFileLogQueryServiceTest.java`
- 任务记录：本目录。
- 不修改数据库迁移、测试 schema 文件、外部数据库、运行服务或 Git。

## BDD

### BDD: 定向访问日志使用事件用户名称

Given 一个 19 位受控文件 ID，存在该文件的访问事件，事件 `user_id` 有值，且关联访问日志 `user_id` 为 null
When 通过真实 Mapper/Service 按该受控文件 ID 查询 `CONTROLLED_FILE_AUDIT` 日志
Then 返回行的 `operatorUserId` 等于事件用户 ID，`operatorName` 等于该用户昵称
And 用户名称映射只来自该次查询已限定的访问事件和访问日志，不扩大到其他文件或租户

### BDD: 访问日志自身用户优先

Given 访问事件用户 ID 与访问日志自身用户 ID 同时存在且不同
When 生成访问日志候选
Then `operatorUserId` 和 `operatorName` 使用访问日志自身用户
And 事件过滤仍只返回目标受控文件的日志

## Design Constraints

- 事件用户只能从当前 `events` 集合并入用户名称映射；定向查询的 `events` 已由 `controlledFileId` 限定。
- 不改变 `toControlledFileAuditCandidate` 中访问日志自身 `userId` 优先于事件 `userId` 的既有语义。
- 测试必须通过真实 Mapper 插入事件和日志、真实 Service 查询；不以 mock 结果代替数据库关联行为。
- 测试为隔离 H2 数据库临时放宽 `user_id` 非空约束，不能修改仓库 schema 或外部数据库。

## Milestones

1. 建立任务记录并写入 Given/When/Then。
2. RED：新增真实 Mapper/Service 回归测试，证明事件用户名称缺失。
3. GREEN：仅修改目标 Service，将同组事件用户并入 `userMap`。
4. 回归：运行最小 `DccControlledFileLogQueryServiceTest` 与 `git diff --check`。
5. 收尾：补验证报告并将任务状态更新为 `ready_for_closeout`；本任务不执行 Git 收尾。

## Expected Verification

- RED 记录测试在修复前失败，失败原因为 `operatorName` 退化为用户 ID。
- GREEN 记录最小 Maven 测试通过。
- 覆盖 `access_log.user_id=null`、事件用户名称、访问日志自身用户优先级和目标文件过滤。
- `git diff --check` 通过。

## Verification Commands

```text
mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileLogQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
git diff --check
```
