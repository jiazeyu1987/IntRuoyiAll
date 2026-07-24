# Execution Log: DCC 上传 E2E 错误原因排查

BDD: 排查 E2E 过程中出现的系统内部错误来源 -> Given DCC 真实上传路径可以成功提交 / When 诊断脚本捕获页面 toast、控制台错误与失败请求 / Then 应明确区分错误是来自上传 API、其他后台请求，还是前端 dev-server/HMR 运行时噪声。

RED: 初始状态 -> FAIL，尚未执行带诊断信息的真实上传复跑，无法确认 `系统内部错误` toast 来源。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-e2e-error-cause-analysis run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-e2e-error-cause-analysis\scripts\diagnose-dcc-upload-e2e-error-cause.mjs` -> PASS

GREEN: 诊断结论
- 提交接口成功：`POST /admin-api/dcc/controlled-files/submit -> code=0`
- toast 来源：`GET /admin-api/dcc/controlled-files/upload-name-options?categoryId=9 -> code=500, message=系统内部错误`
- 后端日志堆栈：
  - `java.lang.NullPointerException`
  - `DccControlledFileQueryServiceImpl.listUploadNameOptions` 第 213 行
  - `currentVersionMap.get(master.getCurrentActiveControlledFileId())`

GREEN: 额外观察
- 本次诊断复跑没有再捕获 `@vite/client` `document is not defined`
- 因此当前可确认的用户可见错误根因，是 `upload-name-options` 的后端空指针，而不是上传提交接口失败
