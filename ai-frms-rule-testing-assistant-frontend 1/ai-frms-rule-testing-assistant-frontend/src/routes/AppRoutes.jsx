import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'

import AuthLayout from '../layouts/AuthLayout'
import MainLayout from '../layouts/MainLayout'
import ProtectedRoute, { RoleGuard } from './ProtectedRoute'

import LoginPage  from '../pages/auth/LoginPage'
import SignUpPage from '../pages/auth/SignUpPage'
import DashboardPage from '../pages/dashboard/DashboardPage'

import RulesPage from '../pages/rules/RulesPage'
import CreateRulePage from '../pages/rules/CreateRulePage'
import EditRulePage from '../pages/rules/EditRulePage'

import TransactionsPage from '../pages/transactions/TransactionsPage'
import CreateTransactionPage from '../pages/transactions/CreateTransactionPage'
import TransactionDetailsPage from '../pages/transactions/TransactionDetailsPage'

import ScenariosPage from '../pages/scenarios/ScenariosPage'
import CreateScenarioPage from '../pages/scenarios/CreateScenarioPage'
import EditScenarioPage from '../pages/scenarios/EditScenarioPage'
import ScenarioDetailsPage from '../pages/scenarios/ScenarioDetailsPage'

import TestCasesPage from '../pages/testcases/TestCasesPage'
import CreateTestCasePage from '../pages/testcases/CreateTestCasePage'
import EditTestCasePage from '../pages/testcases/EditTestCasePage'
import TestCaseDetailsPage from '../pages/testcases/TestCaseDetailsPage'

import ExecutionsPage from '../pages/executions/ExecutionsPage'
import RunExecutionPage from '../pages/executions/RunExecutionPage'
import ExecutionDetailsPage from '../pages/executions/ExecutionDetailsPage'

import AiAssistantPage from '../pages/ai/AiAssistantPage'

import ReportsPage from '../pages/reports/ReportsPage'

import AuditLogsPage from '../pages/audit/AuditLogsPage'

import SettingsPage from '../pages/settings/SettingsPage'

import NotFoundPage from '../pages/error/NotFoundPage'
import ServerErrorPage from '../pages/error/ServerErrorPage'

const AppRoutes = () => {
  return (
    <Routes>
      {/* ── Public routes ── */}
      <Route element={<AuthLayout />}>
        <Route path="/login"  element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
      </Route>

      {/* ── Protected routes ── */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Rules — view: all; create/edit: ADMIN only */}
          <Route path="/rules"                element={<RulesPage />} />
          <Route path="/rules/create"         element={<RoleGuard roles={['ADMIN']}><CreateRulePage /></RoleGuard>} />
          <Route path="/rules/:id/edit"       element={<RoleGuard roles={['ADMIN']}><EditRulePage /></RoleGuard>} />

          {/* Transactions — view: all; create: ADMIN only */}
          <Route path="/transactions"          element={<TransactionsPage />} />
          <Route path="/transactions/create"   element={<RoleGuard roles={['ADMIN']}><CreateTransactionPage /></RoleGuard>} />
          <Route path="/transactions/:id"      element={<TransactionDetailsPage />} />

          {/* Scenarios — view: all; create/edit: ADMIN only */}
          <Route path="/scenarios"             element={<ScenariosPage />} />
          <Route path="/scenarios/create"      element={<RoleGuard roles={['ADMIN']}><CreateScenarioPage /></RoleGuard>} />
          <Route path="/scenarios/:id"         element={<ScenarioDetailsPage />} />
          <Route path="/scenarios/:id/edit"    element={<RoleGuard roles={['ADMIN']}><EditScenarioPage /></RoleGuard>} />

          {/* Test Cases — view: all; create/edit: ADMIN+TESTER; delete page: ADMIN only */}
          <Route path="/testcases"             element={<TestCasesPage />} />
          <Route path="/testcases/create"      element={<RoleGuard roles={['ADMIN','TESTER']}><CreateTestCasePage /></RoleGuard>} />
          <Route path="/testcases/:id"         element={<TestCaseDetailsPage />} />
          <Route path="/testcases/:id/edit"    element={<RoleGuard roles={['ADMIN','TESTER']}><EditTestCasePage /></RoleGuard>} />

          {/* Executions — view: all; run: ADMIN+TESTER only */}
          <Route path="/executions"            element={<ExecutionsPage />} />
          <Route path="/executions/run"        element={<RoleGuard roles={['ADMIN','TESTER']}><RunExecutionPage /></RoleGuard>} />
          <Route path="/executions/:id"        element={<ExecutionDetailsPage />} />

          {/* AI Assistant — ADMIN+TESTER only */}
          <Route path="/ai"                    element={<RoleGuard roles={['ADMIN','TESTER']}><AiAssistantPage /></RoleGuard>} />

          {/* Reports — all can view */}
          <Route path="/reports"               element={<ReportsPage />} />

          {/* Audit Logs — ADMIN only */}
          <Route path="/audit-logs"            element={<RoleGuard roles={['ADMIN']}><AuditLogsPage /></RoleGuard>} />

          {/* Settings */}
          <Route path="/settings"              element={<SettingsPage />} />
        </Route>
      </Route>

      {/* ── Root redirect ── */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* ── Error pages ── */}
      <Route path="/500" element={<ServerErrorPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default AppRoutes
