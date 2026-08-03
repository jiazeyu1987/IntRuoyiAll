# eDHR 历史批记录缺失 BATCH 配置页签清空

## Task Goal

当历史批记录页签遇到历史批次缺失正式 BATCH 批记录配置时，不再因为后端重组批记录配置而阻断页面访问；页签内缺失配置对应的历史批记录内容应清空/不展示，并保持错误链路可见，不用前端静默吞错。

## Milestones

- [ ] 定位历史批记录页签的数据加载入口与后端缺失 BATCH 配置错误抛出点。
- [ ] 增加回归测试，先复现缺失 BATCH 配置导致历史页签展示失败的行为。
- [ ] 实现最小正式修复，只清空缺失配置的历史批记录页签内容，不影响正常 BATCH 配置批次。
- [ ] 运行定向验证并记录 RED/GREEN/回归结果。

## Expected Verification

- 后端或前端定向回归测试覆盖缺失 BATCH 配置时历史批记录页签为空。
- 相邻正常批次仍能展示历史批记录内容。
- 不引入 fallback、默认成功、吞异常或前端隐藏后端系统错误。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。用户明确要求删除缺失 BATCH 配置的历史批记录页签内容，修复应是明确业务分支，不吞掉非目标错误。
- `是否从根因和长期维护角度解决`：是。以历史页签的信息边界处理缺失正式 BATCH 配置，不混淆表单槽位、工序开始和正式批记录表单。
- `是否存在临时补丁或绕过`：否。

## Baseline

- Existing dirty-worktree baseline commit: `125d640fa chore: baseline existing dcc dirty work before edhr history fix`.
