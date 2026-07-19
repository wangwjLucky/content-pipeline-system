import request from './request'

export function getPlatformAccounts(params: { page?: number; size?: number; platform?: string }) {
  return request.get('/platform-accounts', { params })
}

export function getPlatformAccount(id: number) {
  return request.get(`/platform-accounts/${id}`)
}

export function createPlatformAccount(data: any) {
  return request.post('/platform-accounts', data)
}

export function updatePlatformAccount(id: number, data: any) {
  return request.put(`/platform-accounts/${id}`, data)
}

export function deletePlatformAccount(id: number) {
  return request.delete(`/platform-accounts/${id}`)
}