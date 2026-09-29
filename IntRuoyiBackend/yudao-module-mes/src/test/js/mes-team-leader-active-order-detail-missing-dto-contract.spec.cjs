const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const moduleRoot = path.resolve(__dirname, '../../..')
const detailPath = path.join(moduleRoot, 'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java')
const responsePath = path.join(moduleRoot, 'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java')
const controllerPath = path.join(moduleRoot, 'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')

test('active-order detail DTO exposes and maps formal source and nonconformance evidence', () => {
  const detail = fs.readFileSync(detailPath, 'utf8')
  const response = fs.readFileSync(responsePath, 'utf8')
  const controller = fs.readFileSync(controllerPath, 'utf8')

  assert.match(detail, /private\s+String\s+reviewMaterialsJson\s*;/,
    'operation fact must carry the materials JSON already projected by the service')
  assert.match(detail, /class\s+SourcePickListDocument\b/,
    'formal pick-list documents must have a typed detail projection')
  assert.match(detail, /private\s+List<SourcePickListDocument>\s+sourcePickListDocuments\s*=/,
    'material details must expose the formal source pick-list documents')
  assert.match(response, /private\s+String\s+reviewMaterialsJson\s*;/,
    'response contract must expose nonconformance material evidence')
  assert.match(response, /class\s+SourcePickListDocument\b/,
    'response contract must type the formal pick-list documents')
  assert.match(response, /private\s+List<SourcePickListDocument>\s+sourcePickListDocuments\s*;/,
    'response material contract must expose source pick-list documents')
  assert.match(controller, /\.setReviewMaterialsJson\(fact\.getReviewMaterialsJson\(\)\)/,
    'controller must preserve nonconformance material evidence in the response')
  assert.match(controller, /\.setSourcePickListDocuments\(material\.getSourcePickListDocuments\(\)\.stream\(\)/,
    'controller must preserve source pick-list document evidence in the response')
})
