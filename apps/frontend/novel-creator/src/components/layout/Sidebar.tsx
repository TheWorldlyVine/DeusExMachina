import { NavLink } from 'react-router-dom'

export function Sidebar() {
  const navItems = [
    { path: '/', label: 'Dashboard', icon: '🏠' },
    { path: '/documents', label: 'Documents', icon: '📚' },
    { path: '/settings', label: 'Settings', icon: '⚙️' },
  ]

  return (
    <div className="w-64 bg-card border-r border-border h-full">
      <div className="p-4">
        <h2 className="text-xl font-bold text-foreground">Novel Creator</h2>
      </div>
      <nav className="mt-8">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `block px-4 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary border-r-2 border-primary'
                  : 'text-muted-foreground hover:text-foreground hover:bg-muted/50'
              }`
            }
          >
            <span className="mr-3">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}