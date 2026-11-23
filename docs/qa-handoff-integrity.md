# QA已办记录完整性

审批中心的QA已办结果必须来自同租户、同评审的原始记录及其有效签名。交接任务的 `DONE` 状态不能单独证明审批通过；缺失证据时不得跳过任务、构造评审或清空历史。

## 运行检查

配置 `QA_AUDIT_DB_HOST`、`QA_AUDIT_DB_PORT`、`QA_AUDIT_DB_NAME`、`QA_AUDIT_DB_USER`、`QA_AUDIT_DB_PASSWORD` 后运行：

```powershell
python scripts/repair/audit-qa-handoff-integrity.py --tenant-id 186
```

命令只读查询同租户QA来源的交接记录，输出缺失或已软删除的来源ID。无异常退出0，有异常退出2；连接和查询错误直接失败。检查范围仅为来源存在性，签名及决定人与时间一致性仍由应用校验。不得将本检查通过等同完整审批验收。

## 保留约束

发布迁移 `20261122_mes_qa_review_retention_guard.sql` 安装两个具名触发器：

- 已关闭或被交接引用的评审禁止物理删除、修改主键、修改租户和改变删除标记；已软删除的交接同样保留来源。
- 已关闭评审禁止重新改成待评审，以免通过改状态绕过保留规则。
- 正常待评审到关闭、处置意见及签名落库沿用现有事务；迁移不修改历史业务行。

触发器不能阻止具备DDL权限的操作者执行 `TRUNCATE`、删表或移除触发器。业务库清理必须先检查交接与评审引用，保留原始决定和签名；不能通过关触发器清掉审批证据。

回退仅移除 `trg_mes_ncr_retained_no_delete`、`trg_mes_ncr_retained_identity` 两个触发器，需明确承担来源再次被删除的风险。原始证据恢复使用原行INSERT，不需要关闭这两个约束。

## 缺失数据的修复

从备份或二进制日志定位准确的原始行，核对租户、评审ID、交接来源、决定人、时间及完整签名引用后恢复，随后由应用校验签名内容与哈希。不得根据交接理由补猜材料、快照或意见；无法取得完整原行的记录仍视为阻塞。恢复后重新运行只读检查，并通过Playwright打开真实已办页面确认。

本地首次全量检查发现租户186有86条有效交接引用22份缺失评审，包含21条QA已办任务；只恢复其中一份不能代表全部问题关闭。初始清单见 `output/qa-handoff-integrity-186.json`。该输出为本地诊断证据，不作为发布迁移的数据输入。

## 本次处理结果

已协调恢复109—117、122—132及134共21份已关闭评审。每份使用二进制日志的完整原行，原签名和原交接不变；逐字段及哈希复核证据见 `workspace/qa21-original-restoration-verification-20261122.json`。两个保留触发器已应用本地库，SQL哈希与当前迁移一致，见 `output/qa-retention-local-apply.json`。独立MySQL的6项检查及23项迁移依赖策略检查通过。

恢复后只读检查仍以失败退出：剩余133一份原评审及其2条引用（QA交接CANCELED、冻结通知DONE），见 `output/qa-handoff-integrity-186-after.json`。133原状态为pending_review，其周期1009200637已不存在，工单当前temporary_frozen为0。`MesProEdhrNonconformanceReviewMapper.selectFreezeCounts` 会按工单统计pending_review而不要求原周期存在，直接写回可能改变冻结计算；因此保留完整原始日志，不恢复成在线待评审、不补造已关闭决定。其后续处理须明确废弃周期的历史保留和冻结业务规则。

运行恢复后发现第二项根因：QA正式签名服务写入“指定系统账号”身份，而QA结果读取端只接受“当前账号”身份。已沿正式签名协议补齐这两种系统账号身份的PASSWORD/PIN读取规则，保留人员、租户、签名对象、原文与证据哈希，以及签名内核的冻结身份校验。QueryContract与HistoricalRead共54项检查通过。新包通过源码与包来源校验，已由协调聊天统一更新48081后端。

真实Playwright已确认测试1管理员已办页面恢复，显示共24条，第1页20条、第2页余下4条QA均可读，无原系统异常。快照为 `workspace/registration-done-signer-fix-20261122.txt`、`workspace/registration-done-restored-page2-20261122.txt`，截图为 `output/playwright/registration-done-restored-20261122.png`。该验证仅覆盖本次已办读取故障，不代表133历史异常已修复，也不代表重新执行QA处置或生产放行。
