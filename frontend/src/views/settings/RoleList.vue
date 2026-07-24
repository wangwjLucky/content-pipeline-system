<template>
  <a-card title="角色管理">
    <template #extra>
      <a-button type="primary" @click="showForm = true; editingId = null; form = { name: '', code: '', description: '' }">新增角色</a-button>
    </template>
    <a-table :dataSource="roles" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
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

    <a-modal v-model:open="showForm" :title="editingId ? '编辑角色' : '新增角色'" @ok="handleSave" ok-text="保存">
      <a-form :model="form" layout="vertical">
        <a-form-item label="角色名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="角色编码"><a-input v-model:value="form.code" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRoles, createRole, updateRole, deleteRole } from '../../api/role'

const roles = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', code: '', description: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '角色名称', dataIndex: 'name' },
  { title: '角色编码', dataIndex: 'code' },
  { title: '描述', dataIndex: 'description' },
  { title: '创建时间', dataIndex: 'createdAt' },
  { title: '操作', key: 'action', width: 150 },
]

async function load() {
  loading.value = true
  try {
    const res: any = await getRoles({})
    roles.value = res.data?.records || res.data || []
  } finally { loading.value = false }
}

function handleEdit(record: any) {
  editingId.value = record.id
  form.value = { name: record.name, code: record.code, description: record.description || '' }
  showForm.value = true
}

async function handleSave() {
  if (editingId.value) {
    await updateRole(editingId.value, form.value)
    message.success('更新成功')
  } else {
    await createRole(form.value)
    message.success('创建成功')
  }
  showForm.value = false
  editingId.value = null
  load()
}

async function handleDelete(id: number) {
  await deleteRole(id)
  message.success('已删除')
  load()
}

onMounted(() => load())
</script>