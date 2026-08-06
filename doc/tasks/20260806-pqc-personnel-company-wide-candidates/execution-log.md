# Execution Log

## Intent

用户反馈 `新增 PQC 检验员` 弹窗下拉选择范围应为全公司范围，不应限制为当前组长下属。

## BDD

- BDD: PQC新增候选来自全公司 -> Given 当前 PQC 组长打开新增 PQC 检验员弹窗 / When 输入姓名或账号搜索 / Then 后端使用全公司正式系统用户搜索，不按当前组长下属过滤。
- BDD: PQC提交校验与候选同范围 -> Given 选择的正式用户不是当前组长下属但属于全公司系统用户 / When 提交关联 / Then 后端允许创建 `leader_type=PQC`、`scope_type=EMPLOYEE` 的 scope，重复关联仍在写库前业务拒绝。

## Command Log

- PENDING: RED/GREEN 待执行。

## Completed Work

- 已建立任务记录。

## Remaining Blocker

- 无。
