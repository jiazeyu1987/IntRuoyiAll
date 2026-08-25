# 验证报告

## 结果

当前代码融合结果为 `in_progress`，业务代码已经应用到 `D:/IntRuoyiWorktree/xiufu20260826`，v7 runtime 基线已同步，流程4/7/8补强代码正在提交和回归。

## 已融合代码

- 流程3：PQC 数量 fixture 和当前主线签名确认测试适配。
- 流程5：损耗导入测试 fixture、测试 SQL 字段。
- 流程6：服务端权威 batch receipt resolver、入口合同和建批服务测试。

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
- 正常融合提交：待执行；不使用 `--no-verify`。
- 流程8材料 receipt 定向验证：`43/43 PASS`，包含门禁服务、receipt adapter、预检和权威放行上下文。
- 流程4 dossier receipt reuse：`1/1 PASS`，活跃订单路径不调用旧三类 writer。
- 流程7 Tx-C 自动触发：代码已接入 `AFTER_COMMIT` 事件，待批次服务事件消费回归和真实数据库 outbox 验证。
- 真实数据库迁移、服务启动、写入型 Playwright E2E：未运行。

## 结论

代码融合本身没有 Git 冲突；流程4旧双写和流程8 receipt 持久化已补齐，流程7自动触发已接入。当前剩余项是事件消费回归、数据库迁移/真实 outbox、主线集成和真实全链路 E2E。不能把当前定向测试结果写成流程1-11全链路完成。
