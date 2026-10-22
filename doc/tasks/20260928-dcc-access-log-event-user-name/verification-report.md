# Verification Report

## Result

PASS（定向修复范围）。

## Implementation

- `DccControlledFileLogQueryServiceImpl` 在构造访问日志用户映射时，同时收集当前查询范围内访问日志和访问事件的非空 `userId`。
- 定向查询的事件集合来自 `selectListByControlledFileId(controlledFileId)`；没有跨文件扩大事件或用户查询范围。
- `toControlledFileAuditCandidate` 仍使用访问日志自身用户优先、事件用户回退的既有规则。

## RED

修复前运行：

```text
mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileLogQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：FAIL。新增场景的 `operatorUserId` 已为事件用户 `2209`，但 `operatorName` 退化为数字字符串 `2209`，证明用户名称映射缺少事件用户。

## GREEN

修复后运行同一命令：

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖内容：

- 19 位 `controlledFileId` 定向查询；
- `access_log.user_id = null` 时使用关联事件用户 ID 和昵称；
- 访问日志自身用户存在时保持优先；
- 其他受控文件事件不进入目标查询；
- 真实 Mapper 插入与真实 Service 查询。

## Diff Check

```text
git diff --check -- <目标 Service> <目标 Test> doc/tasks/20260928-dcc-access-log-event-user-name
```

结果：PASS。

## External State

未修改外部数据库、仓库 schema 文件、运行服务或 Git 历史。测试仅对隔离 H2 连接中的测试表临时放宽 `user_id` 非空约束，以构造目标边界数据。
