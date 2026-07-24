# 删除 ERP / 分贝通未提交改动

## Task Goal
按用户要求删除当前工作区中 ERP / 分贝通相关未提交改动，保留已提交的排产修复结果和其他非目标文件。

## Current Status
completed

## Milestones
1. [x] 识别 ERP / 分贝通未提交改动范围。
2. [x] 仅删除目标未提交改动。
3. [x] 验证目标改动已清理且不影响已提交排产修复。

## Expected Verification
- git status --short 不再显示 ERP / 分贝通相关未提交路径。
- 不回退已提交的排产修复提交。

## Final Verification
- `git status --short -- <ERP/Fenbeitong target paths>`：PASS，目标路径已清理。
- 剩余后端工作区改动仅为本任务记录文件。

## 经验门禁
- PowerShell：显式 UTF-8；不使用 &&。
- 删除操作：只处理用户明确要求的 ERP / 分贝通相关未提交改动，不触碰已提交排产修复。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按 Git 工作区目标路径精确清理。
- 是否存在临时补丁或绕过：否。
