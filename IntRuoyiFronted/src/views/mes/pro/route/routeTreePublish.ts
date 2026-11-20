import { h, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ProRouteApi, type ProRouteVersionVO } from '@/api/mes/pro/route'
import { RouteTreeApi } from '@/api/mes/pro/route/tree'
import RouteTreePublishPreview from './RouteTreePublishPreview.vue'

export const submitRouteVersionWithTreeReview = async (version: ProRouteVersionVO) => {
  const route = await ProRouteApi.getRoute(version.routeId)
  if (route.nodeType !== 'GROUP') {
    if (route.parentId != null) {
      const parent = await ProRouteApi.getRoute(route.parentId)
      const [current, expected] = await Promise.all([
        ProRouteApi.getRouteProcessFlowGraph(version.routeId, version.id),
        ProRouteApi.getRouteProcessFlowGraph(route.parentId, parent.activeRouteVersionId)
      ])
      const ordered = (nodes: typeof current.nodes) => [...nodes].sort((a, b) => a.sort - b.sort)
      const ownNodes = ordered(current.nodes), parentNodes = ordered(expected.nodes)
      if (ownNodes.map(node => String(node.processId)).join(',') !== parentNodes.map(node => String(node.processId)).join(',')) {
        await ElMessageBox.confirm(
          `该流程的工序组成或顺序与「${parent.name}」不同。当前：${ownNodes.map(node => node.processName).join(' → ')}；父级：${parentNodes.map(node => node.processName).join(' → ')}。确认保留差异并提交？`,
          '工序一致性确认', { type: 'warning', confirmButtonText: '保留差异并提交', cancelButtonText: '返回修改' }
        )
      }
    }
    return ProRouteApi.submitAndPublishRouteCandidateVersion({ id: version.id })
  }
  const preview = await RouteTreeApi.preview(version.id)
  const reason = ref('')
  await ElMessageBox({
    title: `${preview.groupName} · 发布差异确认`,
    message: () => h(RouteTreePublishPreview, {
      preview, reason: reason.value, 'onUpdate:reason': (value: string) => { reason.value = value }
    }),
    customClass: 'route-tree-publish-message',
    showCancelButton: true,
    confirmButtonText: '确认覆盖并提交发布',
    showConfirmButton: !preview.blockers.length,
    cancelButtonText: '返回修改',
    closeOnClickModal: false,
    beforeClose: (action, _instance, done) => {
      if (action === 'confirm') {
        if (preview.blockers.length) { ElMessage.error('请先处理全部下属流程的冲突'); return }
        if (!reason.value.trim()) { ElMessage.error('请填写变更原因'); return }
      }
      done()
    }
  })
  return RouteTreeApi.submit({ id: version.id, fingerprint: preview.fingerprint, reason: reason.value.trim() })
}
