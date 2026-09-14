# Verification Report

## Scope

- 使用新版识别前后端，通过真实页面 `MES 系统 / eDHR批记录 / 批记录表单` 导入 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`。
- 目标产品名称：`球囊扩张压力泵`。
- 目标版本：`V14.0`。

## Result

- PASS：本机前端 `http://127.0.0.1:8081` 可访问。
- PASS：本机后端 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- PASS：Playwright 真实页面上传 Word 并触发升版导入，页面显示 `V14.0`。
- PASS：只读页面会话 API 核验返回 15 条 `球囊扩张压力泵 / V14.0` 批记录表单。
- PASS：核验行均来自源文件 `批记录压力泵.doc`，状态为 `PENDING_APPROVAL`。

## Evidence

- 页面入口截图：`doc/tasks/20260726-batch-record-import-v14/artifacts/01-batch-record-form-page.png`
- V14 预检截图：`doc/tasks/20260726-batch-record-import-v14/artifacts/02-word-import-preflight.png`
- V14 列表截图：`doc/tasks/20260726-batch-record-import-v14/artifacts/03-v14-list-visible.png`
- 最终验证截图：`doc/tasks/20260726-batch-record-import-v14/artifacts/04-v14-final-verification.png`
- 只读核验数据：`doc/tasks/20260726-batch-record-import-v14/artifacts/v14-readonly-verification.json`

## Notes

- 初始自动化用模糊关键词 `压力泵` 命中相邻产品 `按压式球囊扩充压力泵`，预检目标为 `V4.0`，已判定为不满足目标。
- 后续选择逻辑改为精确匹配项目名称，属于通用下拉选择收敛方式，不基于表单名、工序名或文件名特例。
