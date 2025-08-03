import { Context } from '../context';
import { DataSources } from '../datasources';

export interface ResolverContext extends Context {
  dataSources: DataSources;
}