# 20260727 删除当前分支 LFS 资产

## Request Summary And Source

- 请求来源：用户在当前任务中明确确认两个 LFS 文件均可删除。
- 请求内容：
  - 删除展厅 Win7 客户端 ZIP。
  - 删除展厅奖项导出回导 E2E 生成的 Excel。

## Current Baseline Reviewed

- 当前分支：`int_main`。
- 当前分支跟踪两个 LFS 对象，共约 310.9 MiB。
- Win7 ZIP 被后端 `/showroom/client-downloads/desktop-win7`、前端下载 API 和公司工作台按钮直接引用。
- Excel 由 `showroom-award-export-import-roundtrip-real.e2e.js` 在运行时生成并用于同文件回导。
- Android 客户端下载不在本次删除范围。

## Classification

- 需求变更：下线 Win7 桌面客户端下载能力。
- 测试资产治理：导出回导 Excel 改为临时运行产物，不再作为仓库资产保留。

## Impact

- 产品：公司工作台不再显示 Windows/Win7 客户端下载动作；Android 下载保留。
- 设计：删除已下线能力，不增加替代入口或 fallback。
- 数据：不修改数据库和业务数据。
- API：删除 `GET /showroom/client-downloads/desktop-win7`；继续保留 Android 下载接口。
- 测试：更新后端下载契约测试和前端静态契约；E2E Excel 在结束后清理。
- 发布：部署包不再包含 108.66 MiB Win7 ZIP。
- 运维：本次不重写 Git 历史；远端历史 LFS 配额不会因普通删除提交自动释放。

## Decision

- 决策：接受。
- 决策依据：用户已明确确认两个资产均可删除。

## Required Approvals

- 用户批准：已取得。
- 历史重写或 force push：未批准，也不在本次范围。

## Downstream Skill Reruns

- `behavior-driven-development`
- `backend-api-delivery`
- 前端静态契约验证
- `task-closeout-cleanup`
- `project-experience-consolidation`

## Blockers And Next Action

- 当前无业务前置阻塞。
- 下一步：先写失败契约，再删除入口、资源和 LFS 属性，完成回归验证。
