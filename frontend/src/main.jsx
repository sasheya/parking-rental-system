import React, { createContext, useContext, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import Checkout from './Checkout'
import api from './api'
import './styles.css'

const AuthContext = createContext(null)
const useAuth = () => useContext(AuthContext)

function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'))
  const login = async (credentials) => {
    const { data } = await api.post('/api/auth/login', credentials)
    const payload = data.data || data
    localStorage.setItem('accessToken', payload.accessToken)
    localStorage.setItem('user', JSON.stringify(payload))
    setUser(payload)
  }
  const logout = async () => {
    try { await api.post('/api/auth/logout') } finally {
      localStorage.removeItem('accessToken'); localStorage.removeItem('user'); setUser(null)
    }
  }
  const register = async (details) => {
    const { data } = await api.post('/api/auth/register', details)
    const payload = data.data || data
    localStorage.setItem('accessToken', payload.accessToken)
    localStorage.setItem('user', JSON.stringify(payload))
    setUser(payload)
  }
  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>
}

function Layout() {
  const { user, logout } = useAuth()
  return <><header><Link className="brand" to="/">PARKWISE</Link><nav><Link to="/">Find parking</Link>{user && <Link to="/bookings">My bookings</Link>}{user?.role === 'ROLE_OWNER' && <Link to="/owner/listings">Owner listings</Link>}{user ? <button onClick={logout}>Log out</button> : <><Link to="/login">Log in</Link><Link to="/register">Register</Link></>}</nav></header><main><Routes><Route path="/" element={<Search />} /><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route path="/parking/:id" element={<ListingDetail />} /><Route path="/checkout" element={<Protected><Checkout /></Protected>} /><Route path="/bookings" element={<Protected><Bookings /></Protected>} /><Route path="/owner/listings" element={<Protected role="ROLE_OWNER"><OwnerListings /></Protected>} /></Routes></main></>
}

function Protected({ children, role }) { const { user } = useAuth(); if (!user) return <Navigate to="/login" replace />; if (role && user.role !== role) return <Navigate to="/" replace />; return children }

function Login() {
  const { login } = useAuth(); const navigate = useNavigate(); const [form, setForm] = useState({ email: '', password: '' }); const [error, setError] = useState('')
  const submit = async (event) => { event.preventDefault(); setError(''); try { await login(form); navigate('/') } catch { setError('Login failed. Check your credentials.') } }
  return <section className="auth-panel"><p className="eyebrow">Welcome back</p><h1>Find your place.</h1><form onSubmit={submit}><input required type="email" placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /><input required type="password" placeholder="Password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /><button className="primary">Log in</button>{error && <p className="error">{error}</p>}</form></section>
}

function Register() {
  const { register } = useAuth(); const navigate = useNavigate(); const [form, setForm] = useState({ email: '', password: '', fullName: '', role: 'ROLE_DRIVER' }); const [error, setError] = useState('')
  const submit = async (event) => { event.preventDefault(); setError(''); try { await register(form); navigate('/') } catch { setError('Registration failed. Check the form and try again.') } }
  return <section className="auth-panel"><p className="eyebrow">Create an account</p><h1>Park smarter.</h1><form onSubmit={submit}><input required placeholder="Full name" value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} /><input required type="email" placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /><input required minLength="6" type="password" placeholder="Password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /><select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}><option value="ROLE_DRIVER">Renter</option><option value="ROLE_OWNER">Owner</option></select><button className="primary">Create account</button>{error && <p className="error">{error}</p>}</form></section>
}

function Search() {
  const [city, setCity] = useState(''); const [spaces, setSpaces] = useState([]); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const search = async (event) => { event.preventDefault(); setLoading(true); setError(''); try { const { data } = await api.get('/api/parking/search', { params: { city } }); setSpaces(data.data || data) } catch { setError('Parking search is unavailable right now.') } finally { setLoading(false) } }
  return <section><div className="hero"><p className="eyebrow">Parking, without the hunt</p><h1>Leave the circling behind.</h1><p>Reserve a reliable space near where you are going.</p><form className="search" onSubmit={search}><input placeholder="Search by city" value={city} onChange={e => setCity(e.target.value)} /><button className="primary">Search spaces</button></form></div><div className="results">{loading && <p>Searching...</p>}{error && <p className="error">{error}</p>}{spaces.map(space => <Link to={`/parking/${space.id}`} className="space" key={space.id}><div><p className="eyebrow">{space.city || 'Local parking'}</p><h2>{space.title || space.address}</h2><p>{space.address}</p></div><strong>${space.pricePerHour}/hr</strong></Link>)}</div></section>
}

function ListingDetail() { const { id } = useParams(); const [space, setSpace] = useState(null); const [slots, setSlots] = useState([]); useEffect(() => { Promise.all([api.get(`/api/parking/${id}`), api.get(`/api/parking/${id}/availability`)]).then(([spaceResponse, slotsResponse]) => { setSpace(spaceResponse.data.data || spaceResponse.data); setSlots(slotsResponse.data.data || slotsResponse.data) }) }, [id]); if (!space) return <p>Loading listing...</p>; return <section><p className="eyebrow">Parking listing</p><h1>{space.title || space.address}</h1><p>{space.description || 'A reliable space for your next stop.'}</p><p>{space.address}</p><h2>Available slots</h2>{slots.length === 0 ? <p className="muted">No slots published yet.</p> : slots.map(slot => <article className="space" key={slot.id}><span>{slot.startTime} to {slot.endTime}</span>{slot.isBooked ? <strong>Booked</strong> : <Link className="primary" to={`/checkout?spaceId=${id}&slotId=${slot.id}&startTime=${encodeURIComponent(slot.startTime)}&endTime=${encodeURIComponent(slot.endTime)}&price=${space.pricePerHour}`}>Book ${space.pricePerHour}</Link>}</article>)}</section> }

function OwnerListings() { const { user } = useAuth(); const [listings, setListings] = useState([]); const [form, setForm] = useState({ title: '', address: '', city: '', pricePerHour: '', totalSlots: 1 }); const [message, setMessage] = useState(''); useEffect(() => { api.get(`/api/parking/owner/${user.userId}`).then(({ data }) => setListings(data.data || data)) }, [user.userId]); const submit = async (event) => { event.preventDefault(); const { data } = await api.post('/api/parking', { ...form, pricePerHour: Number(form.pricePerHour), totalSlots: Number(form.totalSlots) }); setListings([...listings, data.data || data]); setMessage('Listing created.'); setForm({ title: '', address: '', city: '', pricePerHour: '', totalSlots: 1 }) }; return <section><p className="eyebrow">Owner workspace</p><h1>Your spaces.</h1><form className="owner-form" onSubmit={submit}><input required placeholder="Listing title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /><input required placeholder="Address" value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} /><input required placeholder="City" value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} /><input required min="0.01" type="number" placeholder="Price per hour" value={form.pricePerHour} onChange={e => setForm({ ...form, pricePerHour: e.target.value })} /><input required min="1" type="number" placeholder="Total slots" value={form.totalSlots} onChange={e => setForm({ ...form, totalSlots: e.target.value })} /><button className="primary">Create listing</button>{message && <p>{message}</p>}</form><div className="results">{listings.map(listing => <article className="space" key={listing.id}><div><h2>{listing.title || listing.address}</h2><p>{listing.address}</p></div><strong>${listing.pricePerHour}/hr</strong></article>)}</div></section> }

function Bookings() { const [bookings, setBookings] = useState([]); const [loaded, setLoaded] = useState(false); useEffect(() => { api.get('/api/bookings/my-bookings').then(({ data }) => setBookings(data.data || data)).finally(() => setLoaded(true)) }, []); return <section><p className="eyebrow">Your reservations</p><h1>My bookings</h1>{!loaded ? <p>Loading...</p> : bookings.length === 0 ? <p className="muted">No bookings yet.</p> : bookings.map(booking => <article className="space" key={booking.id}><div><h2>Booking #{booking.id}</h2><p>{booking.startTime} to {booking.endTime}</p></div><strong>{booking.status}</strong></article>)}</section> }

createRoot(document.getElementById('root')).render(<BrowserRouter><AuthProvider><Layout /></AuthProvider></BrowserRouter>)
