# rerun-20261020-r2-round2 E10

## Current Status

blocked — 业务完整A–E五分支本轮PASS，临时配置UI恢复回读、6个context/browser/runner关闭exit0、9份脱敏trace校验PASS；已HOLD。Git未授权，正式工程收尾blocked。曾在关闭资源前实际标ready_for_closeout；独立新证据置于runs/rerun-20261020-r2-round2/，不复制或覆盖历史结果。

## Goal and BDD

Given int_qms新运行包及本例真实账号、独立新申请和唯一正文名；When Playwright真实前端完成公共/私有草稿、驳回返工、主动换类型和冻结人员不可用分支；Then A公共变化明确二次确认、B私有配置保存重开隔离、C连续检入及重提保留原执行矩阵、D取消零变化且确认后保存重开使用目标矩阵、E真实最终提交显示具体禁用人员并零新增版本/流程，恢复后原矩阵重提至原主管本人新待办。

## Milestones and Verification

1. 实际分支、41508/48061、20976/8061、Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147及resolver伴随类核对，写execute/runtimeReady门禁。
2. 本例原账号真实登录；原矩阵候选/备注及批准人启用状态只读核对，先登记恢复函数再临时UI改动。
3. 五个独立新申请；逐分支执行，保留首次失败与校准，不无限重跑。
4. UI恢复原值回读、trace脱敏完整性核验、browser/context/runner关闭与handoff。

## 设计约束检查

原被验收业务动作全部UI完成；只听自然请求元数据，不以响应JSON断言。只动本例CAT1/CAT2矩阵候选/备注及原批准人临时状态并finally恢复。部门为空只观察，不新增门槛或改组织。无产品修改、Maven/构建/重启、Git、直接业务API/DB、子Agent。不启动第三轮；旧FAIL不改。历史runner不覆盖，新runner仅绑定本轮运行证明及runId。
