# Verification Report

## Result

- 当前分支：`int_main`。
- 上游分支：`origin/int_main`。
- 远端同步前置：behind=0、ahead=20。
- 前后端候选文件：164 个，其中后端 110 个、前端 54 个。
- 暂存安全扫描：未发现凭据文件、运行日志、PID、构建目录、二进制归档或大于等于 10MB 的文件。

## Verification

- 分支运行端口门禁：PASS。
- 后端 `yudao-server` 全依赖模块编译打包：PASS。
- 前端 TypeScript 类型检查：PASS。
- 前端本地构建：PASS。

## Pending

- 创建提交。
- 检查提交后前后端残余改动。
- 扫描待推送历史中的大对象并推送 `origin/int_main`。
