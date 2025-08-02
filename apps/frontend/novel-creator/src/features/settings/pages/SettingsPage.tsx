export function SettingsPage() {
  return (
    <div className="p-8 max-w-4xl">
      <h1 className="text-3xl font-bold text-foreground mb-6">Settings</h1>
      
      <div className="space-y-6">
        <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
          <h2 className="text-xl font-semibold mb-4">Profile</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <input type="text" className="w-full px-3 py-2 border border-input rounded-md" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input type="email" className="w-full px-3 py-2 border border-input rounded-md" />
            </div>
          </div>
        </div>
        
        <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
          <h2 className="text-xl font-semibold mb-4">AI Preferences</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Default Model</label>
              <select className="w-full px-3 py-2 border border-input rounded-md">
                <option value="balanced">Balanced</option>
                <option value="speed">Speed</option>
                <option value="quality">Quality</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Writing Style</label>
              <select className="w-full px-3 py-2 border border-input rounded-md">
                <option value="narrative">Narrative</option>
                <option value="descriptive">Descriptive</option>
                <option value="dialogue">Dialogue-heavy</option>
              </select>
            </div>
          </div>
        </div>
        
        <button className="px-6 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90">
          Save Changes
        </button>
      </div>
    </div>
  )
}