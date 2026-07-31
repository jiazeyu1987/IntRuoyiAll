# 修复 Git 443 端口无法推送

## Task Goal

诊断并修复当前仓库通过 GitHub HTTPS remote 推送时 443 端口不可用的问题，优先定位 Git 配置、代理、凭据或网络前置条件，不引入降级或绕过。

## Milestones

- [x] 记录当前 Git 分支、remote、工作区状态和适用门禁
- [x] 诊断 HTTPS 443 连通性、Git 代理配置和推送失败原因
- [ ] 执行最小范围修复并验证 remote 可访问
- [x] 记录验证证据、剩余阻塞和最终状态

## Expected Verification

- `git status --short --branch`
- `git remote -v`
- `git ls-remote origin HEAD`
- 如不涉及新增提交，使用只读/空推送验证网络连通性，避免混入既有脏改动

## Current Status

blocked

## Applicable Gates

- PowerShell / Git 编排：禁止使用 `&&`；Git 推送诊断前确认分支、remote、工作区状态和凭据/网络 blocker。
- 任务收尾：完成后记录验证证据；未能推送或缺凭据/网络时不得标记 completed。
- GitHub 推送：推送前需确认 GitHub HTTPS 或 SSH 认证链路可用；网络、凭据或代理不可用时必须 fail fast。

## Blocker

- GitHub HTTPS 直连 `github.com:443` 超时或连接失败。
- Git 全局配置 `http.https://github.com.proxy=http://127.0.0.1:7890`，但本机 `127.0.0.1:7890` 没有代理服务监听。
- FlClash 已启动，配置中 `mixed-port: 7890`，但核心仍未监听 7890，Git 通过代理继续失败。
- `ssh.github.com:443` 和 `github.com:22` 网络可达，但当前 `C:\Users\BJB110\.ssh\id_rsa` 未被 GitHub 接受，SSH 推送会失败 `Permission denied (publickey)`。

## Required User Action

- 打开 FlClash，确认已选择可用节点并开启代理核心，使 `127.0.0.1:7890` 开始监听；或
- 将 `C:\Users\BJB110\.ssh\id_rsa.pub` 添加到 GitHub 账号后，允许我改为 SSH 443 推送配置。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先定位 Git 443 推送失败根因，再做最小配置修复。
- `是否存在临时补丁或绕过`：否。
