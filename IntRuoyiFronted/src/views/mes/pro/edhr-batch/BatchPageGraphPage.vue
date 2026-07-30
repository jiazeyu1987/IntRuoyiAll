<template>
  <ContentWrap>
    <section class="edhr-page-graph-page" data-edhr-page-graph>
      <EdhrBatchRecordTabs active-tab="pageGraph" />

      <header class="edhr-page-graph-page__header">
        <div>
          <p class="edhr-page-graph-page__eyebrow">页面关系图</p>
          <h2>批记录页面关系图</h2>
          <p class="edhr-page-graph-page__description">
            每个节点代表一个页面或业务入口，连线表达报工、记录本、工序池、FIFO、审核副本与正式批记录之间的数据关系；这里不是工艺路线流转关系图，不写回 MES 工艺路线配置。
          </p>
        </div>
      </header>

      <div class="edhr-page-graph-page__stage" aria-label="批记录页面关系图">
        <article
          v-for="group in pageGroups"
          :key="group.key"
          class="edhr-page-graph-page__group"
        >
          <div class="edhr-page-graph-page__group-title">{{ group.title }}</div>
          <button
            v-for="node in group.nodes"
            :key="node.id"
            type="button"
            class="edhr-page-graph-page__node"
            :class="{
              'is-clickable': !node.isDisabled,
              'is-disabled': node.isDisabled
            }"
            data-edhr-page-node
            :data-edhr-page-node-id="node.id"
            :disabled="node.isDisabled"
            :aria-disabled="node.isDisabled"
            @click="handleOpenNode(node)"
          >
            <span class="edhr-page-graph-page__node-kind">{{ node.kind }}</span>
            <span class="edhr-page-graph-page__node-title">{{ node.title }}</span>
            <span class="edhr-page-graph-page__node-desc">{{ node.description }}</span>
            <span class="edhr-page-graph-page__node-status">
              {{ node.isDisabled ? '待接入' : '可进入' }}
            </span>
          </button>
        </article>
      </div>

      <div class="edhr-page-graph-page__edges" aria-label="页面关系说明">
        <div
          v-for="edge in pageEdges"
          :key="`${edge.from}-${edge.to}`"
          class="edhr-page-graph-page__edge"
          data-edhr-page-edge
          :data-edhr-page-edge-from="edge.from"
          :data-edhr-page-edge-to="edge.to"
        >
          <span>{{ nodeTitleById[edge.from] }}</span>
          <strong>→</strong>
          <span>{{ nodeTitleById[edge.to] }}</span>
          <em>{{ edge.label }}</em>
        </div>
      </div>
    </section>
  </ContentWrap>
</template>

<script setup lang="ts">
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchPageGraph' })

type PageNode = {
  id: string
  title: string
  description: string
  kind: string
  route?: string
  isDisabled: boolean
}

type PageNodeGroup = {
  key: string
  title: string
  nodes: PageNode[]
}

type PageEdge = {
  from: string
  to: string
  label: string
}

const router = useRouter()

const pageGroups: PageNodeGroup[] = [
  {
    key: 'settings',
    title: '配置与生产工单',
    nodes: [
      {
        id: 'mes-settings',
        title: 'MES工序/班组设置',
        description: '维护账号可切换工序、员工绑定关系和页面模板关系。',
        kind: '配置页',
        isDisabled: true
      },
      {
        id: 'work-order',
        title: '生产工单',
        description: '生产工单按计划开始时间进入 FIFO 满足顺序。',
        kind: '业务入口',
        isDisabled: true
      }
    ]
  },
  {
    key: 'frontline',
    title: '一线报工与质检填写',
    nodes: [
      {
        id: 'production-fill',
        title: '生产填写',
        description: '一线员工从报工入口提交输入数量、设备参数、输出数量和损耗数量。',
        kind: '报工页',
        route: '/mes/pro/feedback/edhr-batch-production-fill',
        isDisabled: false
      },
      {
        id: 'pqc-fill',
        title: 'PQC填写',
        description: 'PQC 简化录入检验结果、检验数量和损耗数量。',
        kind: '质检页',
        route: '/mes/pro/feedback/edhr-batch-pqc-fill',
        isDisabled: false
      }
    ]
  },
  {
    key: 'pool',
    title: '资源池与审核准备',
    nodes: [
      {
        id: 'process-pool',
        title: '工序池',
        description: '接收原始报工和记录本事件，作为订单取数资源池。',
        kind: '数据池',
        isDisabled: true
      },
      {
        id: 'fifo-allocation',
        title: 'FIFO分配',
        description: '按生产工单计划开始时间排序，先排的工单先满足。',
        kind: '分配逻辑',
        isDisabled: true
      },
      {
        id: 'team-lead-review',
        title: '班组长复核',
        description: '复核一线原始数据、员工切换和异常记录。',
        kind: '复核页',
        isDisabled: true
      },
      {
        id: 'review-copy',
        title: 'EDHR审核副本',
        description: '生成最接近限制范围的审核副本，原始值和修正值同时保留。',
        kind: '审核页',
        route: '/mes/pro/process-pool/review-copy',
        isDisabled: false
      },
      {
        id: 'event-revision',
        title: '原始记录修改',
        description: '原始记录提交后允许修改，但必须保留修改日志。',
        kind: '日志页',
        route: '/mes/pro/process-pool/event-revision',
        isDisabled: false
      }
    ]
  },
  {
    key: 'record',
    title: '正式批记录与归档',
    nodes: [
      {
        id: 'formal-record',
        title: '正式批记录',
        description: '按工序设置绑定的正式批记录表单查看、填写和审核。',
        kind: '批记录页',
        route: '/mes/pro/feedback/edhr-batch-execution',
        isDisabled: false
      },
      {
        id: 'history-record',
        title: '历史批记录',
        description: '查看已形成的历史批记录和追溯记录。',
        kind: '查询页',
        route: '/mes/pro/feedback/edhr-batch-history',
        isDisabled: false
      },
      {
        id: 'archive',
        title: '归档',
        description: '记录审核完成后的归档状态和追溯入口。',
        kind: '归档页',
        isDisabled: true
      }
    ]
  }
]

const pageEdges: PageEdge[] = [
  { from: 'mes-settings', to: 'production-fill', label: '控制工序和员工可切换范围' },
  { from: 'mes-settings', to: 'pqc-fill', label: '控制 PQC 模板和绑定员工' },
  { from: 'work-order', to: 'fifo-allocation', label: '提供工单计划开始时间' },
  { from: 'production-fill', to: 'process-pool', label: '提交报工和批记录原始事件' },
  { from: 'pqc-fill', to: 'process-pool', label: '提交质检原始事件' },
  { from: 'process-pool', to: 'fifo-allocation', label: '资源池按先进先出满足工单' },
  { from: 'process-pool', to: 'team-lead-review', label: '异常与修改日志进入复核' },
  { from: 'fifo-allocation', to: 'review-copy', label: '审核副本按限制范围修正超限值' },
  { from: 'review-copy', to: 'formal-record', label: '形成可审核的正式批记录视图' },
  { from: 'formal-record', to: 'history-record', label: '审核完成后进入历史查询' },
  { from: 'history-record', to: 'archive', label: '最终归档与追溯' }
]

const nodeTitleById = pageGroups.reduce<Record<string, string>>((result, group) => {
  for (const node of group.nodes) {
    result[node.id] = node.title
  }
  return result
}, {})

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

.edhr-page-graph-page__stage {
  display: grid;
  grid-template-columns: repeat(4, minmax(210px, 1fr));
  gap: 14px;
  align-items: stretch;
}

.edhr-page-graph-page__group {
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  padding: 14px;
}

.edhr-page-graph-page__group-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.edhr-page-graph-page__node {
  width: 100%;
  min-height: 128px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 12px;
  border: 1px solid #cbd8eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
  text-align: left;
  color: #172033;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.edhr-page-graph-page__node:last-child {
  margin-bottom: 0;
}

.edhr-page-graph-page__node.is-clickable {
  cursor: pointer;
}

.edhr-page-graph-page__node.is-clickable:hover {
  border-color: #3367d6;
  box-shadow: 0 8px 22px rgba(51, 103, 214, 0.14);
  transform: translateY(-1px);
}

.edhr-page-graph-page__node.is-disabled {
  cursor: not-allowed;
  color: #6b7585;
  background: #f3f6fa;
}

.edhr-page-graph-page__node-kind {
  color: #3367d6;
  font-size: 12px;
  font-weight: 700;
}

.edhr-page-graph-page__node-title {
  color: inherit;
  font-size: 16px;
  font-weight: 700;
}

.edhr-page-graph-page__node-desc {
  flex: 1;
  color: #526070;
  font-size: 13px;
  line-height: 1.55;
}

.edhr-page-graph-page__node-status {
  width: fit-content;
  border-radius: 999px;
  background: #eef4ff;
  padding: 3px 9px;
  color: #2954b4;
  font-size: 12px;
  font-weight: 700;
}

.edhr-page-graph-page__node.is-disabled .edhr-page-graph-page__node-status {
  background: #e5e9f0;
  color: #667085;
}

.edhr-page-graph-page__edges {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}

.edhr-page-graph-page__edge {
  display: grid;
  grid-template-columns: minmax(92px, auto) 20px minmax(92px, auto) 1fr;
  gap: 8px;
  align-items: center;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fbfcff;
  padding: 10px 12px;
  color: #172033;
  font-size: 13px;
}

.edhr-page-graph-page__edge strong {
  color: #3367d6;
  text-align: center;
}

.edhr-page-graph-page__edge em {
  color: #667085;
  font-style: normal;
}

@media (max-width: 1280px) {
  .edhr-page-graph-page__stage {
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .edhr-page-graph-page__stage,
  .edhr-page-graph-page__edges {
    grid-template-columns: 1fr;
  }

  .edhr-page-graph-page__edge {
    grid-template-columns: 1fr 20px 1fr;
  }

  .edhr-page-graph-page__edge em {
    grid-column: 1 / -1;
  }
}
</style>
