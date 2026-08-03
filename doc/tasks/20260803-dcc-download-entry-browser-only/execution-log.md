# Execution Log

## User Intent

- 用户基于截图要求：“下载只保留红框里的入口”。
- 红框入口为文控受控浏览列表操作列中的“下载”按钮。

## BDD Scenarios

- BDD: 受控文件下载入口只保留列表行按钮 -> Given 用户在受控浏览列表查看文件行 When 文件具备下载权限 Then 操作列保留“下载”按钮并调用现有下载逻辑。
- BDD: 详情页不再提供直接下载入口 -> Given 用户从追溯/详情或预览态查看受控文件 When 页面渲染操作区 Then 页面不显示“下载当前受控副本”或“下载受控文件”按钮。

## Command Log

- Read frontend rules: `Get-Content docs/frontend-development.md -Encoding utf8`。
- Read closeout rules: `Get-Content docs/task-closeout-rules.md -Encoding utf8`。
- Read PowerShell encoding rules: `Get-Content docs/powershell-encoding.md -Encoding utf8`。
- Read frontend feature skill contract: `Get-Content C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md -Encoding utf8` and `references/frontend-contract.md`。

## Current Notes

- 受控浏览列表操作列中已有目标“下载”入口。
- 详情页存在两个直接下载按钮和对应 `openDownload/downloadLoading` 逻辑，需移除。
- Experience gate: 命中截图按钮入口变更、最小静态契约隔离和 DCC 受控浏览只读边界；本次不新增 fallback、不调整权限和 API。
