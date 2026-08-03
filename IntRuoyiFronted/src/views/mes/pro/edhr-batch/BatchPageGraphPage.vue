<template>
  <ContentWrap>
    <section class="edhr-page-graph-page" data-edhr-page-graph>
      <EdhrBatchRecordTabs active-tab="pageGraph" />

      <header class="edhr-page-graph-page__header">
        <div>
          <p class="edhr-page-graph-page__eyebrow">页面关系图</p>
          <h2>批记录页面关系图</h2>
          <p class="edhr-page-graph-page__description">
            每个节点代表一个页面或业务入口，箭头表达工序开始、批记录表单、表单槽位、报工、工序池、FIFO、审核副本与正式批记录之间的职责边界；这里不是工艺路线流转关系图，不写回 MES 工艺路线配置。
          </p>
        </div>
      </header>

      <div class="edhr-page-graph-page__viewport">
        <div class="edhr-page-graph-page__canvas" aria-label="批记录页面关系图">
          <div class="edhr-page-graph-page__lane-labels" aria-hidden="true">
            <span
              v-for="lane in flowLanes"
              :key="lane.key"
              class="edhr-page-graph-page__lane-label"
              :style="resolveLaneStyle(lane)"
            >
              {{ lane.title }}
            </span>
          </div>

          <VueFlow
            :nodes="flowNodes"
            :edges="flowEdges"
            class="edhr-page-graph-page__flow"
            :connection-mode="ConnectionMode.Strict"
            :default-edge-options="defaultEdgeOptions"
            :delete-key-code="null"
            :edges-connectable="false"
            :edges-updatable="false"
            :fit-view-on-init="true"
            :max-zoom="1.1"
            :min-zoom="0.45"
            :nodes-connectable="false"
            :nodes-draggable="false"
            :nodes-focusable="true"
            :pan-on-drag="false"
            :prevent-scrolling="true"
          >
            <template #node-edhr-page="{ data }">
              <button
                type="button"
                class="edhr-page-graph-page__node"
                :class="{
                  'is-clickable': !data.pageNode.isDisabled,
                  'is-disabled': data.pageNode.isDisabled,
                  [`is-${data.pageNode.tone}`]: true
                }"
                data-edhr-page-node
                :data-edhr-page-node-id="data.pageNode.id"
                :disabled="data.pageNode.isDisabled"
                :aria-disabled="data.pageNode.isDisabled"
                @click="handleOpenNode(data.pageNode)"
              >
                <span class="edhr-page-graph-page__node-sort">{{ data.pageNode.order }}</span>
                <span class="edhr-page-graph-page__node-main">
                  <span class="edhr-page-graph-page__node-title">{{ data.pageNode.title }}</span>
                  <span class="edhr-page-graph-page__node-kind">{{ data.pageNode.kind }}</span>
                </span>
                <span class="edhr-page-graph-page__node-desc">{{ data.pageNode.description }}</span>
                <span class="edhr-page-graph-page__node-status">
                  {{ data.pageNode.isDisabled ? '待接入' : '可进入' }}
                </span>

                <Handle
                  id="target-left"
                  class="edhr-page-graph-page__handle is-in is-left"
                  type="target"
                  :position="Position.Left"
                />
                <Handle
                  id="target-top"
                  class="edhr-page-graph-page__handle is-in is-top"
                  type="target"
                  :position="Position.Top"
                />
                <Handle
                  id="source-right"
                  class="edhr-page-graph-page__handle is-out is-right"
                  type="source"
                  :position="Position.Right"
                />
                <Handle
                  id="source-bottom"
                  class="edhr-page-graph-page__handle is-out is-bottom"
                  type="source"
                  :position="Position.Bottom"
                />
              </button>
            </template>
          </VueFlow>

          <div class="edhr-page-graph-page__edge-registry" aria-hidden="true">
            <span
              v-for="edge in pageEdges"
              :key="`${edge.from}-${edge.to}`"
              class="edhr-page-graph-page__edge-registry-item"
              data-edhr-page-edge
              :data-edhr-page-edge-from="edge.from"
              :data-edhr-page-edge-to="edge.to"
            >
              {{ edge.label }}
            </span>
          </div>
        </div>
      </div>
    </section>
  </ContentWrap>
</template>

<script setup lang="ts">
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import {
  ConnectionMode,
  Handle,
  MarkerType,
  Position,
  VueFlow,
  type Edge,
  type Node
} from '@vue-flow/core'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchPageGraph' })

type PageNodeTone = 'setup' | 'input' | 'pool' | 'review' | 'record'

type PageNode = {
  id: string
  order: string
  title: string
  description: string
  kind: string
  route?: string
  isDisabled: boolean
  x: number
  y: number
  tone: PageNodeTone
}

type PageEdge = {
  from: string
  to: string
  label: string
  sourceHandle?: string
  targetHandle?: string
}

type FlowLane = {
  key: string
  title: string
  x: number
}

const router = useRouter()

const flowLanes: FlowLane[] = [
  { key: 'setup', title: '配置 / 工单', x: 156 },
  { key: 'frontline', title: '一线填写', x: 398 },
  { key: 'pool', title: '资源池 / FIFO', x: 628 },
  { key: 'review', title: '复核 / 审核副本', x: 858 },
  { key: 'record', title: '正式批记录 / 归档', x: 1088 }
]

const graphNodes: PageNode[] = [
  {
    id: 'route-start',
    order: '01',
    title: '工序开始',
    description: '只决定开始节点动作、上传人和附件责任，不提供需要展示或填写的表单内容。',
    kind: '开始节点配置',
    isDisabled: true,
    x: 70,
    y: 82,
    tone: 'setup'
  },
  {
    id: 'batch-record-form-config',
    order: '02',
    title: '批记录表单',
    description: '只来自工序设置逐工序正式绑定，驱动正式生产批记录查看、填写和审核。',
    kind: '正式绑定配置',
    isDisabled: true,
    x: 70,
    y: 255,
    tone: 'setup'
  },
  {
    id: 'form-slot-binding',
    order: '03',
    title: '表单槽位',
    description: '只按 formBindings 承载补充特殊表单或动态表单中心模板，不得替代正式批记录。',
    kind: '补充表单配置',
    isDisabled: true,
    x: 70,
    y: 428,
    tone: 'setup'
  },
  {
    id: 'work-order',
    order: '04',
    title: '生产工单',
    description: '生产工单按计划开始时间进入 FIFO 满足顺序。',
    kind: '业务入口',
    isDisabled: true,
    x: 70,
    y: 625,
    tone: 'setup'
  },
  {
    id: 'production-fill',
    order: '05',
    title: '生产填写',
    description: '一线员工从报工入口提交数量、设备参数和正式批记录或补充槽位填写动作。',
    kind: '报工页',
    route: '/mes/pro/feedback/edhr-batch-production-fill',
    isDisabled: false,
    x: 312,
    y: 135,
    tone: 'input'
  },
  {
    id: 'pqc-fill',
    order: '06',
    title: 'PQC填写',
    description: 'PQC 简化录入检验结果、检验数量和损耗数量。',
    kind: '质检页',
    route: '/mes/pro/feedback/edhr-batch-pqc-fill',
    isDisabled: false,
    x: 312,
    y: 485,
    tone: 'input'
  },
  {
    id: 'process-pool',
    order: '07',
    title: '工序池',
    description: '接收原始报工和记录本事件，作为订单取数资源池。',
    kind: '数据池',
    isDisabled: true,
    x: 542,
    y: 282,
    tone: 'pool'
  },
  {
    id: 'fifo-allocation',
    order: '08',
    title: 'FIFO分配',
    description: '按生产工单计划开始时间排序，先排的工单先满足。',
    kind: '分配逻辑',
    isDisabled: true,
    x: 542,
    y: 585,
    tone: 'pool'
  },
  {
    id: 'team-lead-review',
    order: '09',
    title: '班组长复核',
    description: '生产组长和 PQC 组长复核一线原始数据、员工切换和异常记录。',
    kind: '复核页',
    route: '/mes/pro/feedback/edhr-batch-team-leader',
    isDisabled: false,
    x: 772,
    y: 122,
    tone: 'review'
  },
  {
    id: 'review-copy',
    order: '10',
    title: 'EDHR审核副本',
    description: '生成最接近限制范围的审核副本，原始值和修正值同时保留。',
    kind: '审核页',
    route: '/mes/pro/process-pool/review-copy',
    isDisabled: false,
    x: 772,
    y: 345,
    tone: 'review'
  },
  {
    id: 'event-revision',
    order: '11',
    title: '原始记录修改',
    description: '原始记录提交后允许修改，但必须保留修改日志。',
    kind: '日志页',
    route: '/mes/pro/process-pool/event-revision',
    isDisabled: false,
    x: 772,
    y: 595,
    tone: 'review'
  },
  {
    id: 'formal-record',
    order: '12',
    title: '正式批记录',
    description: '按逐工序正式批记录表单绑定查看、打开、填写和形成生产批记录。',
    kind: '批记录页',
    route: '/mes/pro/feedback/edhr-batch-execution',
    isDisabled: false,
    x: 1002,
    y: 245,
    tone: 'record'
  },
  {
    id: 'archive',
    order: '13',
    title: '归档',
    description: '记录审核完成后的归档状态，历史明细从表单追溯进入。',
    kind: '归档页',
    isDisabled: true,
    x: 1002,
    y: 455,
    tone: 'record'
  }
]

const pageEdges: PageEdge[] = [
  { from: 'route-start', to: 'production-fill', label: '仅决定开始节点动作和附件责任' },
  { from: 'batch-record-form-config', to: 'formal-record', label: '按工序设置逐工序正式绑定' },
  { from: 'form-slot-binding', to: 'production-fill', label: '仅提供补充表单槽位' },
  { from: 'work-order', to: 'fifo-allocation', label: '提供工单计划开始时间' },
  { from: 'production-fill', to: 'process-pool', label: '提交报工和批记录原始事件' },
  { from: 'pqc-fill', to: 'process-pool', label: '提交质检原始事件' },
  {
    from: 'process-pool',
    to: 'fifo-allocation',
    label: '资源池按先进先出满足工单',
    sourceHandle: 'source-bottom',
    targetHandle: 'target-top'
  },
  { from: 'process-pool', to: 'team-lead-review', label: '异常与修改日志进入复核' },
  { from: 'fifo-allocation', to: 'review-copy', label: '审核副本按限制范围修正超限值' },
  { from: 'review-copy', to: 'formal-record', label: '形成可审核的正式批记录视图' },
  { from: 'formal-record', to: 'archive', label: '审核完成后进入归档与表单追溯' }
]

const defaultEdgeOptions = {
  type: 'smoothstep',
  animated: false,
  markerEnd: MarkerType.ArrowClosed,
  style: {
    stroke: '#1677ff',
    strokeWidth: 2.2
  }
}

const toFlowNode = (node: PageNode): Node => ({
  id: node.id,
  type: 'edhr-page',
  position: { x: node.x, y: node.y },
  data: { pageNode: node },
  draggable: false,
  selectable: false,
  connectable: false,
  width: 190,
  height: 92
})

const toFlowEdge = (edge: PageEdge): Edge => ({
  id: `${edge.from}-${edge.to}`,
  source: edge.from,
  target: edge.to,
  sourceHandle: edge.sourceHandle || 'source-right',
  targetHandle: edge.targetHandle || 'target-left',
  type: 'smoothstep',
  markerEnd: MarkerType.ArrowClosed,
  data: { pageEdge: edge },
  selectable: false,
  animated: false,
  style: {
    stroke: '#1677ff',
    strokeWidth: 2.2
  }
})

const flowNodes = computed(() => graphNodes.map((node) => toFlowNode(node)))
const flowEdges = computed(() => pageEdges.map((edge) => toFlowEdge(edge)))

const resolveLaneStyle = (lane: FlowLane) => ({
  left: `${lane.x}px`
})

const handleOpenNode = async (node: PageNode) => {
  if (node.isDisabled || !node.route) {
    return
  }
  await router.push({ path: node.route })
}
</script>

<style scoped>
.edhr-page-graph-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-page-graph-page__header {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 20px 24px;
}

.edhr-page-graph-page__eyebrow {
  margin: 0 0 6px;
  color: #3367d6;
  font-size: 13px;
  font-weight: 700;
}

.edhr-page-graph-page__header h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 700;
}

.edhr-page-graph-page__description {
  max-width: 980px;
  margin: 10px 0 0;
  color: #526070;
  font-size: 14px;
  line-height: 1.7;
}

.edhr-page-graph-page__viewport {
  overflow-x: auto;
  border: 1px solid #d7dfed;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-page-graph-page__canvas {
  position: relative;
  min-width: 1200px;
  height: 790px;
  overflow: hidden;
  background: #f7f9fc;
}

.edhr-page-graph-page__lane-labels {
  position: absolute;
  top: 18px;
  left: 0;
  z-index: 5;
  width: 100%;
  pointer-events: none;
}

.edhr-page-graph-page__lane-label {
  position: absolute;
  transform: translateX(-50%);
  border: 1px solid #cfd9e8;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  padding: 5px 12px;
  color: #3c4658;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgb(23 32 51 / 6%);
}

.edhr-page-graph-page__flow {
  width: 100%;
  height: 100%;
  background: linear-gradient(#edf1f6 1px, transparent 1px),
    linear-gradient(90deg, #edf1f6 1px, transparent 1px), #f7f9fc;
  background-size: 24px 24px;
}

.edhr-page-graph-page__node {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  grid-template-rows: auto auto auto;
  align-content: center;
  align-items: center;
  gap: 5px 9px;
  width: 190px;
  min-height: 92px;
  border: 1px solid #c9d6e8;
  border-radius: 8px;
  background: #ffffff;
  padding: 10px 12px;
  color: #172033;
  text-align: left;
  box-shadow: 0 8px 18px rgb(23 32 51 / 8%);
  cursor: default;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.edhr-page-graph-page__node.is-clickable {
  cursor: pointer;
}

.edhr-page-graph-page__node.is-clickable:hover {
  border-color: #3367d6;
  box-shadow: 0 14px 28px rgba(51, 103, 214, 0.18);
  transform: translateY(-2px);
}

.edhr-page-graph-page__node.is-disabled {
  color: #6b7585;
  background: #f1f5f9;
  box-shadow: none;
  cursor: not-allowed;
}

.edhr-page-graph-page__node-sort {
  grid-row: 1 / 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #eaf3ff;
  color: #1677ff;
  font-weight: 700;
}

.edhr-page-graph-page__node-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.edhr-page-graph-page__node-title {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-page-graph-page__node-kind {
  color: #3367d6;
  font-size: 12px;
  font-weight: 700;
}

.edhr-page-graph-page__node-desc {
  grid-column: 1 / 3;
  display: -webkit-box;
  overflow: hidden;
  color: #526070;
  font-size: 12px;
  line-height: 1.42;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.edhr-page-graph-page__node-status {
  grid-column: 1 / 3;
  width: fit-content;
  border-radius: 999px;
  background: #eef4ff;
  padding: 3px 9px;
  color: #2954b4;
  font-size: 12px;
  font-weight: 700;
}

.edhr-page-graph-page__node.is-input {
  border-color: #86d6b6;
}

.edhr-page-graph-page__node.is-input .edhr-page-graph-page__node-kind,
.edhr-page-graph-page__node.is-input .edhr-page-graph-page__node-sort {
  color: #168a5f;
}

.edhr-page-graph-page__node.is-input .edhr-page-graph-page__node-sort {
  background: #e8f8f1;
}

.edhr-page-graph-page__node.is-pool {
  border-color: #e8c27c;
}

.edhr-page-graph-page__node.is-pool .edhr-page-graph-page__node-kind,
.edhr-page-graph-page__node.is-pool .edhr-page-graph-page__node-sort {
  color: #9a5a0c;
}

.edhr-page-graph-page__node.is-pool .edhr-page-graph-page__node-sort {
  background: #fff7e6;
}

.edhr-page-graph-page__node.is-review {
  border-color: #c4b5fd;
}

.edhr-page-graph-page__node.is-review .edhr-page-graph-page__node-kind,
.edhr-page-graph-page__node.is-review .edhr-page-graph-page__node-sort {
  color: #6d28d9;
}

.edhr-page-graph-page__node.is-review .edhr-page-graph-page__node-sort {
  background: #f3edff;
}

.edhr-page-graph-page__node.is-record {
  border-color: #82d3c8;
}

.edhr-page-graph-page__node.is-record .edhr-page-graph-page__node-kind,
.edhr-page-graph-page__node.is-record .edhr-page-graph-page__node-sort {
  color: #0f766e;
}

.edhr-page-graph-page__node.is-record .edhr-page-graph-page__node-sort {
  background: #e7f7f5;
}

.edhr-page-graph-page__node.is-disabled .edhr-page-graph-page__node-status {
  background: #e2e8f0;
  color: #64748b;
}

.edhr-page-graph-page__handle {
  z-index: 3;
  box-sizing: border-box;
  width: 2px;
  height: 2px;
  border: 0;
  background: transparent;
  opacity: 0;
  pointer-events: none;
}

.edhr-page-graph-page__handle.is-left {
  left: -13px;
}

.edhr-page-graph-page__handle.is-right {
  right: -13px;
}

.edhr-page-graph-page__handle.is-top {
  top: -13px;
}

.edhr-page-graph-page__handle.is-bottom {
  bottom: -13px;
}

.edhr-page-graph-page__edge-registry {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

:deep(.vue-flow__edge-path) {
  stroke: #1677ff;
  stroke-width: 2.2;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path),
:deep(.vue-flow__edge:hover .vue-flow__edge-path) {
  stroke: #0f5fc2;
  stroke-width: 2.8;
}

:deep(.vue-flow__edge) {
  pointer-events: none;
}

:deep(.vue-flow__pane) {
  pointer-events: none;
}

:deep(.vue-flow__nodes) {
  pointer-events: none;
}

:deep(.vue-flow__node) {
  z-index: 4;
  pointer-events: auto;
}

:deep(.vue-flow__node *) {
  pointer-events: auto;
}

@media (max-width: 768px) {
  .edhr-page-graph-page__header {
    padding: 16px;
  }

  .edhr-page-graph-page__canvas {
    min-width: 1120px;
    height: 780px;
  }
}
</style>
