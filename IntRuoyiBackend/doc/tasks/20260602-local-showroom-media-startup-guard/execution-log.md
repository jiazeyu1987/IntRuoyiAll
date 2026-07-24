# Execution Log

## Bug

本机后端启动脚本只负责启动进程，不会在启动后真实读取展厅图片和语音直链。若 E2E 或本机配置让运行时桶与展厅媒体对象所在桶不一致，脚本仍可能报告启动成功，问题会拖到页面访问时才暴露。

## Expected

本机后端启动脚本应在后端健康检查通过后，用真实 `/admin-api/infra/file/28/get/showroom/**` 路径读取图片和语音样本；图片必须返回 `image/*`，语音必须返回 `audio/*`。缺少样本或媒体类型不匹配时必须 fail fast。

## Reproduction

- 静态检查当前 `script/deploy/restart-int-ruoyi-local.ps1`，确认缺少 `Assert-ShowroomMediaReadbackReady` 这类启动后读回守卫。
- 新增 `script/tests/test_runtime_control_scripts.py` 测试，要求启动脚本包含展厅图片/语音读回守卫。

## Root Cause

启动脚本缺少后端启动后的业务读回门禁，只验证了进程启动/依赖存在，未验证运行时真实文件读取路径与 MinIO 对象是否一致。

## BDD

BDD: 本机后端启动必须验证展厅图片读回 -> Given 本机后端启动脚本完成 Java 进程启动；When 脚本读取 `infra_file` 中 config 28 的展厅图片样本直链；Then 响应必须是 `image/*`，否则启动任务失败。

BDD: 本机后端启动必须验证展厅语音读回 -> Given 本机后端启动脚本完成 Java 进程启动；When 脚本读取 `infra_file` 中 config 28 的展厅语音样本直链；Then 响应必须是 `audio/*`，否则启动任务失败。

BDD: 缺少展厅媒体样本必须失败 -> Given 本机数据库缺少 config 28 的展厅图片或语音样本；When 启动脚本执行展厅媒体守卫；Then 脚本必须 fail fast，说明缺少的样本类型和影响。

## TDD Evidence

- STATUS: task-created -> 已建立本机展厅媒体启动守卫任务，下一步写 RED 测试。

## Verification

- 待记录。

## Blockers

- 无。
