<template>
  <ContentWrap data-testid="registration-certificate-read-page">
    <el-form :model="queryParams" inline label-width="96px">
      <el-form-item label="注册证编号">
        <el-input v-model="queryParams.certificateNo" clearable placeholder="请输入注册证编号" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部服务端状态" style="width: 180px">
          <el-option
            v-for="item in REGISTRATION_CERTIFICATE_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目代码">
        <el-select v-model="queryParams.missingProjectCode" clearable placeholder="全部" style="width: 140px">
          <el-option label="已提供" :value="false" />
          <el-option label="缺失" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item label="注册证文件">
        <el-select v-model="queryParams.missingFile" clearable placeholder="全部" style="width: 140px">
          <el-option label="已提供" :value="false" />
          <el-option label="缺失" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" row-key="certificateId">
      <el-table-column label="注册证编号" prop="certificateNo" min-width="180" />
      <el-table-column label="所属公司" prop="ownerCompanyName" min-width="180" />
      <el-table-column label="产品" prop="productName" min-width="180" />
      <el-table-column label="版本" prop="versionNo" width="90" align="center" />
      <el-table-column label="状态" prop="status" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="getRegistrationCertificateStatusTagType(row.status)">
            {{ formatRegistrationCertificateStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="项目代码" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getMissingMarkerTagType(row.hasProjectCode)">
            {{ formatMissingMarker(row.hasProjectCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册证文件" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getMissingMarkerTagType(row.hasRegistrationFile)">
            {{ formatMissingMarker(row.hasRegistrationFile) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="批准日" prop="approvalDate" width="120" />
      <el-table-column label="生效日" prop="effectiveDate" width="120" />
      <el-table-column label="有效期至" prop="expiryDate" width="120" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.certificateId)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="loadPage"
    />
  </ContentWrap>

  <ContentWrap data-testid="registration-certificate-old-index">
    <template #header>
      <span>旧证索引</span>
    </template>
    <el-table v-loading="oldLoading" :data="oldList" row-key="versionId">
      <el-table-column label="注册证编号" prop="certificateNo" min-width="180" />
      <el-table-column label="所属公司" prop="ownerCompanyName" min-width="180" />
      <el-table-column label="产品" prop="productName" min-width="180" />
      <el-table-column label="版本" prop="versionNo" width="90" align="center" />
      <el-table-column label="旧证状态" prop="status" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="getRegistrationCertificateStatusTagType(row.status)">
            {{ formatRegistrationCertificateStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="原有效期至" prop="expiryDate" width="140" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.certificateId)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:page="oldQueryParams.pageNo"
      v-model:limit="oldQueryParams.pageSize"
      :total="oldTotal"
      @pagination="loadOldIndexPage"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getRegistrationCertificateOldIndexPage,
  getRegistrationCertificatePage,
  type DccRegistrationCertificatePageItemVO,
  type DccRegistrationCertificateOldIndexItemVO,
  type DccRegistrationCertificatePageReqVO
} from '@/api/dcc/registrationCertificate'
import {
  REGISTRATION_CERTIFICATE_STATUS_OPTIONS,
  formatMissingMarker,
  formatRegistrationCertificateStatus,
  getMissingMarkerTagType,
  getRegistrationCertificateStatusTagType
} from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateIndex' })

const router = useRouter()
const loading = ref(false)
const oldLoading = ref(false)
const list = ref<DccRegistrationCertificatePageItemVO[]>([])
const oldList = ref<DccRegistrationCertificateOldIndexItemVO[]>([])
const total = ref(0)
const oldTotal = ref(0)

const queryParams = reactive<DccRegistrationCertificatePageReqVO>({
  pageNo: 1,
  pageSize: 10
})

const oldQueryParams = reactive<DccRegistrationCertificatePageReqVO>({
  pageNo: 1,
  pageSize: 10
})

const loadPage = async () => {
  loading.value = true
  try {
    const page = await getRegistrationCertificatePage(queryParams)
    list.value = page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

const loadOldIndexPage = async () => {
  oldLoading.value = true
  try {
    const page = await getRegistrationCertificateOldIndexPage(oldQueryParams)
    oldList.value = page.list || []
    oldTotal.value = page.total || 0
  } finally {
    oldLoading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNo = 1
  oldQueryParams.pageNo = 1
  loadPage()
  loadOldIndexPage()
}

const handleReset = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.certificateNo = undefined
  queryParams.status = undefined
  queryParams.missingProjectCode = undefined
  queryParams.missingFile = undefined
  oldQueryParams.pageNo = 1
  oldQueryParams.pageSize = 10
  loadPage()
  loadOldIndexPage()
}

const openDetail = (certificateId: number | string) => {
  router.push(`/mdm/registration-certificate/detail/${certificateId}`)
}

onMounted(() => {
  loadPage()
  loadOldIndexPage()
})
</script>
