# R3 四轮复跑计划与收口

## Current Status

blocked — 24例业务执行及Root复核已关闭，12 PASS / 5 FAIL / 7 BLOCKED；工程清理及Git门禁另记。

## Goal / Authorization

按当前用户要求，01—06、07—12、13—18、19—24每轮6例顺序重跑，所有原线程gpt-6.1-sol / low；每轮全部关闭和Root复核后放行下一轮。仅验证及搜集结果。

## BDD

Given 固定int_qms真实8061/48061环境与任务自有账号、数据；When 四轮真实Playwright页面复跑；Then 每例保留真实覆盖、首错、终态、trace、残留与关闭记录，Root逐例复核后汇总，不把执行关闭当作PASS。

## Milestones / Verification

1. [x] 实际分支、端口、Jar及源文件快照核对。
2. [x] 24原线程四轮派工，模型和low设置入台账。
3. [x] 四轮关闭复核，268份trace ZIP CRC；第四轮47份SHA。
4. [x] 24例结果及问题汇总，最终运行源码与Jar指纹一致。
5. [ ] 工程closeout：无当轮Git写入授权；不清理历史及并行资产，不宣称completed。

## 设计约束检查

全部验收动作真实前端，不用API/DB业务动作、mock、响应JSON oracle或伪造登录。未知提交/签名不重复；一度失败不能被后续读到成功掩盖。仅操作任务自有数据，无生产修复、构建、重启、迁移、远程、Git写入、新线程或子Agent。音视频仅附件，一人一部门。不补造第8项。

## Evidence

rerun-r3-dispatch-ledger.json；rerun-r3-results.md；rerun-r3-issues.md；rerun-r3-round1-review.json至rerun-r3-round4-review.json；rerun-r3-final-runtime-check.json。

Closeout预览退出0，623项历史删除候选，未apply；本次执行及汇总已关闭，工程门禁blocked。详见execution-log.md。
