import { h } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { DccProjectProductCreateReqVO, DccProjectReviewerConfiguration } from '@/api/dcc/controlledFile/projectProductRequests'
import { requireConfiguredReviewer, reviewerIdentity } from './project-reviewer'
import { validateAttributes } from '../../project-attributes/state'

interface LeaderChoice { id: string | number; nickname?: string; username?: string }
interface TemplateChoice { id: string | number; name: string; active: boolean }

export const confirmProjectProductApplication = async (
  payload: DccProjectProductCreateReqVO,
  leaders: LeaderChoice[],
  templates: TemplateChoice[],
  reviewer: DccProjectReviewerConfiguration,
  resubmission: boolean
): Promise<void> => {
  const captured = JSON.parse(JSON.stringify(payload)) as DccProjectProductCreateReqVO
  const configured = requireConfiguredReviewer(reviewer)
  const attributes = validateAttributes(captured.defaultAttributes)
  const leaderId = reviewerIdentity(captured.projectLeaderUserId)
  const templateId = reviewerIdentity(captured.folderTemplateId)
  const selectedLeaders = leaders.filter(row => reviewerIdentity(row.id) === leaderId)
  const selectedTemplates = templates.filter(row => reviewerIdentity(row.id) === templateId && row.active)
  if (selectedLeaders.length !== 1 || selectedTemplates.length !== 1)
    throw new Error('项目负责人或启用目录模板与本次申请不一致，请重新选择')
  const leaderName = selectedLeaders[0].nickname || selectedLeaders[0].username
  if (!leaderName?.trim() || !selectedTemplates[0].name?.trim())
    throw new Error('项目负责人或目录模板的正式名称缺失')
  const choices: Record<string, string> = { Y: '是', N: '否', NA: '不适用' }
  const markets: Record<string, string> = { NMPA: 'NMPA 国内', CE: 'CE 欧盟', FDA: 'FDA', MADSAP: 'MADSAP', OTHER: '其他', NA: '不适用' }
  const fields: [string, string][] = [
    ['项目名称', captured.projectName], ['项目代码', captured.projectCode],
    ['项目负责人', `${leaderName}（账号 #${leaderId}）`],
    ['产品编码', captured.productCode], ['产品名称', captured.productName], ['产品分类', captured.classification],
    ['存储文件夹模板', `${selectedTemplates[0].name}（#${templateId}）`],
    ['目标市场', attributes.targetMarkets.map(value => markets[value]).join('、')],
    ['其他市场说明', attributes.otherMarket || '未选择其他市场'],
    ['是否为注册人', choices[attributes.licenseHolder!]],
    ['是否为生产方', choices[attributes.actualManufacturer!]],
    ['文件转移', choices[attributes.documentTransfer!]],
    ['转移至', attributes.transferTo || '未选择转移'],
    ['审核人', `${configured.reviewerNickname}（${configured.reviewerUsername}，账号 #${configured.reviewerUserId}）`],
    ['备注', captured.remark || '未填写'],
    [resubmission ? '重提说明' : '新建申请原因', (resubmission ? captured.resubmissionReason : captured.creationReason) || '']
  ]
  await ElMessageBox.confirm(h('div', { 'data-testid': 'dcc-project-product-confirmation', style: { maxHeight: '65vh', overflowY: 'auto' } }, [
    h('p', '请核对本次项目、产品及默认属性，提交后进入审核。'),
    h('dl', fields.flatMap(([label, value]) => [h('dt', { class: 'font-600 mt-8px' }, label), h('dd', { class: 'm-0 break-all' }, value)]))
  ]), resubmission ? '核对重新提交的项目及产品申请' : '核对新建项目及产品申请', {
    confirmButtonText: '提交申请', cancelButtonText: '返回修改', closeOnClickModal: false
  })
}
