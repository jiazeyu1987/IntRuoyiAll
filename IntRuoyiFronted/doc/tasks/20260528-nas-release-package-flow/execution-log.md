# 执行日志：NAS 发布包流转前端

BDD: 发布包动作展示 -> Given 用户进入运行控制台 / When 查看运维动作 / Then 页面显示构建发布包、部署发布包到测试服、标记测试通过、上线已验证发布包，不再显示“提升正式服”。

BDD: ReleaseTag 前端校验 -> Given 用户选择部署测试服、标记测试通过或上线正式服 / When ReleaseTag 为空并提交 / Then 前端提示先填写发布包编号，不发起 API 请求。

RED: `node tests\e2e\runtime-control-release-package-static.spec.js` -> FAIL，旧页面缺少 `build-release`、ReleaseTag 输入和“上线已验证发布包”文案。

GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: task-closeout-cleanup preview -> PASS，无删除项。
