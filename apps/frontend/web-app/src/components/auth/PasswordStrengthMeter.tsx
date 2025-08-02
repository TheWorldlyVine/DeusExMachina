interface PasswordStrengthMeterProps {
  password: string
}

interface PasswordRequirement {
  regex: RegExp
  text: string
}

const requirements: PasswordRequirement[] = [
  { regex: /.{8,}/, text: 'At least 8 characters' },
  { regex: /[A-Z]/, text: 'One uppercase letter' },
  { regex: /[a-z]/, text: 'One lowercase letter' },
  { regex: /[0-9]/, text: 'One number' },
  { regex: /[^A-Za-z0-9]/, text: 'One special character' },
]

function calculateStrength(password: string): {
  score: number
  label: string
  color: string
  width: string
} {
  let score = 0
  
  requirements.forEach(({ regex }) => {
    if (regex.test(password)) score++
  })

  if (password.length >= 12) score++
  if (password.length >= 16) score++
  
  const configs = [
    { min: 0, label: 'Weak', color: 'bg-red-500', width: '25%' },
    { min: 3, label: 'Medium', color: 'bg-yellow-500', width: '50%' },
    { min: 5, label: 'Strong', color: 'bg-green-500', width: '75%' },
    { min: 7, label: 'Very Strong', color: 'bg-green-600', width: '100%' },
  ]
  
  const config = configs.reverse().find(c => score >= c.min) || configs[0]
  
  return {
    score,
    label: config.label,
    color: config.color,
    width: config.width,
  }
}

export function PasswordStrengthMeter({ password }: PasswordStrengthMeterProps) {
  if (!password) return null

  const strength = calculateStrength(password)
  const numericWidth = parseInt(strength.width)

  return (
    <div className="mt-2">
      <div className="flex justify-between items-center mb-1">
        <span className="text-sm font-medium text-gray-700">Password strength</span>
        <span className={`text-sm font-medium ${
          strength.label === 'Weak' ? 'text-red-600' :
          strength.label === 'Medium' ? 'text-yellow-600' :
          strength.label === 'Strong' ? 'text-green-600' :
          'text-green-700'
        }`}>
          {strength.label}
        </span>
      </div>
      
      <div 
        className="w-full bg-gray-200 rounded-full h-2 mb-3"
        role="progressbar"
        aria-label="Password strength"
        aria-valuenow={numericWidth}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          data-testid="strength-bar"
          className={`h-2 rounded-full transition-all duration-300 ${strength.color}`}
          style={{ width: strength.width }}
        />
      </div>

      <ul className="text-xs space-y-1">
        {requirements.map(({ regex, text }) => {
          const isMet = regex.test(password)
          return (
            <li
              key={text}
              className={`flex items-center ${
                isMet ? 'text-green-600' : 'text-gray-500'
              }`}
            >
              <span className="mr-2">{isMet ? '✓' : '○'}</span>
              {text}
            </li>
          )
        })}
      </ul>
    </div>
  )
}