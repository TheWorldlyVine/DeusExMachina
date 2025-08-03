import { AuthAPI } from './auth';
import { DocumentAPI } from './document';
import { MemoryAPI } from './memory';
import { GenerationAPI } from './generation';

export interface DataSources {
  authAPI: AuthAPI;
  documentAPI: DocumentAPI;
  memoryAPI: MemoryAPI;
  generationAPI: GenerationAPI;
}

export function dataSources(): DataSources {
  return {
    authAPI: new AuthAPI(),
    documentAPI: new DocumentAPI(),
    memoryAPI: new MemoryAPI(),
    generationAPI: new GenerationAPI(),
  };
}