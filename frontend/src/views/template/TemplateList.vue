<template>
  <a-card title="Prompt 模板管理">
    <template #extra>
      <a-button type="primary" @click="showForm = true">新增模板</a-button>
    </template>
    <a-table :dataSource="templates" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-switch v-model:checked="record.enabled" @change="() => handleToggleEnabled(record)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a style="color: red">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="showForm" :title="editingId ? '编辑模板' : '新增模板'" @ok="handleSave" ok-text="保存" width="700px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="form.type">
            <a-select-option value="script">脚本</a-select-option>
            <a-select-option value="prompt">Prompt</a-select-option>
            <a-select-option value="image">图片</a-select-option>
            <a-select-option value="voice">配音</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="内容"><a-textarea v-model:value="form.content" :rows="8" /></a-form-item>
        <a-form-item label="变量（逗号分隔）"><a-input v-model:value="form.variables" placeholder="如: title, topic, style" /></a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTemplates, createTemplate, updateTemplate, deleteTemplate } from '../../api/template'

const templates = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', type: 'script', content: '', variables: '' })

const columns = [
  { title: '名称', dataIndex: 'name' },
  { title: '类型', dataIndex: 'type' },
  { title: '内容', dataIndex: 'content', ellipsis: true },
  { title: '变量', dataIndex: 'variables' },
  { title: '启用', key: 'enabled' },
  { title: '操作', key: 'action' },
]

async function load() {
  loading.value = true
  try {
    const res: any = await getTemplates({})
    templates.value = res.data?.records || res.data || []
  } finally { loading.value = false }
}

function handleEdit(record: any) {
  editingId.value = record.id
  form.value = { name: record.name, type: record.type, content: record.content, variables: record.variables || '' }
  showForm.value = true
}

async function handleSave() {
  if (editingId.value) {
    await updateTemplate(editingId.value, form.value)
    message.success('更新成功')
  } else {
    await createTemplate(form.value)
    message.success('创建成功')
  }
  showForm.value = false
  editingId.value = null
  form.value = { name: '', type: 'script', content: '', variables: '' }
  load()
}

async function handleToggleEnabled(record: any) {
  await updateTemplate(record.id, { enabled: record.enabled })
}

async function handleDelete(id: number) {
  await deleteTemplate(id)
  message.success('已删除')
  load()
}

onMounted(() => load())
</script>