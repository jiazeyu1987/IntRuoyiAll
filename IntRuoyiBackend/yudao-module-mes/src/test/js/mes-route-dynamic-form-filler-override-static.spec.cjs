const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const flowGraphDesigner = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const backendService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java')
const publishProjectionService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')

assert.match(flowGraphDesigner, /默认使用表单填写人/, '前端未配置覆盖时必须提示继承批记录表单填写人。')
assert.match(flowGraphDesigner, /data-flow-action="clear-form-binding-filler-override"/, '前端必须提供清除覆盖并恢复默认填写人的操作。')
assert.match(flowGraphDesigner, /覆盖填写人来源/, '前端填写人选择必须表达为覆盖项。')
assert.doesNotMatch(flowGraphDesigner, /缺少填写人来源/, '前端不得阻断已选表单但未配置覆盖填写人的保存。')

assert.match(backendService, /StrUtil\.isBlank\(candidateSourceType\) && CollUtil\.isEmpty\(candidateSourceIds\)[\s\S]*return/, '后端保存缺少覆盖填写人时必须允许继承表单级填写人。')
assert.match(backendService, /StrUtil\.isBlank\(candidateSourceType\) \|\| candidateSourceIds\.size\(\) != 1/, '后端保存必须继续阻断不完整覆盖填写人。')
assert.match(publishProjectionService, /physicalDeleteByRouteProcessAndReport\(routeProcessId, formBindingKey\)[\s\S]*StrUtil\.isBlank\(candidateSourceType\) && candidateSourceIds\.isEmpty\(\)[\s\S]*return/, '发布投影缺少覆盖填写人时必须只清除路线级覆盖，继承表单级填写人。')

console.log('mes-route-dynamic-form-filler-override-static PASS')
