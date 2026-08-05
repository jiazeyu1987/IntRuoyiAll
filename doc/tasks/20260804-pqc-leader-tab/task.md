# 20260804 生产组长与 PQC 组长独立主导航页签

## Task Goal

将 `生产组长` 和 `PQC组长` 改为类似 `批次执行` 的两个独立主导航菜单入口，放在 eDHR 父菜单下、`QA` 下方；两者不再作为 eDHR 批次页面内部 Tab，也不再混在单一 `组长工作台` 内展示。

## Milestones

- [x] 定位现有 eDHR 内部组长 Tab、共享工作台、页面关系图和动态菜单边界
- [x] 编写最小静态合同，锁定 `QA -> 生产组长 -> PQC组长 -> 批次执行` 主导航顺序
- [x] 实现独立主导航菜单页面口径并移除 eDHR 内部组长 Tab/旧路由依赖
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION 证据
- [x] 修复并应用本地菜单迁移，使 `admin` 可见 QA、生产组长和 PQC组长入口
- [x] 运行真实只读 Playwright E2E，验证 `admin` 可见并能打开两个独立页签
- [ ] 完成提交/推送 closeout

## Expected Verification

- 运行任务专用静态合同，覆盖 `批记录表单 -> QA -> 生产组长 -> PQC组长 -> 批次执行 -> 表单追溯 -> 表单日志` 菜单顺序。
- 证明生产组长/PQC组长分别使用 `/mes/pro/process-pool/production-leader` 与 `/mes/pro/process-pool/pqc-leader`。
- 证明 eDHR 内部 tabs 和旧 `/mes/pro/feedback/edhr-batch-*-leader` 路由不再承载组长内容。
- 运行 `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`、`node tests\e2e\edhr-batch-page-graph-tab-static.spec.js`、`node tests\e2e\mes-edhr-qa-menu-static.spec.js`、`node tests\e2e\mes-process-pool-team-leader-static.spec.js`。
- 运行 `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py`。
- 运行 `pnpm ts:check` 和 `git diff --check -- <task-owned paths>`。
- 运行前端与数据库交付证据校验脚本。
- 本地 MySQL 迁移执行后，只读核对 `admin` 在 tenant 1 和 tenant 122 对 `900434/900436/900435` 均有有效可见绑定。
- 运行真实只读 Playwright E2E：登录 `芋道源码/admin`，验证 eDHR 菜单树、侧边栏可见文本、`生产组长`/`PQC组长` 独立页面 DOM、目标接口业务成功、eDHR 内部 tabs 无组长内容、MES 写请求为 0。

## Current Status

ready_for_closeout

- 实现、admin 本地菜单迁移、定向验证和真实只读 E2E 均已完成；最终提交/推送仍受共享工作区非本任务脏改与分支 ahead 状态阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，使用正式动态菜单、独立路由、锁定类型包装页和 eDHR 内部 tabs 清理表达入口边界。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：静态合同 PASS 与真实 E2E PASS 必须分开记录；本任务执行静态合同和 `pnpm ts:check`，未将静态合同冒充真实 Playwright。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：更新 `tests/e2e/*static.spec.js` 时按稳定文件/组件/路由标记断言，不依赖坐标或截图。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 组长页继续读取 `pqcItemDetails/itemResults` 项目级明细，不恢复固定 `length/appearance/seal/pressure` 或 legacy `pqcPieceValues` 作为权威事实。
