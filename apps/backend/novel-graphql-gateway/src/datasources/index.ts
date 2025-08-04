import { AuthAPI } from './auth';
import { DocumentAPI } from './document';
import { MemoryAPI } from './memory';
import { GenerationAPI } from './generation';
import { Context } from '../context';

export interface DataSources {
  authAPI: AuthAPI;
  documentAPI: DocumentAPI;
  memoryAPI: MemoryAPI;
  generationAPI: GenerationAPI;
}

export function dataSources(cache: any, context: Context): DataSources {
  // Apollo Server 4 doesn't pass cache the same way as v3
  // For now, don't use cache in datasources
  return {
    authAPI: new AuthAPI({ context }),
    documentAPI: new DocumentAPI({ context }),
    memoryAPI: new MemoryAPI({ context }),
    generationAPI: new GenerationAPI({ context }),
  };
}