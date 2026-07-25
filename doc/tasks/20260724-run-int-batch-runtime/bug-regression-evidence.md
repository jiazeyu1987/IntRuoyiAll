# Bug Regression Evidence

## Bug Summary

The backend could not be packaged for local runtime because existing source referenced missing runtime contracts.

## Reproduction

- `mvn.cmd -pl yudao-module-bpm -DskipTests compile` failed on missing BPM Form Center runtime package.
- `mvn.cmd -pl yudao-server -am -DskipTests package` failed on missing ERP Kingdee sync runtime package and MES initial-window fields.

## Root Cause

- Source code referenced runtime packages under BPM and ERP that were not present in the workspace.
- The root `.gitignore` rule `**/runtime/` ignores these package directories, so the restored files are not visible in normal `git status` unless force-added or `.gitignore` is corrected.

## Regression Verification

- BPM Form Center contract tests passed.
- ERP Kingdee job tests passed.
- MES module compile passed.
- Full backend package passed.

## Runtime Resolution

本机 `127.0.0.1:3306` 拒绝认证，但用户明确要求沿用 `E:\IntRuoyi` 的连接方式。后端已使用 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 启动，并在 `48041` 健康检查返回 `{"status":"UP"}`。
