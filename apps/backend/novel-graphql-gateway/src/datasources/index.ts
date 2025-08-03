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
  return {
    authAPI: new AuthAPI({ cache, context }),
    documentAPI: new DocumentAPI({ cache, context }),
    memoryAPI: new MemoryAPI({ cache, context }),
    generationAPI: new GenerationAPI({ cache, context }),
  };
}