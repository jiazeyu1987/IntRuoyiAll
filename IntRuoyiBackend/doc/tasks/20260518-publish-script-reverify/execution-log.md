BDD: 发布脚本必须独立完成 MinIO 同步和公开下载配置 -> Given 测试服文件配置要求 `enablePublicAccess=true` 且脚本依赖容器内 `mc` 同步对象 / When 运行发布脚本 / Then 脚本必须自行补齐访问宿主 MinIO 的 host-gateway 映射并把目标桶设置为 download，而不是依赖人工补命令。
BDD: 最终版脚本必须端到端发布成功 -> Given 当前本地 IntRuoyi 后端、前端、数据库和 MinIO 数据均可用 / When 运行最终版 `publish-int-ruoyi-to-test.ps1` / Then 脚本必须自动完成构建、镜像导出、远端导入、数据库同步、MinIO 同步、容器启动和远端探活，且不需要人工续跑中间步骤。

- 2026-05-18 Asia/Shanghai: created this follow-up task package after confirming the previous publish task was already completed.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, the publish script still lacked the MinIO sync host-gateway mapping and bucket download grant.
- GREEN: updated `publish-int-ruoyi-to-test.ps1` to read runtime credentials, tolerate Windows OpenSSH socket-noise lines, resolve `.ps1` / `.cmd` command entrypoints, mirror the `yudao` bucket through `host.docker.internal`, and grant `mc anonymous set download dst/yudao`.
- RED: early end-to-end reruns exposed real script defects in order:
  - frontend test build blocked by `TtsTestPane.vue` self-closing native `audio` tag
  - OpenSSH stderr noise broke PowerShell native-command handling
  - `pnpm.ps1` needed explicit command resolution
  - MinIO shell command lost argument grouping under `Start-Process`
  - generated MySQL reset/import SQL mishandled backticks and `USE` statements
- GREEN: each blocker above was fixed in the script or the one blocking frontend file, then reverified.
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `3 passed`.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` -> PASS, the script completed the full test release and printed the final frontend and backend URLs.
- GREEN: remote script verification -> PASS, backend `http://127.0.0.1:48081/actuator/health` and frontend `http://127.0.0.1:8081/` both became ready during the successful script run.
