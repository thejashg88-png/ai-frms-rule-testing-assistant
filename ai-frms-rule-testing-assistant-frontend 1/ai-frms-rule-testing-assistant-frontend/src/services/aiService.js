import aiApi from '../api/aiApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 1500) => new Promise((r) => setTimeout(r, ms))

const MOCK_EXPLANATIONS = {
  CREDIT: 'This rule evaluates the customer\'s credit utilization. When the transaction amount exceeds the configured maxAmount threshold or when combined with existing credit usage surpasses the percentageThreshold, the rule triggers a REJECT action to prevent over-limit transactions.',
  AMOUNT: 'This rule monitors transaction amounts for compliance review. Transactions exceeding the configured maxAmount are flagged for MONITOR action, notifying the compliance team without blocking the transaction flow.',
  VELOCITY: 'This velocity rule tracks the frequency of card usage within a rolling time window. If the number of transactions (txnCount) within the configured frequency window exceeds the threshold, it indicates potential card fraud and triggers a REJECT.',
  GEO: 'Geographic mismatch detection compares the card\'s issuing country with the merchant\'s location. A high percentageThreshold mismatch triggers a MONITOR action for manual review by the fraud team.',
  FRAUD: 'Fraud detection rules identify duplicate or suspicious transactions within a session window. Duplicate transactions with matching amount and merchant within the frequency window are rejected to prevent double-billing and fraud.',
}

export const aiService = {
  generateTestCases: async (ruleId, ruleName, ruleType) => {
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
    try { return await aiApi.generateTestCases({ ruleId, ruleName, ruleType }) }
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
    try { return await aiApi.explainRule(rule?.id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  analyzeFailure: async (execution) => {
    if (isMock) {
      await delay(2000)
      return {
        summary: 'The test case failed because the rule returned a different action than expected.',
        rootCause: execution?.failureReason ?? 'Expected action did not match the rule engine output. The configured threshold values may need adjustment.',
        suggestions: [
          'Check the rule\'s maxAmount and percentageThreshold — they may be set too conservatively.',
          'Review the test input data to confirm the expected result is actually valid for these parameters.',
          'Run the rule with borderline values to find the exact decision boundary.',
        ],
        confidence: 0.87,
      }
    }
    try { return await aiApi.analyzeFailure(execution?.id) }
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
    try { return await aiApi.generateTransaction(hints) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  chat: async (message, context = {}) => {
    if (isMock) {
      await delay(1500)
      return {
        reply: `Based on your FRMS configuration, here is my analysis:\n\n**Regarding "${message}"**\n\nIn a payment rule testing context, this typically involves validating the rule engine's response against expected outcomes. I recommend checking threshold values, reviewing the transaction input data for edge cases, and ensuring the expected action aligns with your fraud risk policy.\n\nWould you like me to generate specific test cases or explain any rule in detail?`,
        context: {},
      }
    }
    try { return await aiApi.chat(message, context) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default aiService
