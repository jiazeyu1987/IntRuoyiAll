<template>
  <el-button v-hasPermi="['mes:pro-process-pool-team-leader:query']" @click="open">领料提示</el-button>
  <el-dialog v-model="visible" title="活跃订单领料提示" width="1000px" :close-on-click-modal="false">
    <el-alert title="以下问题仅作记录，不阻止完工；原始领料数据保持不变。" type="info" :closable="false" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="my-12px" />
    <el-table v-else v-loading="loading" :data="rows" empty-text="暂无已记录的领料提示">
      <el-table-column prop="billNo" label="领料单号" min-width="155" />
      <el-table-column prop="materialCode" label="物料编码" min-width="170" />
      <el-table-column prop="lotNumber" label="原始批号" min-width="110" />
      <el-table-column prop="warehouse" label="原始仓库" min-width="110" />
      <el-table-column prop="quantity" label="原始实发数量" width="120" />
      <el-table-column prop="warnings" label="记录内容" min-width="230" />
    </el-table>
    <template #footer>
      <el-button v-if="error" :disabled="loading" @click="load">重试</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { getTeamLeaderActiveOrderTransferTrace } from '@/api/mes/pro/processpool/teamLeader'

const props = defineProps<{ activeOrderId: string }>()
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const rows = ref<Array<{ billNo?: string; materialCode?: string; lotNumber?: string; warehouse?: string; quantity: number | string | null; warnings: string }>>([])
const load = async () => {
  loading.value = true
  error.value = ''
  rows.value = []
  try {
    const traces = await getTeamLeaderActiveOrderTransferTrace(Number(props.activeOrderId))
    const notices: typeof rows.value = []
    for (const trace of traces) {
      if (trace.sourceObjectType !== 'ERP_PRODUCTION_PICK_LIST_ITEM' || trace.sourceType !== 'BATCH_TRACE') continue
      if (!trace.sourceSnapshotJson) throw new Error('领料追溯缺少来源记录')
      const snapshot = JSON.parse(trace.sourceSnapshotJson)
      if (!Array.isArray(snapshot.sourceWarnings)) throw new Error('领料提示记录格式错误')
      if (snapshot.sourceWarnings.length === 0) continue
      const item = snapshot.pickListItem
      if (!item) throw new Error('领料提示缺少原始明细')
      notices.push({ billNo: trace.sourceObjectCode, materialCode: item.materialNumber,
        lotNumber: item.lotNumber, warehouse: item.warehouseNumber, quantity: item.actualQuantity,
        warnings: snapshot.sourceWarnings.join('；') })
    }
    rows.value = notices
  } catch (e) {
    error.value = e instanceof Error ? e.message : '领料提示加载失败'
  } finally {
    loading.value = false
  }
}
const open = async () => { visible.value = true; await load() }
</script>
