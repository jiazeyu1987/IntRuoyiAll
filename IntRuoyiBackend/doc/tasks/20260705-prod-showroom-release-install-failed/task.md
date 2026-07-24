completed

# 正式服展厅站点 release 安装失败修复

## 任务目标
- 修复正式服 `http://172.30.30.57:8083/` 提示 `SHOWROOM_RELEASE_INSTALL_FAILED` 的问题。
- 当前运行 release 为 `20260602T065841Z-be276b74dfa8-ca5704904844`，目标 release 为 `20260705T034529Z-be276b74dfa8-a93b25a4d7bf`。
- 只处理正式服展厅前台安装/切换链路，不回滚已成功的后台手动发布 release，不引入 fallback。

## 里程碑
1. 建立任务记录并读取正式服、发布、PowerShell 门禁。- 已完成
2. 定位安装失败错误码来源、正式服前台/后端日志、release 指针和安装目录状态。- 已完成
3. 按根因做最小修复并重新触发/验证前台安装。- 已完成
4. 访问 8083 复验目标 release 生效，记录证据。- 已完成
5. 收尾清理并提交任务记录。- 进行中

## 根因
- 后台手动发布本身成功，`release/current`、manifest 与文档接口均可正常返回目标 release。
- 正式服 8083 运行的 Website 前台 dist 落后，不支持当前 release manifest 中的 `award-detail` 文档类型。
- 浏览器诊断显示 8083 已成功请求 target release、manifest、`award-detail-*.json`，但页面报 `kind award-detail is not supported.`。

## 修复内容
- 使用本地 Website 当前源码构建新 dist；本地源码和测试已支持 `award-detail`。
- 备份正式服 `/opt/intruoyi/runtime/website/dist` 到 `/opt/intruoyi/runtime/website.dist-backup-20260705T081829Z-before-award-detail`。
- 部署新 Website dist 到正式服 `/opt/intruoyi/runtime/website/dist`。
- 由于 Docker bind mount 在目录级替换后仍引用旧 inode，重启 `intruoyi-website` 容器使其重新绑定当前 dist。

## 验证结果
- `npm test -- --run src/showroom-api.test.js -t "award"` 通过。
- `npm test -- --run src/showroom-api.test.js src/showroom-release-runtime.js src/showroom-release-fixture.js` 通过。
- `npm run build` 通过。
- 容器内新 JS 包含 `award-detail` 支持。
- Playwright 打开 `http://172.30.30.57:8083/` 后不再显示 `SHOWROOM_RELEASE_INSTALL_FAILED`、`更新失败` 或 `kind award-detail is not supported`，页面进入正式业务内容。
- `release/current` 返回目标 release `20260705T034529Z-be276b74dfa8-a93b25a4d7bf`。

## 经验门禁
- 正式服写入已由用户当前问题授权；只允许展厅 release 安装链路的必要修复。
- PowerShell 使用 UTF-8；远程脚本用 UTF-8 base64；不使用 `&&`。
- 发布/恢复不得用 mock、默认成功、静默跳过或自动降级掩盖失败。
- 不操作 `/mnt/nas` 根目录、挂载或 fstab，不清空共享盘。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，正式服前台 dist 与已发布 release 契约对齐，支持 `award-detail` 文档类型。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep
- doc/tasks/20260705-prod-showroom-release-install-failed/task.md
- doc/tasks/20260705-prod-showroom-release-install-failed/execution-log.md

## 当前状态
- 状态：已完成。
- 最终结果：正式服 8083 已恢复，目标 release 可安装并展示业务内容。