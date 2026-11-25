<template>
  <div>
    <el-button
      v-if="editable"
      v-hasPermi="['mes:dv-machinery:update']"
      type="primary"
      plain
      class="mb-10px"
      @click="capacityFormRef?.open(machineryId)"
      >维护工序产能</el-button
    >
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" border>
      <el-table-column label="设备编码" align="center" prop="machineryCode" min-width="120"
        ><template #default="{ row }"><MachineryCodeLink :code="row.machineryCode" /></template
      ></el-table-column>
      <el-table-column label="产线名称" align="center" prop="lineName" min-width="180" />
      <el-table-column label="工序名称" align="center" prop="processName" min-width="160" />
      <el-table-column label="设备名称" align="center" prop="deviceName" min-width="180" />
      <el-table-column label="设备数量" align="center" prop="deviceQuantity" width="100" />
      <el-table-column
        label="10.5小时日产能"
        align="center"
        prop="tenHalfHourDailyCapacity"
        width="140"
      />
      <el-table-column
        label="设备标准小时产能"
        align="center"
        prop="standardHourlyCapacity"
        width="150"
      />
      <el-table-column label="备注" align="center" prop="remark" min-width="120" />
      <el-table-column v-if="editable" label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.processId"
            v-hasPermi="['mes:dv-machinery:update']"
            link
            type="primary"
            @click="capacityFormRef?.open(machineryId, row.processId)"
            >维护产能</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <MachineryProcessCapacityForm ref="capacityFormRef" @success="handleCapacitySaved" />
  </div>
</template>

<script setup lang="ts">
import MachineryCodeLink from '@/components/MachineryCodeLink.vue'
import MachineryProcessCapacityForm from './MachineryProcessCapacityForm.vue'
import { DvMachineryProcessApi, DvMachineryProcessVO } from '@/api/mes/dv/machinery/process'

defineOptions({ name: 'MachineryProcessList' })

const props = defineProps<{
  machineryId: number | string
  editable?: boolean
}>()

const capacityFormRef = ref()
const emit = defineEmits(['success'])
const handleCapacitySaved = async () => {
  await getList()
  emit('success')
}

const loading = ref(false)
const list = ref<DvMachineryProcessVO[]>([])

const getList = async () => {
  if (!props.machineryId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await DvMachineryProcessApi.getMachineryProcessList(props.machineryId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.machineryId,
  () => {
    getList()
  },
  { immediate: true }
)
</script>
