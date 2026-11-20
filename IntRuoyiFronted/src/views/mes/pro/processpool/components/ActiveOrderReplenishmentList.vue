<template>
  <el-button v-hasPermi="['mes:pro-process-pool-team-leader:query']" @click="open">
    补料单
  </el-button>
  <el-dialog v-model="visible" title="订单补料单" width="1000px" :close-on-click-modal="false">
    <el-alert
      title="以下为当前生产订单的补料记录。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="my-12px" />
    <el-table v-else v-loading="loading" :data="lines" empty-text="暂无补料单">
      <el-table-column prop="billNo" label="补料单号" min-width="145" />
      <el-table-column label="物料编码" min-width="170">
        <template #default="{ row }"><MaterialCodeLink :code="row.materialCode" /></template>
      </el-table-column>
      <el-table-column prop="materialName" label="物料名称" min-width="165" />
      <el-table-column prop="lotNumber" label="批号" min-width="120" />
      <el-table-column prop="actualQuantity" label="实补数量" width="100" />
      <el-table-column label="单据状态" width="110">
        <template #default="{ row }">{{ statusName(row.documentStatus) }}</template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button v-if="error" :disabled="loading" @click="load">重试</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MaterialCodeLink from '@/components/MaterialCodeLink.vue'
import { getActiveOrderReplenishments, type ActiveOrderReplenishmentLine } from '@/api/mes/pro/processpool/replenishment'

const props = defineProps<{ activeOrderId: string }>()
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const lines = ref<ActiveOrderReplenishmentLine[]>([])

const statuses: Record<string, string> = { A: '创建', B: '审核中', C: '已审核', D: '重新审核' }
const statusName = (status: string) => statuses[status] ?? status

const load = async () => {
  loading.value = true
  error.value = ''
  lines.value = []
  try {
    lines.value = await getActiveOrderReplenishments(props.activeOrderId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '补料单加载失败'
  } finally {
    loading.value = false
  }
}
const open = async () => {
  visible.value = true
  await load()
}
</script>
