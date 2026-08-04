# 20260804 PQC 组长内容独立页签

## Task Goal

将 PQC 组长相关内容从组长工作台主内容中拆出，改为在专门页签显示；组长工作台主内容不再直接显示 PQC 组长内容。

## Milestones

- [x] 定位现有组长工作台与 PQC 组长内容实现边界
- [x] 编写最小静态合同，先证明当前 PQC 内容仍混在组长工作台中
- [x] 实现专门页签展示 PQC 组长内容，并从默认工作台内容中移除
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION 证据
- [ ] 完成收尾检查、清理和最终状态记录

## Expected Verification

- 运行任务专用静态合同，覆盖 PQC 组长内容只能在专门页签下显示。
- 运行相邻前端静态合同或 `pnpm ts:check`，若受历史无关问题阻塞则记录首个无关失败。
- 运行 `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-leader-tab/frontend-feature-evidence.md`。

## Current Status

ready_for_closeout

- 当前工作树已恢复为 PQC 独立页签口径并通过定向验证；最终提交/推送仍需按 Git closeout 规则处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按页签信息边界调整展示结构，不以隐藏异常或默认空数据替代正式展示。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：静态合同 PASS 与真实 E2E PASS 必须分开记录；本任务执行静态合同和 `pnpm ts:check`，未将静态合同冒充真实 Playwright。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：更新 `tests/e2e/*static.spec.js` 时按稳定文件/组件/路由标记断言，不依赖坐标或截图。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 组长页继续读取 `pqcItemDetails/itemResults` 项目级明细，不恢复固定 `length/appearance/seal/pressure` 或 legacy `pqcPieceValues` 作为权威事实。
