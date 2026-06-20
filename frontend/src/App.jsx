import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import BooksPage from './pages/BooksPage.jsx'
import BookDetailPage from './pages/BookDetailPage.jsx'
import BookFormPage from './pages/BookFormPage.jsx'
import CategoriesPage from './pages/CategoriesPage.jsx'
import ForbiddenPage from './pages/ForbiddenPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <HomePage />
            </ProtectedRoute>
          }
        />
        <Route path="/books" element={<ProtectedRoute><BooksPage /></ProtectedRoute>} />
        <Route path="/books/new" element={<ProtectedRoute roles={['LIBRARIAN', 'ADMIN']}><BookFormPage /></ProtectedRoute>} />
        <Route path="/books/:id" element={<ProtectedRoute><BookDetailPage /></ProtectedRoute>} />
        <Route path="/books/:id/edit" element={<ProtectedRoute roles={['LIBRARIAN', 'ADMIN']}><BookFormPage /></ProtectedRoute>} />
        <Route path="/categories" element={<ProtectedRoute roles={['LIBRARIAN', 'ADMIN']}><CategoriesPage /></ProtectedRoute>} />
        <Route path="/forbidden" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
