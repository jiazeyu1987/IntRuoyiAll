# eDHR 历史批记录只读页不依赖 BATCH 流转门禁

## Task Goal

当历史批记录页签读取已归档批次时，不再为了展示只读历史内容而计算当前任务流转门禁；即使历史路线快照或当前路线缺少 BATCH 流程配置，也应展示已经持久化的批记录执行快照、任务事件和归档目录，不因当前配置缺失阻断全部历史信息。

## Milestones

- [x] 定位历史批记录页签的数据加载入口与后端缺失 BATCH 配置错误抛出点。
- [x] 增加回归测试，先复现缺失 BATCH 配置导致历史页签展示失败的行为。
- [x] 实现最小正式修复：终态历史批次跳过当前流转门禁，继续读取已持久化执行快照。
- [x] 运行定向验证并记录 RED/GREEN/回归结果。

## Expected Verification

- 后端或前端定向回归测试覆盖缺失 BATCH 门禁配置时历史批记录页签仍返回持久化执行快照。
- 相邻正常批次仍能展示历史批记录内容。
- 不引入 fallback、默认成功、吞异常或前端隐藏后端系统错误。

## Current Status

ready_for_closeout：实现、定向验证和经验门禁沉淀已完成；提交/推送仍待处理。当前 `int_main` 落后 `origin/int_main` 2 个提交，且工作区存在大量并行任务脏改动，不能安全执行全工作区基线提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。历史批记录页签是终态只读读取，不再执行当前流转门禁；活动批次仍沿用正式 BATCH 配置 fail-fast。
- `是否从根因和长期维护角度解决`：是。根因是历史只读页复用了活动任务门禁计算，并且签名单元格解析反查当前 Jimu 报表；修复后历史读取以已持久化执行快照/布局为准。
- `是否存在临时补丁或绕过`：否。

## Baseline

- Existing dirty-worktree baseline commit: `125d640fa chore: baseline existing dcc dirty work before edhr history fix`.
