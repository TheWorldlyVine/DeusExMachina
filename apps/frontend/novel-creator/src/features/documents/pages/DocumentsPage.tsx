import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { fetchDocuments, createDocument } from '../documentSlice'
import { LoadingScreen } from '@/components/common/LoadingScreen'
import { toast } from 'react-hot-toast'

export function DocumentsPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const { documents, isLoading, error } = useAppSelector((state) => state.documents)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [isCreating, setIsCreating] = useState(false)

  useEffect(() => {
    dispatch(fetchDocuments())
  }, [dispatch])

  const handleCreateDocument = async () => {
    setIsCreating(true)
    try {
      const result = await dispatch(createDocument({
        title: 'Untitled Novel',
        description: 'A new novel created with AI assistance',
        genre: 'Fiction',
        tags: ['draft']
      })).unwrap()
      
      toast.success('Document created successfully!')
      navigate(`/editor/${result.id}`)
    } catch (error) {
      toast.error('Failed to create document')
      console.error('Create document error:', error)
    } finally {
      setIsCreating(false)
    }
  }

  if (isLoading && documents.length === 0) {
    return <LoadingScreen />
  }

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-foreground">Documents</h1>
        <button 
          onClick={handleCreateDocument}
          disabled={isCreating}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isCreating ? 'Creating...' : 'New Document'}
        </button>
      </div>
      
      {error && (
        <div className="mb-4 p-4 bg-destructive/10 border border-destructive rounded-md">
          <p className="text-destructive">{error}</p>
        </div>
      )}

      {documents.length === 0 ? (
        <div className="bg-card rounded-lg shadow-sm border border-border p-6">
          <p className="text-muted-foreground text-center">No documents yet. Create your first novel!</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {documents.map((doc) => (
            <div
              key={doc.id}
              onClick={() => navigate(`/editor/${doc.id}`)}
              className="bg-card rounded-lg shadow-sm border border-border p-6 cursor-pointer hover:shadow-md transition-shadow"
            >
              <h3 className="text-lg font-semibold text-foreground mb-2">{doc.title}</h3>
              {doc.subtitle && (
                <p className="text-sm text-muted-foreground mb-2">{doc.subtitle}</p>
              )}
              <div className="flex justify-between text-sm text-muted-foreground">
                <span>{doc.wordCount} words</span>
                <span>{new Date(doc.updatedAt).toLocaleDateString()}</span>
              </div>
              <div className="mt-2">
                <span className={`inline-block px-2 py-1 text-xs rounded-full ${
                  doc.status === 'DRAFT' ? 'bg-yellow-100 text-yellow-800' :
                  doc.status === 'IN_REVIEW' ? 'bg-blue-100 text-blue-800' :
                  doc.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' :
                  'bg-gray-100 text-gray-800'
                }`}>
                  {doc.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}