<template>
  <doc-alert title="ERP 手册（功能开启）" url="https://doc.iocoder.cn/erp/build/" />

  <div class="flex flex-col">
    <el-row v-if="isSuperAdmin" :gutter="16" class="row">
      <el-col :span="24">
        <div class="erp-write-permission-panel">
          <div class="erp-write-permission-panel__main">
            <div class="erp-write-permission-panel__title">ERP 写权限</div>
            <div class="erp-write-permission-panel__desc">
              关闭后禁止系统写入外部 ERP；打开后允许已授权的 ERP 写入入口执行。
            </div>
          </div>
          <div class="erp-write-permission-panel__action">
            <el-tag :type="externalWriteEnabled ? 'success' : 'danger'" effect="light">
              {{ externalWriteEnabled ? '允许写入' : '禁止写入' }}
            </el-tag>
            <el-switch
              :model-value="externalWriteEnabled"
              :loading="externalWriteLoading"
              :disabled="externalWriteLoading"
              inline-prompt
              active-text="开"
              inactive-text="关"
              @click="toggleExternalWritePermission"
            />
          </div>
        </div>
      </el-col>
    </el-row>
    <!-- 销售/采购的全局统计 -->
    <el-row :gutter="16" class="row">
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="今日销售" :value="saleSummary?.todayPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="昨日销售" :value="saleSummary?.yesterdayPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="今日采购" :value="purchaseSummary?.todayPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="昨日采购" :value="purchaseSummary?.yesterdayPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="本月销售" :value="saleSummary?.monthPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="今年销售" :value="saleSummary?.yearPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="本月采购" :value="purchaseSummary?.monthPrice" />
      </el-col>
      <el-col :md="6" :sm="12" :xs="24" :loading="loading">
        <SummaryCard title="今年采购" :value="purchaseSummary?.yearPrice" />
      </el-col>
    </el-row>
    <!-- 销售/采购的时段统计 -->
    <el-row :gutter="16" class="row">
      <!-- 销售统计 -->
      <el-col :md="12" :sm="12" :xs="24" :loading="loading">
        <TimeSummaryChart title="销售统计" :value="saleTimeSummaryList" />
      </el-col>
      <!-- 采购统计 -->
      <el-col :md="12" :sm="12" :xs="24" :loading="loading">
        <TimeSummaryChart title="采购统计" :value="purchaseTimeSummaryList" />
      </el-col>
    </el-row>
  </div>
</template>
<script lang="ts" setup>
import SummaryCard from './components/SummaryCard.vue'
import TimeSummaryChart from './components/TimeSummaryChart.vue'
import {
  ErpSaleSummaryRespVO,
  ErpSaleTimeSummaryRespVO,
  SaleStatisticsApi
} from '@/api/erp/statistics/sale'
import {
  ErpPurchaseSummaryRespVO,
  ErpPurchaseTimeSummaryRespVO,
  PurchaseStatisticsApi
} from '@/api/erp/statistics/purchase'
import { ErpKingdeeConfigApi } from '@/api/erp/config'
import { checkRole } from '@/utils/permission'

/** 商城首页 */
defineOptions({ name: 'ErpHome' })

const message = useMessage()
const loading = ref(true) // 加载中
const isSuperAdmin = computed(() => checkRole(['super_admin']))
const externalWriteEnabled = ref(false)
const externalWriteLoading = ref(false)

/** 获得销售统计 */
const saleSummary = ref<ErpSaleSummaryRespVO>() // 销售概况统计
const saleTimeSummaryList = ref<ErpSaleTimeSummaryRespVO[]>() // 销售时段统计
const getSaleSummary = async () => {
  saleSummary.value = await SaleStatisticsApi.getSaleSummary()
  saleTimeSummaryList.value = await SaleStatisticsApi.getSaleTimeSummary()
}

/** 获得采购统计 */
const purchaseSummary = ref<ErpPurchaseSummaryRespVO>() // 采购概况统计
const purchaseTimeSummaryList = ref<ErpPurchaseTimeSummaryRespVO[]>() // 采购时段统计
const getPurchaseSummary = async () => {
  purchaseSummary.value = await PurchaseStatisticsApi.getPurchaseSummary()
  purchaseTimeSummaryList.value = await PurchaseStatisticsApi.getPurchaseTimeSummary()
}

/** 获得 ERP 写权限状态 */
const getExternalWritePermission = async () => {
  if (!isSuperAdmin.value) {
    return
  }
  externalWriteLoading.value = true
  try {
    const permission = await ErpKingdeeConfigApi.getExternalWritePermission()
    externalWriteEnabled.value = permission.enabled
  } finally {
    externalWriteLoading.value = false
  }
}

/** 切换 ERP 写权限状态 */
const toggleExternalWritePermission = async () => {
  if (externalWriteLoading.value) {
    return
  }
  const previousEnabled = externalWriteEnabled.value
  const nextEnabled = !previousEnabled
  externalWriteEnabled.value = nextEnabled
  externalWriteLoading.value = true
  try {
    await ErpKingdeeConfigApi.updateExternalWritePermission({ enabled: nextEnabled })
    if (nextEnabled) {
      message.success('ERP 写权限已打开')
    } else {
      message.error('ERP 写权限已关闭')
    }
  } catch (error) {
    externalWriteEnabled.value = previousEnabled
    message.error('ERP 写权限保存失败，请稍后重试')
    throw error
  } finally {
    externalWriteLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  loading.value = true
  await Promise.all([getSaleSummary(), getPurchaseSummary(), getExternalWritePermission()])
  loading.value = false
})
</script>
<style lang="scss" scoped>
.row {
  .el-col {
    margin-bottom: 1rem;
  }
}

.erp-write-permission-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.erp-write-permission-panel__main {
  min-width: 0;
}

.erp-write-permission-panel__title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.erp-write-permission-panel__desc {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.erp-write-permission-panel__action {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}
</style>
