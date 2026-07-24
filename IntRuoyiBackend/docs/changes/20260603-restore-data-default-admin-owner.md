# Change Decision: Restore Data Default Admin Owner

## Request

用户要求解决运行控制台点击恢复数据遇到的阻碍，所有默认责任人都是 `admin`，快照、演练等问题都改成不需要。

## Baseline

当前默认 `admin` 只覆盖 `release-owner` 的发布/回滚动作；`restore-data` 需要 `data-owner`，缺少显式责任矩阵时仍会被责任人门禁阻断。恢复演练报告和现场快照阻断已在上一任务取消。

## Classification

Requirement change / operations gate change.

## Impact

- `prod + restore-data + data-owner` 未显式配置时默认使用 `admin`。
- 显式配置的 data-owner 仍优先生效。
- 恢复数据仍需要 PROD 确认、原因、有效恢复候选、manifest、checksum 和镜像标签。

## Decision

Accepted. Add default `admin` owner for restore-data data-owner instead of bypassing responsibility validation.

## Downstream

- Update `RuntimeOpsResponsibilityServiceImpl` default owner baseline.
- Update responsibility and runtime control unit tests.
- Keep the previous no-rehearsal/no-snapshot restore candidate contract.

## Blockers

None. User explicitly requested the default owner behavior.
