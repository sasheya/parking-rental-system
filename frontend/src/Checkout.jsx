import React, { useState } from 'react'
import { CardElement, Elements, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { useSearchParams, Link, useNavigate } from 'react-router-dom'
import api from './api'

const stripePromise = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY
  ? loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY)
  : null

function PaymentForm() {
  const [params] = useSearchParams()
  const { user } = JSON.parse(localStorage.getItem('user') || '{}')
  const stripe = useStripe()
  const elements = useElements()
  const navigate = useNavigate()
  const [vehicleId, setVehicleId] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setMessage('')
    try {
      const bookingResponse = await api.post('/api/bookings', {
        parkingSpaceId: Number(params.get('spaceId')),
        slotId: Number(params.get('slotId')),
        vehicleId: vehicleId ? Number(vehicleId) : null,
        startTime: params.get('startTime'),
        endTime: params.get('endTime')
      })
      const booking = bookingResponse.data.data || bookingResponse.data
      const paymentResponse = await api.post('/api/payments/initiate', {
        bookingId: booking.id,
        amount: booking.totalAmount,
        currency: 'usd'
      })
      const payment = paymentResponse.data.data || paymentResponse.data

      if (stripe && elements && payment.clientSecret && !payment.paymentIntentId?.startsWith('pi_mock_')) {
        const result = await stripe.confirmCardPayment(payment.clientSecret, {
          payment_method: { card: elements.getElement(CardElement) }
        })
        if (result.error) throw new Error(result.error.message)
      }

      await api.post('/api/payments/confirm', {
        paymentIntentId: payment.paymentIntentId,
        paymentMethod: 'card'
      })
      navigate('/bookings')
    } catch (error) {
      setMessage(error.response?.data?.error?.message || error.message || 'Checkout failed.')
    } finally {
      setLoading(false)
    }
  }

  return <section className="auth-panel">
    <p className="eyebrow">Secure checkout</p>
    <h1>Reserve your space.</h1>
    <p>{params.get('startTime')} to {params.get('endTime')}</p>
    <form onSubmit={submit}>
      <input placeholder="Vehicle ID (optional)" value={vehicleId} onChange={event => setVehicleId(event.target.value)} />
      {stripePromise && <div className="card-element"><CardElement /></div>}
      {!stripePromise && <p className="muted">Simulation mode is active. Configure a Stripe publishable key for card entry.</p>}
      <button className="primary" disabled={loading || (Boolean(stripePromise) && !stripe)}>{loading ? 'Processing...' : 'Confirm and pay'}</button>
      {message && <p className="error">{message}</p>}
      <Link to={`/parking/${params.get('spaceId')}`}>Back to listing</Link>
    </form>
  </section>
}

export default function Checkout() {
  return <Elements stripe={stripePromise}><PaymentForm /></Elements>
}
