import {
  LayoutDashboard,
  Shield,
  CreditCard,
  GitBranch,
  TestTube2,
  Play,
  BrainCircuit,
  BarChart3,
  Settings,
} from 'lucide-react'

export const MENU_ITEMS = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    path: '/dashboard',
    section: 'main',
  },
  {
    id: 'rules',
    label: 'Rules',
    icon: Shield,
    path: '/rules',
    section: 'management',
  },
  {
    id: 'transactions',
    label: 'Transactions',
    icon: CreditCard,
    path: '/transactions',
    section: 'management',
  },
  {
    id: 'scenarios',
    label: 'Scenarios',
    icon: GitBranch,
    path: '/scenarios',
    section: 'testing',
  },
  {
    id: 'testcases',
    label: 'Test Cases',
    icon: TestTube2,
    path: '/testcases',
    section: 'testing',
  },
  {
    id: 'executions',
    label: 'Executions',
    icon: Play,
    path: '/executions',
    section: 'testing',
  },
  {
    id: 'ai',
    label: 'AI Assistant',
    icon: BrainCircuit,
    path: '/ai',
    section: 'tools',
  },
  {
    id: 'reports',
    label: 'Reports',
    icon: BarChart3,
    path: '/reports',
    section: 'analytics',
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: Settings,
    path: '/settings',
    section: 'user',
  },
]

export const MENU_SECTIONS = {
  main:       'Main',
  management: 'Rule Management',
  testing:    'Testing',
  tools:      'Tools',
  analytics:  'Analytics',
  user:       'User',
}

export default MENU_ITEMS
