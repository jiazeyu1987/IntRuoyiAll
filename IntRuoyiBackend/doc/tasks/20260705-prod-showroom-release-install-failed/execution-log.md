# 执行日志

## BDD / TDD
- BDD: 展厅前台安装新 release -> Given 后台手动发布生成 target release, When 用户访问正式服 8083, Then 前台应安装并运行 target release，不显示 `SHOWROOM_RELEASE_INSTALL_FAILED`。
- BDD: 安装失败必须暴露根因 -> Given 安装所需资源、manifest、目录或权限缺失, When 8083 尝试更新, Then 日志应记录明确错误，不允许静默继续旧版本。

## 门禁
- GREEN: experience-preflight -> PASS, 已读取 PowerShell、正式服访问与发布备份恢复门禁。

## RED / 定位
- RED: prod-frontstage-user-report -> FAIL, 用户访问 `http://172.30.30.57:8083/` 提示当前仍运行 `20260602T065841Z-be276b74dfa8-ca5704904844`，目标 `20260705T034529Z-be276b74dfa8-a93b25a4d7bf`，errorCode=`SHOWROOM_RELEASE_INSTALL_FAILED`。
- RED: prod-8083-runtime-diagnosis -> FAIL, 浏览器打开正式服 8083 后已获取 target release/current、manifest 与 award-detail 文档，但页面显示 `kind award-detail is not supported.`。
- Root cause: 正式服 8083 运行的 Website dist 为旧构建，不支持当前 release manifest 中的 `award-detail` 文档类型；本地 Website 源码和测试已包含 award-detail 支持。

## GREEN / REGRESSION
- GREEN: website-award-targeted-tests -> PASS, 本地 Website `src/showroom-api.test.js` 中 award 相关用例通过。
- GREEN: website-release-runtime-tests -> PASS, `npm test -- --run src/showroom-api.test.js src/showroom-release-runtime.js src/showroom-release-fixture.js` 通过，51 tests passed。
- GREEN: website-build -> PASS, 本地 Website `npm run build` 成功生成新 dist。
- GREEN: prod-website-dist-deploy -> PASS, 已备份正式服 `/opt/intruoyi/runtime/website/dist`，部署本地构建的新 Website dist；新 JS 包含 `award-detail` 支持。
- RED: prod-website-dist-swap-v1 -> FAIL, 目录级替换 `/opt/intruoyi/runtime/website/dist` 后 8083 返回 404；根因是 `intruoyi-website` Docker bind mount 仍引用旧目录 inode。
- GREEN: prod-website-container-restart -> PASS, 重启 `intruoyi-website` 后重新绑定当前 dist，8083 根路径恢复 HTTP 200，容器内 JS 包含 `award-detail` 支持。
- GREEN: prod-8083-post-deploy-browser-verify -> PASS, Playwright 打开正式服 8083 后不再出现 `SHOWROOM_RELEASE_INSTALL_FAILED` 或 `kind award-detail is not supported`，页面进入业务展厅内容。
- GREEN: prod-release-current-final -> PASS, `release/current` 返回目标 release `20260705T034529Z-be276b74dfa8-a93b25a4d7bf`。

## 收尾
- 待执行 task-closeout cleanup preview/apply，删除临时诊断脚本、截图、JSON、dist tar，仅保留 task.md / execution-log.md。