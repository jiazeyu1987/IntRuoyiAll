# Backend API Evidence

## Scope

提供 MES 工序只读分页查询，返回展示快照、设备对象列表和执行工序对象，不提供写接口。

## API Contract

Pending after RED test.

## Auth And Errors

- 独立查询权限。
- 缺表或数据组装错误直接返回真实系统错误，不返回默认空成功。

## BDD Scenarios

- 分页一行对应一个 MES 工序目录行。
- 多设备不复制主列表行。
- 只返回二代压力泵目录。
- 无写接口。

## RED

Pending.

## GREEN

Pending.

## Contract Verification

Pending.

## Observability

沿用现有请求日志和异常映射。

## Blockers

None at task start.

