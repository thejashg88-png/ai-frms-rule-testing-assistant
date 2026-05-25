import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'

import AuthLayout from '../layouts/AuthLayout'
import MainLayout from '../layouts/MainLayout'
import ProtectedRoute from './ProtectedRoute'

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

          {/* Rules */}
          <Route path="/rules"                element={<RulesPage />} />
          <Route path="/rules/create"         element={<CreateRulePage />} />
          <Route path="/rules/:id/edit"       element={<EditRulePage />} />

          {/* Transactions */}
          <Route path="/transactions"          element={<TransactionsPage />} />
          <Route path="/transactions/create"   element={<CreateTransactionPage />} />
          <Route path="/transactions/:id"      element={<TransactionDetailsPage />} />

          {/* Scenarios */}
          <Route path="/scenarios"             element={<ScenariosPage />} />
          <Route path="/scenarios/create"      element={<CreateScenarioPage />} />
          <Route path="/scenarios/:id"         element={<ScenarioDetailsPage />} />
          <Route path="/scenarios/:id/edit"    element={<EditScenarioPage />} />

          {/* Test Cases */}
          <Route path="/testcases"             element={<TestCasesPage />} />
          <Route path="/testcases/create"      element={<CreateTestCasePage />} />
          <Route path="/testcases/:id"         element={<TestCaseDetailsPage />} />
          <Route path="/testcases/:id/edit"    element={<EditTestCasePage />} />

          {/* Executions */}
          <Route path="/executions"            element={<ExecutionsPage />} />
          <Route path="/executions/run"        element={<RunExecutionPage />} />
          <Route path="/executions/:id"        element={<ExecutionDetailsPage />} />

          {/* AI Assistant */}
          <Route path="/ai"                    element={<AiAssistantPage />} />

          {/* Reports */}
          <Route path="/reports"               element={<ReportsPage />} />

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
