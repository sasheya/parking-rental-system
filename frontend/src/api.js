import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  withCredentials: true
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshing
api.interceptors.response.use((response) => response, async (error) => {
  const original = error.config
  if (error.response?.status !== 401 || original?._retry || original?.url?.includes('/api/auth/refresh')) {
    return Promise.reject(error)
  }
  original._retry = true
  refreshing ||= api.post('/api/auth/refresh')
  try {
    const { data } = await refreshing
    const token = data.data?.accessToken || data.accessToken
    if (!token) throw new Error('Refresh response did not include an access token')
    localStorage.setItem('accessToken', token)
    original.headers.Authorization = `Bearer ${token}`
    return api(original)
  } finally {
    refreshing = undefined
  }
})

export default api
