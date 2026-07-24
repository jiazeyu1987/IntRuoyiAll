# 执行日志

## BDD

- BDD: eDHR 自动补齐工序编码使用 ER 前缀 -> Given Word 导入自动生成 eDHR 工艺路线且缺少工序主数据；When 后端自动补齐工序；Then 新工序编码必须以 `ER` 开头，不得包含 `EDHR_PROC_`。
- BDD: 已有旧编码立即可见为短编码 -> Given 本机已有 `EDHR_PROC_...` 工序记录；When 执行本次本机数据修复；Then 工序列表不再显示 `EDHR_PROC_...`，对应编码变为 `ER...`。

## TDD 证据

- RED: mvn -pl yudao-module-mes -Dtest=MesProBatchRecordRouteGenerationCodeRuleTest test -> FAIL，新增回归测试首次路径按父工程目录拼接，Surefire 在模块目录执行导致 `NoSuchFileException`。
- GREEN: mvn -pl yudao-module-mes -Dtest=MesProBatchRecordRouteGenerationCodeRuleTest test -> PASS，`PROCESS_CODE_PREFIX` 已固定为 `ER`，测试 1 个用例通过。
- RED: git commit -> FAIL，仓库钩子要求 `sql/mysql/` 迁移必须同步提供 `script/tests/` 门禁测试。
- GREEN: python -m pytest script/tests/test_mes_pro_process_er_prefix_sql.py -> PASS，SQL 迁移门禁 3 个用例通过。
- GREEN: mvn -pl yudao-module-mes -Dtest=MesProBatchRecordRouteGenerationCodeRuleTest test -> PASS，后端工序编码前缀回归测试通过。

## 数据修复

- GREEN: local-db-prefix-update -> PASS，本机 `mes_pro_process` 已更新 28 行，`EDHR_PROC_` 剩余 0 行。
- GREEN: local-db-prefix-verify -> PASS，本机 `mes_pro_process.code LIKE 'EDHR_PROC_%'` 回查为 0。

## 数据库证据

- Data change goal and affected entities: 将 eDHR Word 导入自动补齐工序编码从 `EDHR_PROC_...` 改为 `ER...`，影响 `mes_pro_process.code`。
- Database engine and migration tool: MySQL 8.0.39，本机 Docker 容器 `int-ruoyi-mysql`；本次使用 SQL 直接更新本机数据，并新增可重复 SQL 脚本。
- Data safety analysis: 更新前检查目标 `ER...` 编码冲突数为 0；只替换字符串前缀，不删除、不合并、不跨环境。
- Rollback or recovery plan: 可用反向前缀替换恢复旧编码，恢复前需同样检查目标冲突。

## 收尾

- 状态：COMPLETED
- 阻塞：无
