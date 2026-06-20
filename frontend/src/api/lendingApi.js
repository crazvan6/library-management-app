import client from './client.js'

// ---- Reservations ----
export const createReservation = async (bookId) => (await client.post('/v1/reservations/', { bookId })).data
export const myReservations = async () => (await client.get('/v1/reservations/my-reservations')).data
export const cancelReservation = async (id) => { await client.delete(`/v1/reservations/${id}`) }
export const reservationsForBook = async (bookId) => (await client.get(`/v1/reservations/book/${bookId}`)).data

// ---- Loans ----
export const myLoans = async () => (await client.get('/v1/loans/my-loans')).data
export const activeLoans = async () => (await client.get('/v1/loans/active')).data
export const overdueLoans = async () => (await client.get('/v1/loans/overdue')).data
export const checkout = async (payload) => (await client.post('/v1/loans/checkout', payload)).data
export const returnBook = async (loanId) => (await client.post('/v1/loans/return', { loanId })).data

// ---- Fines ----
export const myFinesSummary = async () => (await client.get('/v1/fines/my-fines')).data
export const myPendingFines = async () => (await client.get('/v1/fines/my-fines/pending')).data
export const pendingFines = async () => (await client.get('/v1/fines/pending')).data
export const payFine = async (fineId, paymentMethod) => (await client.post('/v1/fines/pay', { fineId, paymentMethod })).data
export const waiveFine = async (fineId, reason) => (await client.post('/v1/fines/waive', { fineId, reason })).data

export const PAYMENT_METHODS = ['CASH', 'CARD', 'BANK_TRANSFER', 'ONLINE']
