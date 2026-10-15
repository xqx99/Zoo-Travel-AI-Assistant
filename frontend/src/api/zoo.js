import axios from 'axios'

export const listZoos = (params) =>
  axios.get('/api/zoo/list', { params }).then(r => r.data.data)

export const getZoo = (id) =>
  axios.get(`/api/zoo/${id}`).then(r => r.data.data)

export const listCities = () =>
  axios.get('/api/zoo/cities').then(r => r.data.data)

export const createZoo = (data) =>
  axios.post('/api/zoo', data).then(r => r.data)

export const updateZoo = (id, data) =>
  axios.put(`/api/zoo/${id}`, data).then(r => r.data)

export const deleteZoo = (id) =>
  axios.delete(`/api/zoo/${id}`).then(r => r.data)
