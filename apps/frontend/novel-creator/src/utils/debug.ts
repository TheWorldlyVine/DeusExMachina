// Debug utility for troubleshooting auth and API issues

export const debugAuth = () => {
  const token = localStorage.getItem('auth_token')
  
  if (!token) {
    console.log('❌ No auth token found in localStorage')
    return null
  }
  
  console.log('✅ Auth token found:', token.substring(0, 20) + '...')
  
  try {
    // Decode JWT without verification to inspect claims
    const parts = token.split('.')
    if (parts.length !== 3) {
      console.log('❌ Invalid JWT format')
      return null
    }
    
    const payload = JSON.parse(atob(parts[1]))
    console.log('📋 JWT Payload:', payload)
    console.log('👤 User ID (sub):', payload.sub)
    console.log('📧 Email:', payload.email)
    console.log('⏰ Expires:', new Date(payload.exp * 1000).toLocaleString())
    
    const now = Date.now() / 1000
    if (payload.exp < now) {
      console.log('❌ Token is expired!')
    } else {
      console.log('✅ Token is valid for', Math.floor((payload.exp - now) / 60), 'more minutes')
    }
    
    return payload
  } catch (error) {
    console.error('❌ Error decoding token:', error)
    return null
  }
}

export const debugApiCall = async (url: string, options: RequestInit = {}) => {
  console.log(`🌐 Making API call to: ${url}`)
  console.log('📤 Request options:', options)
  
  try {
    const response = await fetch(url, options)
    console.log('📥 Response status:', response.status, response.statusText)
    console.log('📥 Response headers:', Object.fromEntries(response.headers.entries()))
    
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      const data = await response.json()
      console.log('📥 Response data:', data)
      return data
    } else {
      const text = await response.text()
      console.log('📥 Response text:', text)
      return text
    }
  } catch (error) {
    console.error('❌ API call failed:', error)
    throw error
  }
}

// Add to window for easy console access
if (typeof window !== 'undefined') {
  (window as unknown as { debugAuth: typeof debugAuth }).debugAuth = debugAuth;
  (window as unknown as { debugApiCall: typeof debugApiCall }).debugApiCall = debugApiCall;
}