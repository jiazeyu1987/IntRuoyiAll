<template>
  <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <el-alert
      v-if="!assistantUrl"
      title="发票凭证打印助手地址未配置，请配置 VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL。"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <iframe
      v-else
      :src="assistantUrl"
      class="invoice-voucher-print-frame"
      title="发票凭证打印"
      frameborder="0"
      allow="clipboard-read; clipboard-write; fullscreen"
    ></iframe>
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'ErpInvoiceVoucherPrint' })

const assistantUrl = computed(() => {
  return import.meta.env.VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL?.trim() || ''
})
</script>

<style scoped>
.invoice-voucher-print-frame {
  display: block;
  width: 100%;
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding) -
      var(--app-content-padding) - 2px
  );
  border: 0;
}
</style>
