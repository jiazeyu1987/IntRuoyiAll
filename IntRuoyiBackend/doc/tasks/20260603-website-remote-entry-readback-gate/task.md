# 20260603-website-remote-entry-readback-gate

## 任务目标

长期修复 Website 远端入口缓存与部署读回门禁：远端 `8083` 的 HTML 入口必须强制 `no-store`，部署脚本必须在发布后从真实 Website URL 读回入口 HTML、前端 bundle marker 和 scoped release current，失败时阻断发布。

## BDD 场景

BDD: Website 远端 HTML 入口不得被旧浏览器缓存 -> Given 发布包部署到远端 `intruoyi-website` Nginx / When 用户访问 `/`、`/index.html` 或 SPA fallback 路径 / Then 响应必须包含 `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`、`Pragma: no-cache`、`Expires: 0`，避免旧 Chrome 持续加载旧 runtime。

BDD: Website 部署后必须读回真实入口和 release -> Given `publish-int-ruoyi.ps1 -Mode deploy-release` 已重建远端 Website / When 脚本进入 HTTP readiness 阶段 / Then 必须从 `http://${ServerHost}:$WebsiteHostPort/` 读回 HTML、入口 JS、scope/cache marker 和 scoped release current；任一不匹配必须失败，不能报告部署成功。

## 里程碑

- [x] M1：创建任务记录并确认当前发布链路位置。
- [x] M2：为 Nginx 入口缓存头和部署 read-back gate 添加失败测试。
- [x] M3：修复 `website.nginx.conf` 与 `publish-int-ruoyi.ps1`。
- [x] M4：运行脚本测试、CI/CD 证据校验与真实发布包验证。
- [x] M5：记录完成状态，执行 task-closeout-cleanup 预览并单独提交。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "website_nginx or public_website" -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "entry_bundle" -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- Website-only 发布包在测试服部署并通过 read-back gate。
- 同一已测试发布包在正式服部署并通过 read-back gate。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。部署后读回失败必须阻断。
- 是否从根因和长期维护角度解决：是。通过发布模板与部署门禁固化，不依赖用户手动清缓存。
- 是否存在临时补丁或绕过：否。不通过手动清浏览器缓存或一次性远端改文件作为最终方案。

## Current Status

completed

## 最终结果

已在 `script/deploy/int-ruoyi-test/website.nginx.conf` 为 `/` 与 `/index.html` 增加 no-store 响应头。已在 `script/deploy/publish-int-ruoyi.ps1` 增加 `Assert-PublicWebsiteEntryReadback`，部署后读取真实 Website 入口、入口 JS bundle、缓存头、`yingtai-showroom`、`TEST`、`3221225472`，并拒绝旧 `1073741824` marker；随后继续校验 scoped release current。

发布包 `20260603_website_entry_readback_nostore` 已先部署到测试服并通过门禁，随后标记 tested，再用 `-RequireTested -ConfirmText PROD` 部署到正式服。

## 最终验证结果

- 测试服 `http://172.30.30.58:8083/`：入口 `/assets/index-B1lPB_BO.js`，no-store headers 与 bundle markers 验证通过。
- 正式服 `http://172.30.30.57:8083/`：`/` 与 `/index.html` 均返回 `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`、`Pragma: no-cache`、`Expires: 0`。
- 正式服 bundle `/assets/index-B1lPB_BO.js` 包含 `3221225472`、`yingtai-showroom`、`TEST`，不包含 `1073741824`。
- 正式服 scoped release current 保持 `20260602T065841Z-be276b74dfa8-ca5704904844`。
