<template>
  <a-card title="AI 模型配置">
    <a-table :dataSource="models" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-switch v-model:checked="record.enabled" />
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'

const models = ref<any[]>([])
const loading = ref(false)

const columns = [
  { title: '模型名称', dataIndex: 'modelName' },
  { title: '供应商', dataIndex: 'provider' },
  { title: '类型', dataIndex: 'modelType' },
  { title: '端点', dataIndex: 'endpoint' },
  { title: '权重', dataIndex: 'weight' },
  { title: '启用', key: 'enabled', width: 80 },
]

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await request.get('/ai-models')
    models.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
})
</script>