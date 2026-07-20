<template>
  <a-card title="素材库">
    <template #extra>
      <a-space>
        <a-select v-model:value="filterType" placeholder="素材类型" style="width: 120px" allowClear @change="load">
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="video">视频</a-select-option>
          <a-select-option value="image">图片</a-select-option>
          <a-select-option value="audio">音频</a-select-option>
        </a-select>
        <a-input-search v-model:value="filterTaskId" placeholder="任务 ID" style="width: 120px" @search="load" />
      </a-space>
    </template>
    <a-table :dataSource="materials" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'video' ? 'blue' : record.type === 'image' ? 'green' : 'cyan'">{{ typeLabel(record.type) }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'SUCCESS' ? 'green' : record.status === 'FAILURE' ? 'red' : 'orange'">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
            <a style="color: red">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getMaterials, deleteMaterial } from '../../api/material'

const materials = ref<any[]>([])
const loading = ref(false)
const filterType = ref('')
const filterTaskId = ref('')

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '任务 ID', dataIndex: 'taskId' },
  { title: '分镜 ID', dataIndex: 'storyboardId' },
  { title: '类型', key: 'type' },
  { title: '模型', dataIndex: 'model' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt' },
  { title: '操作', key: 'action' },
]

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待处理', SUCCESS: '已完成', FAILURE: '失败' }
  return map[s] || s
}

function typeLabel(s: string) {
  const map: Record<string, string> = { video: '视频', image: '图片', audio: '音频' }
  return map[s] || s
}

async function load() {
  loading.value = true
  try {
    const res: any = await getMaterials({ type: filterType.value || undefined, taskId: filterTaskId.value ? Number(filterTaskId.value) : undefined })
    materials.value = res.data || []
  } finally { loading.value = false }
}

async function handleDelete(id: number) {
  await deleteMaterial(id)
  message.success('已删除')
  load()
}

onMounted(() => load())
</script>