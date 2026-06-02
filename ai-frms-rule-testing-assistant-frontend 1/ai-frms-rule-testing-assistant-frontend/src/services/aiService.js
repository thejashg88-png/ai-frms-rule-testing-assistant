import aiApi from '../api/aiApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 1500) => new Promise((r) => setTimeout(r, ms))

const safeParseJson = (value) => {
  if (!value || typeof value !== 'string') return null
  try {
    return JSON.parse(value)
  } catch (e) {
    console.warn('[AI] Failed to parse rawAiResponse', e)
    return null
  }
}

// ── Amount normalization ──────────────────────────────────────────────────────
// Strips leading-zero strings like "00000150000" → 150000
// Falls back to action/requirement-based heuristic when raw is absent
const normalizeAmount = (raw, expectedAction, requirement) => {
  if (raw != null) {
    const str = String(raw).replace(/^0+(?=\d)/, '').replace(/[^0-9.]/g, '')
    const numeric = parseFloat(str)
    if (!isNaN(numeric) && numeric > 0) return numeric
  }
  const req = (requirement || '').toLowerCase()
  const aboveMatch = req.match(/above\s+([\d,]+)/i) || req.match(/over\s+([\d,]+)/i) || req.match(/exceeds?\s+([\d,]+)/i)
  const belowMatch = req.match(/below\s+([\d,]+)/i) || req.match(/under\s+([\d,]+)/i)
  if (aboveMatch) return Number(aboveMatch[1].replace(/,/g, '')) + 50000
  if (belowMatch) return Math.max(Number(belowMatch[1].replace(/,/g, '')) - 10000, 1000)
  if (expectedAction === 'REJECT')  return 200000
  if (expectedAction === 'MONITOR') return 150000
  return 50000
}

// Local keyword inference used when the AI service is unavailable or returns no usable data.
// This avoids a blank form — the user still gets a reasonable starting point.
const inferFromRequirement = (requirement) => {
  const req = (requirement || '').toLowerCase()

  let expectedAction = 'MONITOR'
  if (req.includes('reject') || req.includes('block') || req.includes('decline')) expectedAction = 'REJECT'
  else if (req.includes('accept') || req.includes('allow') || req.includes('pass')) expectedAction = 'ACCEPT'

  const riskLevel = expectedAction === 'REJECT' ? 'HIGH' : expectedAction === 'MONITOR' ? 'MEDIUM' : 'LOW'

  let ruleName = ''
  if (req.includes('single large') || req.includes('large transaction') || req.includes('large tx')) {
    ruleName = 'Single Large Transaction Rule'
  } else if (req.includes('velocity') || req.includes('frequency') || req.includes('repeated')) {
    ruleName = 'Velocity Rule'
  } else if (req.includes('geo') || req.includes('foreign') || req.includes('country') || req.includes('location')) {
    ruleName = 'Geographic Rule'
  }

  const amount     = normalizeAmount(null, expectedAction, requirement)
  const actionLabel = expectedAction === 'REJECT' ? 'Reject' : expectedAction === 'MONITOR' ? 'Monitor' : 'Accept'
  const name       = ruleName ? `${ruleName} — ${actionLabel} Case` : `Test Case — ${actionLabel} Expected`

  return {
    name,
    description: `Validate that transaction triggers ${expectedAction} action based on rule configuration.`,
    expectedAction,
    expectedRiskLevel: riskLevel,
    ruleName,
    amount,
    insight: `This test validates that a ${expectedAction.toLowerCase()} outcome occurs when the configured rule threshold is ${expectedAction === 'ACCEPT' ? 'not exceeded' : 'exceeded'}.`,
    notes:
      expectedAction === 'MONITOR' ? 'Transaction will be flagged for review without being blocked.' :
      expectedAction === 'REJECT'  ? 'Transaction will be declined by the rule engine.' :
                                     'Transaction should pass through without triggering any rule.',
  }
}

// ── Extract a single test case from any known AI response shape ───────────────
const normalizeAiTestCase = (apiResponse, requirement, existingContext = {}) => {
  let tc = null

  const topLevels = [apiResponse?.data, apiResponse, apiResponse?.data?.data]
  for (const lvl of topLevels) {
    if (!lvl) continue
    const arr = lvl.testCases || lvl.generatedTestCases
    if (Array.isArray(arr) && arr.length > 0) { tc = arr[0]; break }
    if (lvl.testCaseName || lvl.name || lvl.inputData) { tc = lvl; break }
  }

  if (!tc) {
    const rawStr = apiResponse?.data?.rawAiResponse || apiResponse?.rawAiResponse
    if (rawStr) {
      const parsed = safeParseJson(rawStr)
      const arr =
        parsed?.data?.testCases        || parsed?.testCases ||
        parsed?.data?.generatedTestCases || parsed?.generatedTestCases
      if (Array.isArray(arr) && arr.length > 0) tc = arr[0]
      else if (parsed?.testCaseName || parsed?.name) tc = parsed
    }
  }

  const inferred = inferFromRequirement(requirement)

  if (!tc) {
    return {
      name:             inferred.name,
      description:      inferred.description,
      status:           'ACTIVE',
      scenarioId:       existingContext.scenarioId   ?? '',
      scenarioName:     existingContext.scenarioName ?? '',
      ruleId:           existingContext.ruleId       ?? '',
      ruleName:         existingContext.ruleName     ?? inferred.ruleName,
      expectedResult:   'PASS',
      expectedAction:   inferred.expectedAction,
      expectedRiskLevel: inferred.expectedRiskLevel,
      inputData: {
        cardNumber:      '4111111111111111',
        amount:          inferred.amount,
        merchantId:      'MID00001',
        transactionType: 'PURCHASE',
        channel:         'ONLINE',
        country:         'IN',
        currency:        'INR',
      },
      _insight:  inferred.insight,
      _notes:    inferred.notes,
      _fallback: true,
    }
  }

  const expectedAction    = tc.expectedAction    || tc.expectedResult?.expectedAction    || inferred.expectedAction
  const expectedRiskLevel = tc.expectedRiskLevel || tc.expectedResult?.expectedRiskLevel || inferred.expectedRiskLevel

  return {
    name:             tc.testCaseName || tc.name || inferred.name,
    description:      tc.description  || inferred.description,
    status:           'ACTIVE',
    scenarioId:       existingContext.scenarioId   ?? '',
    scenarioName:     existingContext.scenarioName ?? '',
    ruleId:           existingContext.ruleId       ?? '',
    ruleName:         existingContext.ruleName ?? tc.ruleName ?? inferred.ruleName,
    expectedResult:   'PASS',
    expectedAction,
    expectedRiskLevel,
    inputData: {
      cardNumber:      tc.inputData?.cardNumber      || '4111111111111111',
      amount:          normalizeAmount(tc.inputData?.amount, expectedAction, requirement),
      merchantId:      tc.inputData?.merchantId      || tc.inputData?.mid || 'MID00001',
      transactionType: tc.inputData?.transactionType || 'PURCHASE',
      channel:         tc.inputData?.channel         || 'ONLINE',
      country:         tc.inputData?.country         || tc.inputData?.countryCode || 'IN',
      currency:        tc.inputData?.currency        || 'INR',
    },
    _insight:  apiResponse?.data?.message || apiResponse?.message || inferred.insight,
    _notes:    inferred.notes,
    _fallback: false,
  }
}

// ── AI Chat response normalizer ───────────────────────────────────────────────
const normalizeAiChatResponse = (response) => {
  const root =
    response?.data?.data ||
    response?.data ||
    response

  let extracted =
    root?.reply ||
    root?.answer ||
    root?.response ||
    root?.message ||
    response?.data?.reply ||
    response?.data?.answer ||
    response?.data?.response ||
    response?.data?.message ||
    'No AI reply returned.'

  // Backend AI service sometimes returns a JSON string in the reply field.
  // e.g. reply = '{"reply":"actual text","context":{}}' — unwrap it.
  if (typeof extracted === 'string' && extracted.trim().startsWith('{')) {
    try {
      const parsed = JSON.parse(extracted)
      extracted =
        parsed?.reply ||
        parsed?.answer ||
        parsed?.response ||
        parsed?.message ||
        parsed?.content ||
        extracted
    } catch (_) {
      // Not JSON — keep as-is
    }
  }

  const reply =
    typeof extracted === 'string'
      ? extracted
      : JSON.stringify(extracted, null, 2)

  return {
    reply,
    context: root?.context || {},
    raw: response,
  }
}

// ── Existing helpers (unchanged) ──────────────────────────────────────────────
const extractTestCases = (apiResponse) => {
  console.log('[extractTestCases input]', apiResponse)

  const directCandidates = [
    apiResponse?.data?.testCases,
    apiResponse?.data?.generatedTestCases,
    apiResponse?.testCases,
    apiResponse?.generatedTestCases,
    apiResponse?.data?.data?.testCases,
    apiResponse?.data?.data?.generatedTestCases,
  ]
  for (const candidate of directCandidates) {
    if (Array.isArray(candidate) && candidate.length > 0) return candidate
  }

  const rawCandidates = [
    apiResponse?.data?.rawAiResponse,
    apiResponse?.rawAiResponse,
    apiResponse?.data?.data?.rawAiResponse,
  ]
  for (const raw of rawCandidates) {
    const parsed = safeParseJson(raw)
    console.log('[AI Generate Test Cases Parsed rawAiResponse]', parsed)
    const parsedCandidates = [
      parsed?.data?.testCases,
      parsed?.data?.generatedTestCases,
      parsed?.testCases,
      parsed?.generatedTestCases,
      parsed?.data?.data?.testCases,
      parsed?.data?.data?.generatedTestCases,
    ]
    for (const candidate of parsedCandidates) {
      if (Array.isArray(candidate) && candidate.length > 0) return candidate
    }
  }

  return []
}

const normalizeArray = (value) => {
  if (Array.isArray(value)) return value
  if (typeof value === 'string' && value.trim()) return [value]
  if (value && typeof value === 'object') return Object.values(value).filter(Boolean)
  return []
}

const extractFailureAnalysisPayload = (apiResponse) => {
  const candidates = [
    apiResponse?.data,
    apiResponse?.data?.data,
    apiResponse,
    apiResponse?.payload,
  ]
  for (const item of candidates) {
    if (
      item &&
      (
        item.summary        ||
        item.rootCause      ||
        item.suggestions    ||
        item.possibleReasons ||
        item.debuggingSteps ||
        item.recommendedFix ||
        item.riskImpact     ||
        item.rawAiResponse
      )
    ) {
      if (item.rawAiResponse) {
        const parsed = safeParseJson(item.rawAiResponse)
        if (parsed?.data) return parsed.data
        if (parsed) return parsed
      }
      return item
    }
  }
  return {}
}

const normalizeExplainRuleResponse = (apiResponse) => {
  const data = apiResponse?.data || apiResponse

  console.log('[normalizeExplainRuleResponse input]', apiResponse)
  console.log('[normalizeExplainRuleResponse extracted data]', data)

  return {
    explanation:        data?.explanation      || data?.summary          || 'No explanation available.',
    summary:            data?.summary          || data?.explanation      || 'No summary available.',
    businessMeaning:    data?.businessMeaning  || 'No business meaning available.',
    technicalMeaning:   data?.technicalMeaning || 'No technical meaning available.',
    exampleScenario:    data?.exampleScenario  || 'No example scenario available.',
    riskLevel:          data?.riskLevel        || 'MEDIUM',
    recommendations:    normalizeArray(data?.recommendations || data?.riskNotes),
    riskNotes:          normalizeArray(data?.riskNotes       || data?.recommendations),
    edgeCases:          normalizeArray(data?.edgeCases),
    suggestedTestCases: normalizeArray(data?.suggestedTestCases),
  }
}

const MOCK_EXPLANATIONS = {
  CREDIT: 'This rule evaluates the customer\'s credit utilization. When the transaction amount exceeds the configured maxAmount threshold or when combined with existing credit usage surpasses the percentageThreshold, the rule triggers a REJECT action to prevent over-limit transactions.',
  AMOUNT: 'This rule monitors transaction amounts for compliance review. Transactions exceeding the configured maxAmount are flagged for MONITOR action, notifying the compliance team without blocking the transaction flow.',
  VELOCITY: 'This velocity rule tracks the frequency of card usage within a rolling time window. If the number of transactions (txnCount) within the configured frequency window exceeds the threshold, it indicates potential card fraud and triggers a REJECT.',
  GEO: 'Geographic mismatch detection compares the card\'s issuing country with the merchant\'s location. A high percentageThreshold mismatch triggers a MONITOR action for manual review by the fraud team.',
  FRAUD: 'Fraud detection rules identify duplicate or suspicious transactions within a session window. Duplicate transactions with matching amount and merchant within the frequency window are rejected to prevent double-billing and fraud.',
}

export const aiService = {
  // ── Generate a single test case from a plain-English requirement ────────────
  generateSingleTestCase: async (requirement, context = {}) => {
    console.log('[AI Test Case Requirement]', requirement)

    if (isMock) {
      await delay(1500)
      const inferred = inferFromRequirement(requirement)
      const normalized = {
        name:             inferred.name,
        description:      inferred.description,
        status:           'ACTIVE',
        scenarioId:       context.scenarioId   ?? '',
        scenarioName:     context.scenarioName ?? '',
        ruleId:           context.ruleId       ?? '',
        ruleName:         context.ruleName     ?? inferred.ruleName,
        expectedResult:   'PASS',
        expectedAction:   inferred.expectedAction,
        expectedRiskLevel: inferred.expectedRiskLevel,
        inputData: {
          cardNumber:      '4111111111111111',
          amount:          inferred.amount,
          merchantId:      'MID00001',
          transactionType: 'PURCHASE',
          channel:         'ONLINE',
          country:         'IN',
          currency:        'INR',
        },
        _insight:  inferred.insight,
        _notes:    inferred.notes,
        _fallback: false,
      }
      console.log('[AI Test Case Normalized]', normalized)
      return normalized
    }

    try {
      const payload = {
        ruleId:      context.ruleId   || undefined,
        ruleName:    context.ruleName || undefined,
        ruleType:    context.ruleType || undefined,
        requirement,
      }
      const apiResponse = await aiApi.generateTestCases(payload)
      console.log('[AI Test Case Raw Response]', apiResponse)
      if (apiResponse?.success === false) {
        throw new Error(apiResponse?.message || 'AI service returned an error.')
      }
      const normalized = normalizeAiTestCase(apiResponse, requirement, context)
      console.log('[AI Test Case Normalized]', normalized)
      return normalized
    } catch (err) {
      console.warn('[AI generateSingleTestCase] API failed, using local fallback:', err.message)
      const inferred = inferFromRequirement(requirement)
      const fallback = {
        name:             inferred.name,
        description:      inferred.description,
        status:           'ACTIVE',
        scenarioId:       context.scenarioId   ?? '',
        scenarioName:     context.scenarioName ?? '',
        ruleId:           context.ruleId       ?? '',
        ruleName:         context.ruleName     ?? inferred.ruleName,
        expectedResult:   'PASS',
        expectedAction:   inferred.expectedAction,
        expectedRiskLevel: inferred.expectedRiskLevel,
        inputData: {
          cardNumber:      '4111111111111111',
          amount:          inferred.amount,
          merchantId:      'MID00001',
          transactionType: 'PURCHASE',
          channel:         'ONLINE',
          country:         'IN',
          currency:        'INR',
        },
        _insight:  inferred.insight,
        _notes:    inferred.notes,
        _fallback: true,
      }
      console.log('[AI Test Case Normalized (fallback)]', fallback)
      return fallback
    }
  },

  generateTestCases: async (payload) => {
    const { ruleId, ruleName, ruleType } = payload ?? {}
    if (isMock) {
      await delay(2000)
      return {
        ruleId,
        ruleName,
        generatedTestCases: [
          {
            name: `${ruleName} — boundary ACCEPT case`,
            description: 'AI-generated: Tests the exact boundary condition that should pass',
            expectedResult: 'PASS',
            expectedAction: 'ACCEPT',
            inputData: { amount: 9999, cardNumber: '4532015112830366', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US' },
          },
          {
            name: `${ruleName} — threshold breach REJECT case`,
            description: 'AI-generated: Tests the condition that should trigger rule rejection',
            expectedResult: 'FAIL',
            expectedAction: 'REJECT',
            inputData: { amount: 10001, cardNumber: '4532015112830366', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US' },
          },
          {
            name: `${ruleName} — edge case MONITOR`,
            description: 'AI-generated: Tests the monitor threshold boundary',
            expectedResult: 'PASS',
            expectedAction: 'MONITOR',
            inputData: { amount: 10000, cardNumber: '5425233430109903', merchantId: 'M002', transactionType: 'PURCHASE', channel: 'POS', country: 'US' },
          },
        ],
        explanation: `Based on the ${ruleType} rule parameters, I identified 3 critical test cases covering the accept boundary, reject threshold, and monitor edge case. These cover the most common failure scenarios for ${ruleType} rules in a payment processing system.`,
      }
    }
    try {
      const apiResponse = await aiApi.generateTestCases(payload)
      console.log('[AI Generate Test Cases Raw Response]', apiResponse)
      console.log('[AI Generate Test Cases Response Data JSON]', JSON.stringify(apiResponse?.data, null, 2))
      if (apiResponse?.success === false) {
        throw new Error(apiResponse?.message || 'AI service returned an error.')
      }
      const testCases = extractTestCases(apiResponse)
      console.log('[AI Generate Test Cases Extracted List]', testCases)
      return {
        ruleType:    apiResponse?.data?.ruleType || apiResponse?.ruleType || payload.ruleType,
        explanation: apiResponse?.data?.message  || apiResponse?.message  || 'Test cases generated successfully',
        testCases,
      }
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  explainRule: async (rule) => {
    if (isMock) {
      await delay(1500)
      const base = MOCK_EXPLANATIONS[rule?.ruleType] ?? 'This rule applies business logic to validate payment transactions against configured thresholds.'
      return {
        explanation: base,
        recommendations: [
          'Review the threshold values quarterly to align with changing fraud patterns.',
          `Consider adding ${rule?.ruleType === 'VELOCITY' ? 'time-of-day' : 'merchant category'} conditions for more targeted detection.`,
          'Set up alerts when this rule triggers more than 5% of transactions — that may indicate a configuration issue.',
        ],
        riskLevel: rule?.action === 'REJECT' ? 'HIGH' : 'MEDIUM',
      }
    }
    try {
      const payload = {
        ruleId:              rule?.id,
        ruleName:            rule?.ruleName     || rule?.name        || rule?.title || '',
        ruleType:            rule?.ruleType,
        action:              rule?.action       || 'MONITOR',
        maxAmount:           rule?.maxAmount    ?? null,
        txnAmount:           rule?.txnAmount    ?? null,
        txnCount:            rule?.txnCount     ?? null,
        frequency:           rule?.frequency    ?? null,
        percentageThreshold: rule?.percentageThreshold ?? null,
        description:         rule?.ruleDescription || rule?.description || '',
      }
      console.log('[AI Explain Rule Payload]', payload)
      const apiResponse = await aiApi.explainRule(payload)
      console.log('[aiService.explainRule apiResponse]', apiResponse)
      if (apiResponse?.success === false) {
        throw new Error(apiResponse?.message || 'AI service returned an error.')
      }
      const normalized = normalizeExplainRuleResponse(apiResponse)
      console.log('[AI Explain normalized]', normalized)
      return normalized
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  // Sends a failed execution's details to the AI for root cause analysis.
  // ruleType must be extracted from the actual execution result — never hardcoded —
  // because different rule types produce different failure patterns.
  analyzeFailure: async (payload) => {
    if (isMock) {
      await delay(2000)
      return {
        summary: 'The test case failed because the rule returned a different action than expected.',
        rootCause: payload?.executionLogs || 'Expected action did not match the rule engine output. The configured threshold values may need adjustment.',
        suggestions: [
          'Check the rule\'s maxAmount and percentageThreshold — they may be set too conservatively.',
          'Review the test input data to confirm the expected result is actually valid for these parameters.',
          'Run the rule with borderline values to find the exact decision boundary.',
        ],
        possibleReasons: [],
        debuggingSteps: [],
        recommendedFix: 'Review and adjust the rule threshold parameters.',
        riskImpact: 'Medium risk — test coverage gap identified.',
        confidence: 87,
      }
    }
    try {
      const apiResponse = await aiApi.analyzeFailure(payload)
      console.log('[AI Failure Analysis Raw Response]', apiResponse)
      console.log('[AI Failure Analysis Response Data JSON]', JSON.stringify(apiResponse?.data || apiResponse, null, 2))
      if (apiResponse?.success === false) {
        throw new Error(apiResponse?.message || 'AI service returned an error.')
      }
      const data = extractFailureAnalysisPayload(apiResponse)
      const normalized = {
        summary:         data.summary        || data.riskImpact                                         || 'No failure summary available.',
        rootCause:       data.rootCause      || data.recommendedFix || normalizeArray(data.possibleReasons)[0] || 'No root cause identified.',
        suggestions:     normalizeArray(data.suggestions || data.recommendations || data.debuggingSteps || data.possibleReasons),
        possibleReasons: normalizeArray(data.possibleReasons),
        debuggingSteps:  normalizeArray(data.debuggingSteps),
        recommendedFix:  data.recommendedFix || 'No recommended fix available.',
        riskImpact:      data.riskImpact     || 'No risk impact available.',
        confidence:      data.confidence     || data.confidenceScore || 80,
      }
      console.log('[AI Failure Analysis Normalized]', normalized)
      return normalized
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  generateTransaction: async (hints = {}) => {
    if (isMock) {
      await delay(1000)
      const types = ['PURCHASE', 'REFUND', 'WITHDRAWAL', 'TRANSFER']
      const channels = ['ONLINE', 'POS', 'ATM', 'MOBILE']
      const countries = ['US', 'GB', 'FR', 'DE', 'IN', 'SG']
      const merchants = [
        { id: 'M001', name: 'Amazon Inc', category: 'E-COMMERCE' },
        { id: 'M002', name: 'Shell Station', category: 'FUEL' },
        { id: 'M003', name: 'Walmart', category: 'RETAIL' },
        { id: 'M004', name: 'Paris Boutique', category: 'LUXURY' },
      ]
      const m = merchants[Math.floor(Math.random() * merchants.length)]
      return {
        cardNumber: `4${Math.floor(Math.random() * 1e15).toString().padStart(15, '0')}`,
        amount: +(Math.random() * (hints.maxAmount ?? 5000) + 10).toFixed(2),
        currency: hints.currency ?? 'USD',
        merchantId: m.id,
        merchantName: m.name,
        merchantCategory: m.category,
        transactionType: hints.transactionType ?? types[Math.floor(Math.random() * types.length)],
        channel: hints.channel ?? channels[Math.floor(Math.random() * channels.length)],
        country: hints.country ?? countries[Math.floor(Math.random() * countries.length)],
        explanation: 'AI-generated transaction based on your specified parameters and typical fraud pattern distributions.',
      }
    }
    try {
      const resp = await aiApi.generateTransaction(hints)
      return resp?.data ?? resp
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  generateRule: async (requirement) => {
    if (isMock) {
      await delay(2000)
      return {
        ruleName: 'High Frequency Transaction Monitor',
        ruleDescription: 'Monitors accounts making more than 3 transactions below ₹50,000 within a 24-hour window to detect potential structuring activity.',
        ruleType: 'VELOCITY',
        action: 'MONITOR',
        status: 'ACTIVE',
        txnCount: 3,
        txnAmount: '000000050000',
        frequency: 24,
        maxAmount: null,
        percentageThreshold: null,
        explanation: 'This rule targets transaction structuring behavior where amounts are deliberately kept below reporting thresholds. Monitoring frequent sub-threshold transactions helps compliance teams identify potential money laundering and structuring patterns before they escalate.',
        riskNotes: [
          'Cross-reference with customer risk profile before escalating a flagged account.',
          'High false-positive rate expected for retail merchants — tune txnCount based on observed baselines.',
          'Align the 24-hour frequency window with your regulatory reporting cycle for accurate SARs.',
        ],
      }
    }
    try {
      const resp = await aiApi.generateRule(requirement)
      return resp?.data ?? resp
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  // Sends a plain-language message to the AI chat endpoint.
  // The UI renders only result.reply (plain text); never the full JSON response.
  // normalizeAiChatResponse handles cases where the backend stores FastAPI's JSON response
  // as a raw string inside AiChatResponse.reply.
  chat: async ({ message, context = {} }) => {
    const payload = { message, context }
    if (isMock) {
      await delay(1500)
      return normalizeAiChatResponse({
        data: {
          reply: `Based on your FRMS configuration, here is my analysis:\n\n**Regarding "${message}"**\n\nIn a payment rule testing context, this typically involves validating the rule engine's response against expected outcomes. I recommend checking threshold values, reviewing the transaction input data for edge cases, and ensuring the expected action aligns with your fraud risk policy.\n\nWould you like me to generate specific test cases or explain any rule in detail?`,
        },
      })
    }
    try {
      const response = await aiApi.chat(payload)
      return normalizeAiChatResponse(response)
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default aiService
