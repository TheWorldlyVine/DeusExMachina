import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { apolloClient } from '@/services/apollo'
import { gql } from '@apollo/client'
import { CharacterMemory, PlotMemory, WorldMemory } from '@/types/memory'

interface MemoryState {
  characters: CharacterMemory[]
  plots: PlotMemory[]
  locations: WorldMemory[]
  currentCharacter: CharacterMemory | null
  currentPlot: PlotMemory | null
  currentLocation: WorldMemory | null
  isLoading: boolean
  error: string | null
}

const initialState: MemoryState = {
  characters: [],
  plots: [],
  locations: [],
  currentCharacter: null,
  currentPlot: null,
  currentLocation: null,
  isLoading: false,
  error: null,
}

// GraphQL Queries
const GET_CHARACTERS = gql`
  query GetCharacters($projectId: ID!) {
    characters(projectId: $projectId) {
      characterId
      projectId
      name
      role
      currentState {
        description
        emotionalState
        goals
        location
        lastUpdated
      }
      observations {
        observationId
        chapterNumber
        sceneNumber
        observation
        observationType
        timestamp
      }
      reflections {
        reflectionId
        reflection
        emotionalImpact
        decisionsInfluenced
        timestamp
      }
      executedActions {
        actionId
        chapterNumber
        sceneNumber
        action
        motivation
        result
        timestamp
      }
      relationships {
        targetCharacterId
        targetCharacterName
        relationshipType
        description
        dynamics {
          chapterNumber
          status
          change
        }
      }
      timelineSummary {
        firstAppearance {
          chapterNumber
          sceneNumber
        }
        lastAppearance {
          chapterNumber
          sceneNumber
        }
        totalScenes
        significantMoments
      }
      metadata
    }
  }
`

const GET_PLOTS = gql`
  query GetPlots($projectId: ID!) {
    plots(projectId: $projectId) {
      plotId
      projectId
      title
      description
      storyArc
      currentState {
        status
        tensionLevel
        lastUpdated
      }
      keyMoments {
        momentId
        chapterNumber
        sceneNumber
        momentType
        description
        impact
        timestamp
      }
      involvedCharacters {
        characterId
        characterName
        role
      }
      conflicts {
        type
        description
        resolved
        resolution
      }
      relatedSubplots
      foreshadowing
      metadata
    }
  }
`

const CREATE_CHARACTER = gql`
  mutation CreateCharacter($character: CharacterInput!) {
    createCharacter(character: $character) {
      characterId
      name
      role
    }
  }
`

const UPDATE_CHARACTER = gql`
  mutation UpdateCharacter($projectId: ID!, $characterId: ID!, $updates: CharacterInput!) {
    updateCharacter(projectId: $projectId, characterId: $characterId, updates: $updates) {
      characterId
      name
      role
    }
  }
`

const DELETE_CHARACTER = gql`
  mutation DeleteCharacter($projectId: ID!, $characterId: ID!) {
    deleteCharacter(projectId: $projectId, characterId: $characterId)
  }
`

const CREATE_PLOT = gql`
  mutation CreatePlot($plot: PlotInput!) {
    createPlot(plot: $plot) {
      plotId
      title
      storyArc
    }
  }
`

const UPDATE_PLOT = gql`
  mutation UpdatePlot($projectId: ID!, $plotId: ID!, $updates: PlotInput!) {
    updatePlot(projectId: $projectId, plotId: $plotId, updates: $updates) {
      plotId
      title
      storyArc
    }
  }
`

const DELETE_PLOT = gql`
  mutation DeletePlot($projectId: ID!, $plotId: ID!) {
    deletePlot(projectId: $projectId, plotId: $plotId)
  }
`

// Async Thunks
export const getCharacters = createAsyncThunk(
  'memory/getCharacters',
  async (projectId: string) => {
    const { data } = await apolloClient.query({
      query: GET_CHARACTERS,
      variables: { projectId },
    })
    return data.characters as CharacterMemory[]
  }
)

export const getPlots = createAsyncThunk(
  'memory/getPlots',
  async (projectId: string) => {
    const { data } = await apolloClient.query({
      query: GET_PLOTS,
      variables: { projectId },
    })
    return data.plots as PlotMemory[]
  }
)

export const getLocations = createAsyncThunk(
  'memory/getLocations',
  async () => {
    // TODO: Implement with GraphQL when location queries are ready
    return [] as WorldMemory[]
  }
)

export const createCharacter = createAsyncThunk(
  'memory/createCharacter',
  async (character: Partial<CharacterMemory>) => {
    const { data } = await apolloClient.mutate({
      mutation: CREATE_CHARACTER,
      variables: { character },
    })
    return data.createCharacter
  }
)

export const updateCharacter = createAsyncThunk(
  'memory/updateCharacter',
  async ({ projectId, characterId, updates }: { 
    projectId: string
    characterId: string
    updates: Partial<CharacterMemory> 
  }) => {
    const { data } = await apolloClient.mutate({
      mutation: UPDATE_CHARACTER,
      variables: { projectId, characterId, updates },
    })
    return data.updateCharacter
  }
)

export const deleteCharacter = createAsyncThunk(
  'memory/deleteCharacter',
  async ({ projectId, characterId }: { projectId: string; characterId: string }) => {
    await apolloClient.mutate({
      mutation: DELETE_CHARACTER,
      variables: { projectId, characterId },
    })
    return characterId
  }
)

export const createPlot = createAsyncThunk(
  'memory/createPlot',
  async (plot: Partial<PlotMemory>) => {
    const { data } = await apolloClient.mutate({
      mutation: CREATE_PLOT,
      variables: { plot },
    })
    return data.createPlot
  }
)

export const updatePlot = createAsyncThunk(
  'memory/updatePlot',
  async ({ projectId, plotId, updates }: { 
    projectId: string
    plotId: string
    updates: Partial<PlotMemory> 
  }) => {
    const { data } = await apolloClient.mutate({
      mutation: UPDATE_PLOT,
      variables: { projectId, plotId, updates },
    })
    return data.updatePlot
  }
)

export const deletePlot = createAsyncThunk(
  'memory/deletePlot',
  async ({ projectId, plotId }: { projectId: string; plotId: string }) => {
    await apolloClient.mutate({
      mutation: DELETE_PLOT,
      variables: { projectId, plotId },
    })
    return plotId
  }
)

const enhancedMemorySlice = createSlice({
  name: 'memory',
  initialState,
  reducers: {
    setCurrentCharacter: (state, action: PayloadAction<CharacterMemory | null>) => {
      state.currentCharacter = action.payload
    },
    setCurrentPlot: (state, action: PayloadAction<PlotMemory | null>) => {
      state.currentPlot = action.payload
    },
    setCurrentLocation: (state, action: PayloadAction<WorldMemory | null>) => {
      state.currentLocation = action.payload
    },
    clearError: (state) => {
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      // Get Characters
      .addCase(getCharacters.pending, (state) => {
        state.isLoading = true
      })
      .addCase(getCharacters.fulfilled, (state, action) => {
        state.isLoading = false
        state.characters = action.payload
      })
      .addCase(getCharacters.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to fetch characters'
      })
      // Get Plots
      .addCase(getPlots.fulfilled, (state, action) => {
        state.plots = action.payload
      })
      // Get Locations
      .addCase(getLocations.fulfilled, (state, action) => {
        state.locations = action.payload
      })
      // Create Character
      .addCase(createCharacter.pending, (state) => {
        state.isLoading = true
      })
      .addCase(createCharacter.fulfilled, (state) => {
        state.isLoading = false
        // Refresh characters list after creation
      })
      .addCase(createCharacter.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to create character'
      })
      // Update Character
      .addCase(updateCharacter.fulfilled, () => {
        // Refresh characters list after update
      })
      // Delete Character
      .addCase(deleteCharacter.fulfilled, (state, action) => {
        state.characters = state.characters.filter(
          char => char.characterId !== action.payload
        )
      })
      // Create Plot
      .addCase(createPlot.pending, (state) => {
        state.isLoading = true
      })
      .addCase(createPlot.fulfilled, (state) => {
        state.isLoading = false
        // Refresh plots list after creation
      })
      .addCase(createPlot.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.error.message || 'Failed to create plot'
      })
      // Update Plot
      .addCase(updatePlot.fulfilled, () => {
        // Refresh plots list after update
      })
      // Delete Plot
      .addCase(deletePlot.fulfilled, (state, action) => {
        state.plots = state.plots.filter(
          plot => plot.plotId !== action.payload
        )
      })
  },
})

export const { 
  setCurrentCharacter, 
  setCurrentPlot, 
  setCurrentLocation,
  clearError 
} = enhancedMemorySlice.actions

export default enhancedMemorySlice.reducer