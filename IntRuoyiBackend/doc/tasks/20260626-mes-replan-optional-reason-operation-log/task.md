# 任务：MES 重排可选理由补齐操作日志与本机 Schema 契约

## 任务目标

- 修复 `/admin-api/mes/pro/auto-schedule/replan/apply` 在 `reason` 为空时仍因 `mes_pro_schedule_order_operation_log.reason` 非空约束写入失败的问题。
- 保持 `/admin-api/mes/pro/auto-schedule/apply` 自动排产发布继续要求业务原因，不扩大到其他仍需理由的操作。
- 让后端服务、迁移 SQL、本机重启 schema 保底流程与真实本地库对“手动重排理由可选”的契约保持一致。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 backend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-replan-reason-optional\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已放开 replan apply 的服务层理由校验，但用户再次实测时暴露出操作日志表 `reason` 列仍为非空约束，导致落库阶段失败；本次单独修复持久化契约回归。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
- 适用强制门禁：
  - 本轮仅处理本机后端代码、SQL 契约、定向单测与本地数据库 schema 修正，不做服务器发布或远端环境写入。
  - 涉及本机真实数据库 schema 写入前，必须先在 `execution-log.md` 记录真实表结构核对结果与修复动作，不得凭记忆直接改列约束。
  - 发布、构建、部署脚本修改统一只在 `D:\ProjectPackage\Int\IntRuoyiMaintance`；本次若需补本机 `restart-int-ruoyi-local.ps1` schema guard，仅限当前源码仓库内已有本机重启脚本范围。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不会吞掉插入失败，也不会对其他必须填写原因的操作静默放宽。
- `是否从根因和长期维护角度解决`：是。同步修正操作日志表 schema 与本机 schema guard，避免接口层放开后再次被日志表约束反向阻断。
- `是否存在临时补丁或绕过`：否。若允许空理由，数据库契约也必须正式允许该字段为空。

## BDD 场景

- `BDD: 手动重排 apply 缺少 reason 仍可写操作日志 -> Given 用户已生成有效重排预览且调用 replanApply 时 reason 为空 / When 后端写入 mes_pro_schedule_order_operation_log / Then reason 列允许为空，不得再因 Field 'reason' doesn't have a default value 失败。`
- `BDD: 自动排产 apply 继续要求 reason -> Given 用户发布自动排产结果 / When apply 请求缺少 reason / Then 服务层仍返回 PRO_SCHEDULE_ORDER_REASON_REQUIRED，不依赖数据库约束兜底。`
- `BDD: 真实本机库与新建库契约一致 -> Given 本机运行库或后续新建库需要创建/修复 mes_pro_schedule_order_operation_log / When 执行迁移 SQL 或本机重启 schema 保底流程 / Then reason 列必须允许 NULL，避免运行态与源码契约漂移。`

## 里程碑

1. M1：创建任务包并补 RED 契约测试。
2. M2：最小修改迁移 SQL / schema guard 与必要后端断言。
3. M3：运行 GREEN 测试并修正本机真实库。
4. M4：回写证据并评估本次改动的提交边界。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_schedule_order_freeze_audit_sql.py -q`
- `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q`
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldAllowMissingReason -Dsurefire.failIfNoSpecifiedTests=false test`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SHOW CREATE TABLE mes_pro_schedule_order_operation_log;"`

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_mes_schedule_order_freeze_audit_sql.py -q` -> PASS，`3 passed in 0.11s`
- `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，`14 passed in 0.12s`
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldAllowMissingReason -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SHOW CREATE TABLE mes_pro_schedule_order_operation_log;"` -> PASS，真实表结构已显示 `reason ... DEFAULT NULL`
- `curl.exe --fail --silent --show-error --max-time 15 http://127.0.0.1:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`

## 阻塞与影响

- 当前无阻塞；真实本地库 `mes_pro_schedule_order_operation_log.reason` 已修正为可空，手动重排 apply 不再受该列非空约束反向阻断。
- 本次范围仅限 MES 自动排产 / 重排操作日志相关后端代码、SQL、schema guard、任务文档与证据，不混入其他模块改动。
