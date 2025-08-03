# Firestore Indexes for Novel Services

variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

# Index for listing documents by user
resource "google_firestore_index" "documents_by_user" {
  project    = var.project_id
  collection = "novel_documents"
  database   = "(default)"

  fields {
    field_path = "active"
    order      = "ASCENDING"
  }

  fields {
    field_path = "authorId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "updatedAt"
    order      = "DESCENDING"
  }

  # This is required for Firestore
  fields {
    field_path = "__name__"
    order      = "DESCENDING"
  }
}