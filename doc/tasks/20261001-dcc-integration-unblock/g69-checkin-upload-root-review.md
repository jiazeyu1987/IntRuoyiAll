# G69 检入上传入口复核

真实页面：原生撤回4043已取得本人锁30，上传修正PDF却在正式 upload-preview 的 CHECKIN 上下文校验被拒；前端正确阻止无上传票据的检入。后端定位到独立源文件上传校验未接受经过精确核验的原生撤回申请，正以正式multipart/controller/service-context做RED/GREEN。

前端只增加正式 response.data.msg 优先显示，保原未知错误提示与实际失败状态。2项有效RED后20项定向回归和lint通过；实际handler失败清票据、触发onError而不伪造成功，CHECKIN实际文件ID及参数不变。未改变接口或类型，按当前有限范围不重复完整构建。主任务in_progress，上传/检入/同B1重提真实闭环仍待后端修复和新包验收。

Root后端复核：3生产最小变更，内部只读校验复用G68真实取消历史及目标/前驱、本人及编制资格；独立CHECKIN上传只在WITHDRAWN时调用，原tenant/category/本人active exact-base lock及项目资格保留。有效RED正式multipart到1080000115；17项/4类回归全0，真实Controller→UploadService事务→Workflow context与原取消修正链覆盖，存储/票据是明示隔离端口；审计头、分类阶段、代理等fixture前置单列。无迁移、实际库写、状态泛放或GET回填。Root只读准确资产/类/收据SHA核通过，待新包实UI同文件再上传。
