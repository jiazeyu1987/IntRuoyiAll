# Database Schema Evidence

## Scope

- 新增配置 seed：`IntRuoyiBackend/sql/mysql/20260726_mes_edhr_release_dossier_requirements.sql`。
- 目标表：`infra_config`；配置键：`mes.edhr.release.dossier.requirements`。
- 默认值四个字段全 false，确保上线默认不改变现有放行行为。

## Migration

- `20260726_mes_edhr_release_dossier_requirements.sql` 使用过程式 seed 写入 `infra_config`，只在缺少当前配置键时插入默认全 false JSON。
- SQL 在写入前校验 `infra_config` 存在、配置不重复、JSON 合法且 4 个字段都是布尔值。

## Data Safety

- SQL 先检查 `infra_config` 表存在，否则 `SIGNAL SQLSTATE '45000'` fail fast。
- SQL 检查重复配置、JSON 非法、字段缺失或字段非布尔值，均 fail fast。
- 不使用 `INSERT IGNORE`，避免静默吞掉异常状态。

## Rollback

- 该 seed 仅新增缺失配置键；如需回滚，可在确认无运行态依赖后删除 `config_key = 'mes.edhr.release.dossier.requirements'` 的任务自有配置行。
- 已存在的合法配置不会被覆盖，避免回滚或发布时改变金手指已设置的业务状态。

## BDD

- `BDD: 默认关闭保持现状 -> Given 四个开关默认关闭 / When 特殊节点未完成且无附件 / Then 放行预检不因这些资料阻塞。`
- `BDD: 配置变更后必须重跑预检 -> Given 预检后开关状态发生变化 / When 提交放行 / Then 后端拒绝提交并提示重新预检。`

## RED

- `RED: MesProEdhrReleasePrecheckContractTest#sqlKeepsRequiredReleaseObjectsAndPermissions -> FAIL`，新增 SQL seed 文件与默认全 false 配置键尚不存在。

## Verification

- `GREEN: rg -n "mes.edhr.release.dossier.requirements|incomingInspectionReportRequired|INSERT IGNORE|Missing infra_config table" IntRuoyiBackend\sql\mysql\20260726_mes_edhr_release_dossier_requirements.sql -> PASS`，确认配置键、默认字段、缺表 fail fast 和无 `INSERT IGNORE`。
- 正式 JUnit SQL contract 尚未跑通，原因同 backend evidence：后端 reactor 被无关编译阻塞。

## Blockers

- 正式 SQL contract 仍依赖后端 JUnit reactor；当前被并行 system、route、BPM 编译阻塞。
