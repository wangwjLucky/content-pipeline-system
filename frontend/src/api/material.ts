import request from './request'

export function getMaterials(params: { taskId?: number; storyboardId?: number; type?: string }) {
  return request.get('/materials', { params })
}

export function getMaterial(id: number) {
  return request.get(`/materials/${id}`)
}

export function deleteMaterial(id: number) {
  return request.delete(`/materials/${id}`)
}

export function batchGenerateMaterials(taskId: number) {
  return request.post(`/materials/batch-generate?taskId=${taskId}`)
}

export function getMaterialDownloadUrl(bucket: string, key: string) {
  return `/api/v1/files/download?bucket=${bucket}&key=${key}`
}