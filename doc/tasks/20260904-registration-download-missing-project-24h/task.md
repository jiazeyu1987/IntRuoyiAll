# 注册证下载缺项目代码与 24 小时口径修正

## Task Goal

让注册证下载规则与验收文档保持一致：缺少项目代码的注册证文件也允许普通用户申请下载；下载授权有效期统一为 24 小时；文件命名在项目代码为空时用空字符串段并保留下划线分隔。

## Milestones

1. completed - 读取项目规则、worktree 限制、既有注册证下载任务证据和当前实现。
2. completed - 更新注册证下载 E2E 验收文档和静态契约测试口径。
3. completed - 融合到 `int_main` 后运行定向前端静态合同与后端单测。

## Expected Verification

- 静态检查确认下载 E2E 文档不再保留注册证下载授权“3 天”口径。
- 静态检查确认缺少项目代码不阻止“申请下载”，文件名使用空项目代码段。
- 后端定向测试确认 `DOWNLOAD_FILE` 申请项目代码可为空、授权有效期为 24 小时。

## Current Status

blocked - `int_main` 本地已再次通过注册证下载定向前端静态合同、DCC 后端下载/审批测试和 BPM 通知通道测试，并已纳入本地注册证下载融合提交。继续同步远端时 `git fetch origin` 和 `git push origin int_main` 均失败，错误为 GitHub HTTPS TLS connect unexpected EOF，因此无法同步最新远端代码，也暂不能标记为 completed。当前仍有 MES 生产组长相关未提交改动，未纳入本任务范围。

## 设计约束检查

- 工作目录：`E:\IntRuoyi`。
- 不做数据库写入、不调用接口替代前端验收动作。
- 本次只记录注册证下载口径修正和融合复验；MES 相关脏改动保持原样。
