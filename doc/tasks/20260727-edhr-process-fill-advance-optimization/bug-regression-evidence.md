# Bug Regression Evidence

## Bug Summary

Bug: 当前工作台和工序推进逻辑隐含“负责人/assignee”口径，不符合用户确认的“无负责人，按填写人集合推进”规则；真实 E2E 夹具还曾因缺少生产工单和动态表单字段归属不一致导致后端 fail-fast。

## Expected Behavior

Expected: 一个工序可有多人填写。当前工序存在过程检验记录填写任务时，仅过程检验填写人可推进；没有过程检验记录填写任务时，当前工序解析出的全部填写人均可推进。个人工作台展示当前用户可填写的 candidate 任务。

## Reproduction

Reproduction: 旧后端目标测试复现 candidate 非 assignee 不可见、主表填写人错误推进或过程检验填写人推进缺失；真实 E2E 曾复现 `生产工单不存在`、`当前工艺路线工序未配置默认批记录报表`、`eDHR 批次工序任务被阻塞`，均定位为测试夹具未满足真实后端前置，而非后端应降级。

## Root Cause

Root Cause: 工作台查询只按 assignee 过滤会漏掉 candidate 填写任务；推进资格没有集中按当前工序填写任务集合和 `PROCESS_INSPECTION` 优先级计算。E2E fixture 早期使用不存在的工单 ID，并把 FormCenter binding key 写入 `batch_record_report_id`，使动态表单被识别为传统批记录。

## RED

RED: 后端目标测试和前端静态合同在旧逻辑下失败；真实 E2E 在修正前被后端正式校验拦截，证明不能用 mock/API-only 代替真实数据。

## GREEN

GREEN: 后端目标测试 PASS；三个前端静态合同 PASS；完整真实 E2E PASS，runKey `EDHR-ADV-6T182008199Z`，DB 断言 nextFillCount `1/0/1`。

## Risk

Verification: 风险集中在同工序多表单 gate 和 FormCenter 动态表单上下文；已通过真实 E2E 覆盖候选非 assignee、主表非推进、过程检验推进、无过程检验推进。未引入 fallback、吞异常或默认成功。

## Blockers

Blockers: none。
