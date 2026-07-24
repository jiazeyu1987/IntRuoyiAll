import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom admin index renders dedicated workflow workbenches instead of summary placeholders', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /ApprovalTaskPanel/)
  assert.match(source, /AssignmentWorkbench/)
  assert.match(source, /DiscussionWorkbench/)
  assert.match(source, /NarrationWorkspace/)
  assert.match(source, /v-else-if="activeSection === 'approval'"/)
  assert.match(source, /v-else-if="activeSection === 'assignment'"/)
  assert.match(source, /v-else-if="activeSection === 'discussion'"/)
  assert.match(source, /v-else-if="activeSection === 'narration'"/)
  assert.doesNotMatch(source, /approval-route/)
  assert.doesNotMatch(source, /assignment-notify/)
  assert.doesNotMatch(source, /comment-anchor/)
  assert.doesNotMatch(source, /narration-workspace',\s*name:\s*'讲解工作台'/)
})

test('workflow workbench files exist inside the allowed write boundary', () => {
  for (const relativePath of [
    'src/views/showroom-admin/approval/ApprovalTaskPanel.vue',
    'src/views/showroom-admin/approval/contracts.ts',
    'src/views/showroom-admin/assignment/AssignmentWorkbench.vue',
    'src/views/showroom-admin/assignment/FieldAssignmentDialog.vue',
    'src/views/showroom-admin/assignment/contracts.ts',
    'src/views/showroom-admin/discussion/DiscussionWorkbench.vue',
    'src/views/showroom-admin/discussion/ProductDiscussionPanel.vue',
    'src/views/showroom-admin/discussion/contracts.ts',
    'src/views/showroom-admin/narration/NarrationWorkspace.vue',
    'src/views/showroom-admin/narration/contracts.ts'
  ]) {
    assert.ok(fs.existsSync(path.join(root, relativePath)), `${relativePath} should exist`)
  }
})

test('approval task panel uses real approval list detail and review actions', () => {
  const source = readText('src/views/showroom-admin/approval/ApprovalTaskPanel.vue')

  assert.match(source, /审批工作台/)
  assert.match(source, /审批队列/)
  assert.match(source, /差异明细/)
  assert.match(source, /签名留痕/)
  assert.match(source, /ShowroomAdminApi\.getApprovalPage/)
  assert.match(source, /ShowroomAdminApi\.getApproval/)
  assert.match(source, /ShowroomAdminApi\.supervisorApprove/)
  assert.match(source, /ShowroomAdminApi\.gaoxinApprove/)
  assert.match(source, /ShowroomAdminApi\.supervisorReject/)
  assert.match(source, /ShowroomAdminApi\.gaoxinReject/)
  assert.match(source, /reviewerUserId/)
  assert.match(source, /ShowroomApprovalSignatureDialog/)
  assert.match(source, /approvalComment/)
  assert.match(source, /请输入登录密码完成电子签名/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /demo/i)
})

test('approval contracts allow submitterDeptId to be null for no-dept skip-supervisor flow', () => {
  const source = readText('src/views/showroom-admin/approval/contracts.ts')

  assert.match(source, /submitterDeptId: number \| null/)
  assert.match(source, /submitterDeptId:\s*optionalNumber\(record\.submitterDeptId\)/)
  assert.doesNotMatch(source, /submitterDeptId:\s*expectNumber\(record\.submitterDeptId/)
})

test('product detail dialog keeps approval submit flow for non-publicity users and direct publish for publicity', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(source, /submitRoutePreview/)
  assert.match(source, /ShowroomAdminApi\.submitProduct/)
  assert.match(source, /ShowroomAdminApi\.publishProduct/)
  assert.match(source, /targetRevisionId/)
  assert.match(source, /fieldCodes = changedFieldCodes\.value/)
  assert.match(source, /submitterDeptId: props\.submitRoutePreview\.submitterDeptId/)
  assert.match(source, /supervisorUserId: props\.submitRoutePreview\.supervisorUserId/)
  assert.match(source, /moduleCode: 'product-detail'/)
  assert.match(source, /产品变更已提交审批/)
  assert.match(source, /保存并发布/)
  assert.match(source, /产品新版本已发布/)
  assert.match(source, /showroom_publicity/)
})

test('product detail dialog supports approval mode with shared signature dialog', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(source, /approvalChangeRequestId/)
  assert.match(source, /approvalDetail/)
  assert.match(source, /ShowroomApprovalSignatureDialog/)
  assert.match(source, /待审批变更/)
  assert.match(source, /approvalComment/)
  assert.match(source, /ShowroomAdminApi\.gaoxinReject/)
  assert.match(source, /ShowroomAdminApi\.supervisorReject/)
})

test('assignment workbench exposes list detail create and complete-and-submit flow', () => {
  const source = readText('src/views/showroom-admin/assignment/AssignmentWorkbench.vue')
  const dialogSource = readText('src/views/showroom-admin/assignment/FieldAssignmentDialog.vue')

  assert.match(source, /补充指派工作台/)
  assert.match(source, /指派记录/)
  assert.match(source, /自动提交/)
  assert.match(source, /审批人/)
  assert.match(source, /企宣角色/)
  assert.match(source, /\/showroom\/assignment\/page/)
  assert.match(source, /\/showroom\/assignment\/get/)
  assert.match(source, /ShowroomAdminApi\.completeAssignmentAndSubmit/)
  assert.match(dialogSource, /FieldAssignmentDialog/)
  assert.match(dialogSource, /ShowroomAdminApi\.createAssignment/)
  assert.match(dialogSource, /getSimpleUserList|userOptions/)
  assert.match(dialogSource, /targetType/)
  assert.match(dialogSource, /fieldCode/)
  assert.doesNotMatch(source, /高昕审批人/)
  assert.doesNotMatch(source, /gaoxinUserId/)
  assert.doesNotMatch(source, /站内信摘要/)
})

test('discussion workbench exposes thread list plus create reply and resolve actions', () => {
  const source = readText('src/views/showroom-admin/discussion/ProductDiscussionPanel.vue')
  const workbenchSource = readText('src/views/showroom-admin/discussion/DiscussionWorkbench.vue')

  assert.match(workbenchSource, /产品讨论工作台/)
  assert.match(source, /讨论线程/)
  assert.match(source, /发起讨论/)
  assert.match(source, /回复/)
  assert.match(source, /已解决|解决讨论/)
  assert.match(source, /ShowroomAdminApi\.getProduct/)
  assert.match(source, /ShowroomAdminApi\.getProductCommentPage/)
  assert.match(source, /ShowroomAdminApi\.createProductComment/)
  assert.match(source, /\/showroom\/product-comment\/reply/)
  assert.match(source, /\/showroom\/product-comment\/resolve/)
  assert.match(source, /FIELD/)
  assert.match(source, /MODULE/)
  assert.match(source, /CHANGE_REQUEST/)
})

test('narration workspace exposes get draft audio submit and preview asset state', () => {
  const source = readText('src/views/showroom-admin/narration/NarrationWorkspace.vue')

  assert.match(source, /讲解工作台/)
  assert.match(source, /讲解稿/)
  assert.match(source, /讲解音频/)
  assert.match(source, /预览资产/)
  assert.match(source, /\/showroom\/narration\/get/)
  assert.match(source, /ShowroomAdminApi\.generateNarrationScript/)
  assert.match(source, /ShowroomAdminApi\.generateNarrationAudio/)
  assert.match(source, /ShowroomAdminApi\.saveNarrationDraft/)
  assert.match(source, /ShowroomAdminApi\.submitNarration/)
  assert.match(source, /ShowroomAdminApi\.supervisorApproveNarration/)
  assert.match(source, /ShowroomAdminApi\.gaoxinApproveNarration/)
  assert.match(source, /ShowroomAdminApi\.publishNarration/)
  assert.match(source, /提交审批/)
  assert.match(source, /主管审批通过/)
  assert.match(source, /企宣审批通过/)
  assert.match(source, /确认发布/)
  assert.doesNotMatch(source, /讲解资产已提交发布/)
  assert.match(source, /sourceRevisionId/)
  assert.match(source, /previewImageUrl/)
  assert.match(source, /ShowroomFrontstageApi/)
  assert.doesNotMatch(source, /空成功/)
})

test('narration workspace requires manual confirmation before submission is enabled', () => {
  const source = readText('src/views/showroom-admin/narration/NarrationWorkspace.vue')

  assert.match(source, /人工确认/)
  assert.match(source, /manualConfirmed/)
  assert.match(source, /提交审批/)
  assert.match(source, /!manualConfirmed|manualConfirmed === false/)
  assert.doesNotMatch(source, /提交发布/)
})

test('narration contracts keep approved state as pending publish instead of effective live success', () => {
  const source = readText('src/views/showroom-admin/narration/contracts.ts')

  assert.match(source, /APPROVED: '已批准待发布'/)
  assert.match(source, /if \(status === 'PUBLISHED'\)/)
  assert.match(source, /if \(status === 'APPROVED'\)/)
})
