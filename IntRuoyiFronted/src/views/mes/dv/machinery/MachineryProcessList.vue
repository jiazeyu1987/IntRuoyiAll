<template>
  <div>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" border>
      <el-table-column label="设备编码" align="center" prop="machineryCode" min-width="120" />
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
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { DvMachineryProcessApi, DvMachineryProcessVO } from '@/api/mes/dv/machinery/process'

defineOptions({ name: 'MachineryProcessList' })

const props = defineProps<{
  machineryId: number
}>()

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
