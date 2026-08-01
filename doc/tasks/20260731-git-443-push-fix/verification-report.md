# Verification Report

## Result

blocked

## Evidence

- `git status --short --branch` -> 当前分支 `int_main`，本地领先 `origin/int_main` 10 个提交，存在既有未提交改动。
- `git remote -v` -> `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- `git config --show-origin --list` -> 全局 GitHub 专用代理为 `http://127.0.0.1:7890`。
- `Test-NetConnection 127.0.0.1 -Port 7890` -> `TcpTestSucceeded: False`。
- `git ls-remote origin HEAD` -> 失败，Git 无法通过 `127.0.0.1:7890` 连接 GitHub 443。
- `git -c http.https://github.com.proxy= ls-remote origin HEAD` -> 失败，GitHub HTTPS 直连 443 不可用。
- 启动 FlClash 后，`Get-NetTCPConnection` 仅发现 `FlClashHelperService` 监听 `127.0.0.1:47890`，未发现 `7890` 监听。
- `ssh -T -o BatchMode=yes -o ConnectTimeout=10 git@ssh.github.com -p 443` -> 失败，当前 SSH key 未被 GitHub 接受。

## Required Preconditions

- FlClash 代理核心必须正常监听 `127.0.0.1:7890`；或
- 当前机器的 SSH 公钥必须添加到 GitHub 后，改用 `ssh.github.com:443` 推送链路。
