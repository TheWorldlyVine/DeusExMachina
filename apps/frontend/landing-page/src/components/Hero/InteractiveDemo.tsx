import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import styles from './InteractiveDemo.module.css'

interface InteractiveDemoProps {
  isActive: boolean
}

interface DemoNode {
  id: string
  name: string
  type: 'location' | 'character' | 'event'
  x: number
  y: number
  connections: string[]
}

const demoData: DemoNode[] = [
  {
    id: '1',
    name: 'The Crystal Citadel',
    type: 'location',
    x: 50,
    y: 30,
    connections: ['2', '3']
  },
  {
    id: '2',
    name: 'Elara Moonshadow',
    type: 'character',
    x: 25,
    y: 50,
    connections: ['1', '4']
  },
  {
    id: '3',
    name: 'Dragon\'s Awakening',
    type: 'event',
    x: 75,
    y: 50,
    connections: ['1', '4']
  },
  {
    id: '4',
    name: 'The Whispering Woods',
    type: 'location',
    x: 50,
    y: 70,
    connections: ['2', '3']
  }
]

export function InteractiveDemo({ isActive }: InteractiveDemoProps) {
  const [selectedNode, setSelectedNode] = useState<string | null>(null)
  const [hoveredNode, setHoveredNode] = useState<string | null>(null)

  useEffect(() => {
    if (isActive && !selectedNode) {
      setSelectedNode('1')
    }
  }, [isActive, selectedNode])

  const getNodeIcon = (type: DemoNode['type']) => {
    switch (type) {
      case 'location':
        return '🏰'
      case 'character':
        return '👤'
      case 'event':
        return '⚡'
    }
  }

  const getNodeColor = (type: DemoNode['type']) => {
    switch (type) {
      case 'location':
        return 'var(--color-primary)'
      case 'character':
        return 'var(--color-secondary)'
      case 'event':
        return 'var(--color-warning)'
    }
  }

  return (
    <div className={styles.demo}>
      <div className={styles.header}>
        <h3 className={styles.title}>Interactive World Map</h3>
        <p className={styles.subtitle}>Click nodes to explore connections</p>
      </div>

      <div className={styles.canvas}>
        <svg className={styles.connections}>
          {demoData.map(node => 
            node.connections.map(targetId => {
              const target = demoData.find(n => n.id === targetId)
              if (!target || node.id > targetId) return null
              
              const isHighlighted = 
                hoveredNode === node.id || 
                hoveredNode === targetId ||
                selectedNode === node.id ||
                selectedNode === targetId

              return (
                <motion.line
                  key={`${node.id}-${targetId}`}
                  x1={`${node.x}%`}
                  y1={`${node.y}%`}
                  x2={`${target.x}%`}
                  y2={`${target.y}%`}
                  stroke={isHighlighted ? 'var(--color-primary)' : 'var(--color-border)'}
                  strokeWidth={isHighlighted ? 3 : 2}
                  opacity={isHighlighted ? 1 : 0.3}
                  initial={{ pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ duration: 1, delay: 0.5 }}
                />
              )
            })
          )}
        </svg>

        {demoData.map((node, index) => (
          <motion.div
            key={node.id}
            className={styles.node}
            style={{
              left: `${node.x}%`,
              top: `${node.y}%`,
              transform: 'translate(-50%, -50%)'
            }}
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
            onClick={() => setSelectedNode(node.id)}
            onMouseEnter={() => setHoveredNode(node.id)}
            onMouseLeave={() => setHoveredNode(null)}
          >
            <div 
              className={styles.nodeCircle}
              style={{
                backgroundColor: getNodeColor(node.type),
                transform: selectedNode === node.id ? 'scale(1.2)' : 'scale(1)'
              }}
            >
              <span className={styles.nodeIcon}>{getNodeIcon(node.type)}</span>
            </div>
            <span className={styles.nodeName}>{node.name}</span>
          </motion.div>
        ))}

        <AnimatePresence>
          {selectedNode && (
            <motion.div
              className={styles.detailPanel}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 20 }}
            >
              {(() => {
                const node = demoData.find(n => n.id === selectedNode)
                if (!node) return null
                
                return (
                  <>
                    <div className={styles.detailHeader}>
                      <span className={styles.detailIcon}>{getNodeIcon(node.type)}</span>
                      <h4>{node.name}</h4>
                    </div>
                    <p className={styles.detailType}>{node.type}</p>
                    <p className={styles.detailDescription}>
                      {node.type === 'location' && 'A mystical place of power and ancient secrets.'}
                      {node.type === 'character' && 'A key figure in the unfolding narrative.'}
                      {node.type === 'event' && 'A pivotal moment that shapes the world.'}
                    </p>
                    <div className={styles.detailConnections}>
                      <strong>Connected to:</strong>
                      <ul>
                        {node.connections.map(id => {
                          const connected = demoData.find(n => n.id === id)
                          return connected ? (
                            <li key={id}>{connected.name}</li>
                          ) : null
                        })}
                      </ul>
                    </div>
                  </>
                )
              })()}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className={styles.footer}>
        <p>This is just a glimpse. Create unlimited worlds with infinite possibilities.</p>
      </div>
    </div>
  )
}