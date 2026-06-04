import {
  LayoutDashboard,
  Shield,
  CreditCard,
  GitBranch,
  TestTube2,
  Play,
  BrainCircuit,
  BarChart3,
  ClipboardList,
  Settings,
} from 'lucide-react'

export const MENU_ITEMS = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    path: '/dashboard',
    section: 'main',
    // all roles
  },
  {
    id: 'rules',
    label: 'Rules',
    icon: Shield,
    path: '/rules',
    section: 'management',
    // all roles
  },
  {
    id: 'transactions',
    label: 'Transactions',
    icon: CreditCard,
    path: '/transactions',
    section: 'management',
    // all roles
  },
  {
    id: 'scenarios',
    label: 'Scenarios',
    icon: GitBranch,
    path: '/scenarios',
    section: 'testing',
    // all roles
  },
  {
    id: 'testcases',
    label: 'Test Cases',
    icon: TestTube2,
    path: '/testcases',
    section: 'testing',
    // all roles
  },
  {
    id: 'executions',
    label: 'Executions',
    icon: Play,
    path: '/executions',
    section: 'testing',
    // all roles
  },
  {
    id: 'ai',
    label: 'AI Assistant',
    icon: BrainCircuit,
    path: '/ai',
    section: 'tools',
    requiredRoles: ['ADMIN', 'TESTER'],
  },
  {
    id: 'reports',
    label: 'Reports',
    icon: BarChart3,
    path: '/reports',
    section: 'analytics',
    // all roles
  },
  {
    id: 'audit-logs',
    label: 'Audit Logs',
    icon: ClipboardList,
    path: '/audit-logs',
    section: 'analytics',
    requiredRoles: ['ADMIN'],
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: Settings,
    path: '/settings',
    section: 'user',
    // all roles
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
