<template>
  <a-card title="AI 模型配置">
    <template #extra>
      <a-button type="primary" @click="showAdd">新增配置</a-button>
    </template>
    <a-table :dataSource="models" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-switch v-model:checked="record.enabled" @change="(v: boolean) => handleToggle(record.id, v)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a style="color: red">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="visible" :title="editingId ? '编辑配置' : '新增配置'" @ok="handleSave" ok-text="保存" width="600px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="模型" :rules="[{ required: true }]">
          <a-select v-model:value="form.modelName" placeholder="从可用模型中选择" show-search @change="onModelSelect">
            <a-select-option v-for="m in allGatewayModels" :key="m.id" :value="m.id">
              {{ m.id }} ({{ m.provider }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="供应商">
          <a-input v-model:value="form.provider" disabled />
        </a-form-item>
        <a-form-item label="API 端点">
          <a-input v-model:value="form.endpoint" disabled />
        </a-form-item>
        <a-form-item label="API Key">
          <a-input-password v-model:value="form.apiKey" placeholder="留空则不修改" />
        </a-form-item>
        <a-form-item label="模型类型">
          <a-select v-model:value="form.modelType">
            <a-select-option value="text">文本</a-select-option>
            <a-select-option value="image">图片</a-select-option>
            <a-select-option value="video">视频</a-select-option>
            <a-select-option value="audio">音频</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="权重（负载均衡）">
          <a-input-number v-model:value="form.weight" :min="1" :max="100" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>

  <a-card title="可用模型（AI Gateway）" style="margin-top: 16px">
    <a-table :dataSource="gatewayProviders" :columns="gatewayColumns" :loading="gwLoading" rowKey="provider" :pagination="false" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'provider'">
          <a-tag>{{ record.provider }}</a-tag>
        </template>
        <template v-if="column.key === 'models'">
          <a-tag v-for="m in record.models" :key="m.id" style="margin: 2px">{{ m.id }}</a-tag>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '../../api/request'

const models = ref<any[]>([])
const loading = ref(false)
const gatewayProviders = ref<any[]>([])
const gwLoading = ref(false)
const visible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<any>({ provider: '', modelName: '', apiKey: '', endpoint: '', modelType: 'text', weight: 10 })

const allGatewayModels = computed(() => {
  const list: any[] = []
  for (const p of gatewayProviders.value) {
    for (const m of p.models || []) {
      list.push({ id: m.id, provider: p.provider, endpoint: p.endpoint || '' })
    }
  }
  return list
})

const columns = [
  { title: '模型名称', dataIndex: 'modelName' },
  { title: '供应商', dataIndex: 'provider' },
  { title: '类型', dataIndex: 'modelType' },
  { title: '端点', dataIndex: 'endpoint' },
  { title: '权重', dataIndex: 'weight' },
  { title: '启用', key: 'enabled', width: 60 },
  { title: '操作', key: 'action', width: 120 },
]

const gatewayColumns = [
  { title: '供应商', key: 'provider' },
  { title: '名称', dataIndex: 'provider_name' },
  { title: 'API 端点', dataIndex: 'endpoint' },
  { title: '模型列表', key: 'models' },
]

function onModelSelect(modelId: string) {
  const m = allGatewayModels.value.find((x) => x.id === modelId)
  if (m) {
    form.value.provider = m.provider
    form.value.endpoint = m.endpoint
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/ai-models')
    models.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

function showAdd() {
  editingId.value = null
  form.value = { provider: '', modelName: '', apiKey: '', endpoint: '', modelType: 'text', weight: 10 }
  visible.value = true
}

function showEdit(record: any) {
  editingId.value = record.id
  form.value = {
    provider: record.provider,
    modelName: record.modelName,
    apiKey: '',
    endpoint: record.endpoint || '',
    modelType: record.modelType || 'text',
    weight: record.weight || 10,
  }
  visible.value = true
}

async function handleSave() {
  const body: any = { provider: form.value.provider, modelName: form.value.modelName, endpoint: form.value.endpoint, modelType: form.value.modelType, weight: form.value.weight }
  if (form.value.apiKey) body.apiKey = form.value.apiKey
  if (editingId.value) {
    await request.put(`/ai-models/${editingId.value}`, body)
    message.success('已更新')
  } else {
    await request.post('/ai-models', body)
    message.success('已创建')
  }
  visible.value = false
  load()
}

async function handleToggle(id: number, enabled: boolean) {
  await request.put(`/ai-models/${id}`, { enabled })
}

async function handleDelete(id: number) {
  await request.delete(`/ai-models/${id}`)
  message.success('已删除')
  load()
}

onMounted(async () => {
  load()
  gwLoading.value = true
  try {
    const gwRes: any = await request.get('http://localhost:8001/ai/v1/models')
    gatewayProviders.value = gwRes.providers || []
  } catch {
    gatewayProviders.value = []
  } finally {
    gwLoading.value = false
  }
})
</script>