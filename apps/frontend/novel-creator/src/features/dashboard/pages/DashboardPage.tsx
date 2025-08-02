export function DashboardPage() {
  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-foreground mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
          <h3 className="text-lg font-semibold mb-2">Recent Documents</h3>
          <p className="text-muted-foreground">No documents yet</p>
        </div>
        <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
          <h3 className="text-lg font-semibold mb-2">Writing Progress</h3>
          <p className="text-muted-foreground">Start writing to track progress</p>
        </div>
        <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
          <h3 className="text-lg font-semibold mb-2">AI Generations</h3>
          <p className="text-muted-foreground">0 generations this month</p>
        </div>
      </div>
    </div>
  )
}