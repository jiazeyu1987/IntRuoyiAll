# 验证报告

## 结果

当前代码融合结果为 `in_progress`，业务代码已经应用并提交到 `D:/IntRuoyiWorktree/xiufu20260826`，v7 runtime 基线已同步，定向回归、schema migration 和独立后端启动验证通过；真实 Tx-C outbox、写入型 E2E 和主工作树运行态仍有阻塞。

## 已融合代码

- 流程3：PQC 数量 fixture 和当前主线签名确认测试适配。
- 流程5：损耗导入测试 fixture、测试 SQL 字段。
- 流程6：服务端权威 batch receipt resolver、入口合同和建批服务测试。
- 流程4：活跃订单完工 receipt 复用，避免 dossier 节点重复调用旧三类回填 writer。
- 流程7：批次执行提交后的 `AFTER_COMMIT` Tx-C 事件触发和生产者映射。
- 流程8：四份材料 `MATERIALS_READY` 权威 receipt 持久化和放行读取。
- 流程4增量：放行 dossier 改为只读 Flow4 completion receipt，删除旧三 writer 计划/写入生产依赖。
- 流程7增量：新增 Flow6->Flow7 witness-only event/application service/invoker，监听阶段固定为 `AFTER_COMMIT`，避免建批事务未提交时执行 Tx-C 或重复消费。
- Stage2.5增量：移除旧 dossier plan/write 依赖，读取正式 completion receipt 结果 ID，使用当前两参数活跃订单模拟服务，并提供 Stage2.5 API入口。

## 已排除代码

- 已在 `int_main` 的流程1、2、7、8、9、10、11代码。
- 流程4 `ac93ad0f6` 与主干 Tx-A 代码内容重复的部分。
- 流程8、10旧分支中相对当前主线的删除和历史实现。
- 流程5提交中的 `docs/worktree-memory.md`。

## 验证

- MES 24模块 compile：PASS。
- 流程3/5/6定向测试：`222/222 PASS`。
- `git diff --check`：PASS。
- runtime guard：PASS，slot 43，前端 `8258`，后端 `48258`。
- 正常目标分支提交：已完成，当前 HEAD 为 `e1b179329ff3ea32227ceca473c4f063d5bcb90a`；不使用 `--no-verify`。
- 流程8材料 receipt 定向验证：`43/43 PASS`，包含门禁服务、receipt adapter、预检和权威放行上下文。
- 流程4 dossier receipt reuse：`1/1 PASS`，活跃订单路径不调用旧三类 writer。
- 流程7 Tx-C 自动触发：代码已接入 `AFTER_COMMIT` application service，应用服务只接收 witness 并唯一调用 producer；真实数据库 outbox 验证仍未执行。
- 流程7事件 witness 映射：`MesProEdhrBatchTraceTxCApplicationServiceContractTest 3/3 PASS`，包含 tenant/batch 校验和 `AFTER_COMMIT` phase 合同。
- 新增材料 receipt SQL 合同：`MesReleaseMaterialGateReceiptSqlContractTest 1/1 PASS`。
- 全融合定向回归：`307/307 PASS`，0 failures、0 errors。
- Flow4/Flow6/Flow7/Flow8/Flow10/traceability 增量回归：`294/294 PASS`，0 failures、0 errors、0 skipped。
- `mvn -o -pl yudao-server -am -DskipTests package`：`BUILD SUCCESS`，30/30 modules。
- 增量后 `mvn -o -pl yudao-module-mes -am -DskipTests compile`：`BUILD SUCCESS`，24/24 modules。
- 增量后 `mvn -o -pl yudao-server -am -DskipTests package`：`BUILD SUCCESS`，30/30 modules。
- 增量稳定运行 Jar SHA-256：`51D2DAF5068F4333DA3D313354299A2796CB163B203359D5F200EB6E0BD52CAF`；`48258` 启动日志确认 `Started YudaoServerApplication`，health HTTP `200`、`{"status":"UP"}`。
- 独立后端启动：`48258` health HTTP `200`，`{"status":"UP"}`；启动日志确认 `Started YudaoServerApplication`，运行 Jar 为 `output/runtime/xiufu20260826/yudao-server-exec.jar`。
- runtime guard：PASS，slot 43，前端 `8258`，后端 `48258`。
- 本地 Docker MySQL migration：PASS；表 `mes_pro_edhr_material_gate_receipt` 为 `InnoDB/utf8mb4_unicode_ci`，18 字段、4 索引、0 行；同一 migration 重跑 PASS。
- migration 后稳定 Jar 启动：PASS；`48258` 日志确认 `Started YudaoServerApplication`，health HTTP `200`、`{"status":"UP"}`，进程随后已停止。
- Stage2.5 增量验证：`MesStage2_5ReceiptHandoffContractTest 1/1 PASS`；24模块测试编译、30模块打包、slot 44 `48259` 启动和 health 均 PASS。
- 真实 Tx-C outbox 写入闭环、写入型 Playwright E2E：未完成。

## 结论

代码融合提交已经完成，当前目标 HEAD 为 `e1b179329ff3ea32227ceca473c4f063d5bcb90a`；`int_main` 已原子 fast-forward 到同一提交。流程4旧双写、流程7 AFTER_COMMIT witness handoff、流程8 receipt 持久化和 Stage2.5 receipt handoff 已补齐；但 `E:/IntRuoyi` 主工作树仍有未提交 overlay，不能把主工作树当前运行文件或定向测试写成流程1-11全链路完成。

剩余阻塞：

1. 真实 Tx-C outbox 写入闭环未执行，缺少可清理的测试批次和真实来源数据。
2. 写入型 Playwright 所需测试租户、生产/PQC/管理者账号、四份材料和清理权限未冻结。
3. `int_main` 分支指针已完成融合，但 `E:/IntRuoyi` 主工作树仍有 309 项 dirty/untracked overlay；其中排除的模拟文件和其它并行改动尚未刷新到新 HEAD，主工作树服务必须保持不作为本次融合运行态使用。
4. 将主干现有 tracked dirty patch 与目标提交做三方保真检查时，BPM 并行改动导致 `FormCenterRuntimeServiceImpl.java:88` 无法解析 `FormTemplateFillRuleAutoDetectService`；这不是流程1-11代码缺陷，不能在本任务中擅自补齐其它任务的服务实现。
5. Stage2.5 已在独立分支修复并融合；真实 Stage2.5 写入型 E2E 仍未执行，必须在测试租户和清理权限具备后验证。
