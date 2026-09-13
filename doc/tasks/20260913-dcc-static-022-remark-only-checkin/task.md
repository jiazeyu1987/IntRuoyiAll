# DCC-STATIC-022 Remark-Only Checkin

## Task Goal

修复 `docs/bugs/20260912-dcc-90-step-static-audit.md` 中 DCC-STATIC-022：后端已支持仅备注真实变化的检入，但正式受控文件浏览页检入弹窗强制上传新源件，且未把修改说明作为 `remark` 提交。

## Milestones

- [x] 读取项目规则、缺陷登记和受影响检入调用链。
- [x] 新增前端静态回归合同并取得 RED。
- [x] 最小化修复浏览页检入弹窗和提交载荷。
- [x] 执行定向静态验证并记录 GREEN。
- [x] 融合 DCC-STATIC-022 任务改动到 `int_main`。

## Expected Verification

- 运行新增 DCC-STATIC-022 前端静态合同，证明检入页面允许源文件或备注至少一项真实变化。
- 运行既有 `dcc-browser-checkout-static.spec.js`，证明原检出/检入入口合同未破坏。
- 按用户要求不执行 E2E、不启动/重启服务、不写数据库、不操作远程。

## Current Status

blocked

本任务代码、静态合同与缺陷登记已融合到 `E:\IntRuoyi` 的 `int_main` 工作区。提交被 `int_main` 既有非本任务未解决冲突阻塞，未执行 push。

## Design Constraints Check

- 仅处理 DCC-STATIC-022，不修改其它 DCC-STATIC 项。
- 不引入 fallback、静默成功、mock 成功或兼容补丁。
- 前端检入应沿用现有 API wrapper 和浏览页弹窗模式。
- 源件或允许变更的元数据至少一项真实变化才可提交；检入备注应作为 `remark` 传给后端形成元数据差异。
- 不执行 E2E、服务启动/重启、数据库写入或远程操作。
