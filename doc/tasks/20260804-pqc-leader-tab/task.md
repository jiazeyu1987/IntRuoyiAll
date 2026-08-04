# 20260804 PQC 组长内容独立页签

## Task Goal

将 PQC 组长相关内容从组长工作台主内容中拆出，改为在专门页签显示；组长工作台主内容不再直接显示 PQC 组长内容。

## Milestones

- [ ] 定位现有组长工作台与 PQC 组长内容实现边界
- [ ] 编写最小静态合同，先证明当前 PQC 内容仍混在组长工作台中
- [ ] 实现专门页签展示 PQC 组长内容，并从默认工作台内容中移除
- [ ] 运行定向验证并记录 RED/GREEN/REGRESSION 证据
- [ ] 完成收尾检查、清理和最终状态记录

## Expected Verification

- 运行任务专用静态合同，覆盖 PQC 组长内容只能在专门页签下显示。
- 运行相邻前端静态合同或 `pnpm ts:check`，若受历史无关问题阻塞则记录首个无关失败。
- 运行 `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-leader-tab/frontend-feature-evidence.md`。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按页签信息边界调整展示结构，不以隐藏异常或默认空数据替代正式展示。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- 已读取 `docs/experience-index.md`；命中 PQC/班组长工作台相关经验，待定位代码后补入精确门禁摘要。

