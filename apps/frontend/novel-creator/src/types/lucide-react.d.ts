declare module 'lucide-react' {
  import { FC, SVGProps } from 'react'
  
  export interface IconProps extends SVGProps<SVGSVGElement> {
    size?: string | number
    absoluteStrokeWidth?: boolean
  }
  
  export type Icon = FC<IconProps>
  
  export const Sparkles: Icon
  export const BookOpen: Icon
  export const Users: Icon
  export const MapPin: Icon
  export const Lightbulb: Icon
  export const PenTool: Icon
  export const RefreshCw: Icon
  export const ChevronRight: Icon
  export const Settings2: Icon
  export const Brain: Icon
  export const Target: Icon
  export const ChevronDown: Icon
  export const ChevronUp: Icon
  export const Edit: Icon
  export const Edit2: Icon
  export const Trash2: Icon
  export const MoreVertical: Icon
  export const Plus: Icon
  export const FileText: Icon
  export const Hash: Icon
  export const Search: Icon
  export const GitBranch: Icon
  export const Map: Icon
  export const PanelLeftClose: Icon
  export const PanelLeftOpen: Icon
  export const PanelRightClose: Icon
  export const PanelRightOpen: Icon
  export const Bold: Icon
  export const Italic: Icon
  export const Underline: Icon
  export const List: Icon
  export const ListOrdered: Icon
  export const Quote: Icon
  export const Undo: Icon
  export const Redo: Icon
  export const Type: Icon
  export const Moon: Icon
  export const Sun: Icon
  export const Maximize: Icon
  export const Maximize2: Icon
  export const Minimize: Icon
  export const Minimize2: Icon
  export const Save: Icon
  export const Download: Icon
  export const X: Icon
  export const Check: Icon
  export const Shield: Icon
  export const Zap: Icon
  export const TrendingUp: Icon
  export const AlertCircle: Icon
  export const MessageSquare: Icon
  export const Heading1: Icon
  export const Heading2: Icon
  export const Heading3: Icon
  export const Minus: Icon
  export const Eye: Icon
  export const EyeOff: Icon
  export const Settings: Icon
  export const User: Icon
  export const Heart: Icon
  export const Calendar: Icon
  export const CheckCircle: Icon
  export const Clock: Icon
  export const Flag: Icon
}