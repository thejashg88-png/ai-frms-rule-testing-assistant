// Shared rule-type field configuration used by RuleForm, TestCaseForm, and AI generators.
// ruleFields       — parameter fields shown in Create/Edit Rule form
// testCaseFields   — transaction input fields shown in Create/Edit Test Case form
// requiredRuleFields — ruleFields that must be non-empty when this ruleType is selected
// hint             — one-line guidance shown below the rule type selector

export const RULE_FIELD_CONFIG = {
  STRUCTURING: {
    ruleFields:         ['txnCount', 'txnAmount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'txnAmount', 'frequency'],
    description:        'Depends on multiple small transactions below threshold within time window',
    hint:               'Required: Txn Count, Txn Amount, and Frequency. Detects transactions structured to avoid reporting thresholds.',
  },

  UNUSUAL_AMT: {
    ruleFields:         ['percentageThreshold'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['percentageThreshold'],
    description:        "Depends on transaction amount compared with customer's historical average",
    hint:               'Required: Percentage Threshold. Needs previous transaction history for same card/customer.',
  },

  HIGH_FREQ_TXN: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'frequency'],
    description:        'Depends on number of transactions within a time window',
    hint:               'Required: Txn Count and Frequency. Needs multiple transactions in the selected time window.',
  },

  SEQUENTIAL_TXN: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'frequency'],
    description:        'Depends on repeated sequential transactions from same card/device/customer',
    hint:               'Required: Txn Count and Frequency. Needs sequential/repeated transactions for same card/device.',
  },

  ROUND_AMT_TXN: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'frequency'],
    description:        'Depends on repeated round amount transactions',
    hint:               'Required: Txn Count and Frequency. Use round amounts (e.g. 1000, 5000) in test input.',
  },

  TXN_VELOCITY: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'frequency'],
    description:        'Depends on transaction velocity/count within a short time',
    hint:               'Required: Txn Count and Frequency. Detects high-velocity transactions in a short window.',
  },

  ABNORMAL_HOUR: {
    ruleFields:         ['frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency', 'transactionTime'],
    requiredRuleFields: ['frequency'],
    description:        'Depends on transaction time/hour',
    hint:               'Required: Frequency. Test should include a transaction time during an abnormal hour (e.g. 02:00).',
  },

  INCONSISTENT_MCC: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'mccCode', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount', 'frequency'],
    description:        'Depends on MCC changes/inconsistency for same card/customer',
    hint:               'Required: Txn Count and Frequency. Test should include an MCC code.',
  },

  SINGLE_LARGE_TX: {
    ruleFields:         ['maxAmount'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['maxAmount'],
    description:        'Depends only on transaction amount crossing maxAmount',
    hint:               'Required: Max Amount. Test passes when amount is above maxAmount and expected action matches rule action.',
  },

  DAILY_TXN_VALUE: {
    ruleFields:         ['maxAmount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['maxAmount'],
    description:        'Depends on daily transaction value/limit',
    hint:               'Required: Max Amount. Frequency is optional to define the daily evaluation window.',
  },

  MONTHLY_TXN_VOLUME: {
    ruleFields:         ['txnCount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['txnCount'],
    description:        'Depends on monthly transaction count/volume',
    hint:               'Required: Txn Count. Detects accounts exceeding monthly transaction volume.',
  },

  ANNUAL_TXN_VALUE: {
    ruleFields:         ['maxAmount', 'frequency'],
    testCaseFields:     ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency'],
    requiredRuleFields: ['maxAmount'],
    description:        'Depends on annual transaction value crossing limit',
    hint:               'Required: Max Amount. Detects accounts exceeding annual transaction value thresholds.',
  },
}

export const ALL_RULE_FIELDS = ['txnCount', 'maxAmount', 'txnAmount', 'frequency', 'percentageThreshold']
export const ALL_TEST_CASE_FIELDS = ['amount', 'cardNumber', 'merchantId', 'transactionType', 'channel', 'countryCode', 'currency', 'transactionTime', 'mccCode']

export const getRuleFieldConfig = (ruleType) => RULE_FIELD_CONFIG[ruleType] ?? null

// Returns true when field should be visible for the given ruleType.
// When ruleType is empty or unknown, shows ALL fields (no filtering).
export const shouldShowRuleField = (ruleType, fieldName) => {
  if (!ruleType) return true
  const config = RULE_FIELD_CONFIG[ruleType]
  if (!config) return true
  return config.ruleFields.includes(fieldName)
}

export const shouldShowTestCaseField = (ruleType, fieldName) => {
  if (!ruleType) return true
  const config = RULE_FIELD_CONFIG[ruleType]
  if (!config) return true
  return config.testCaseFields.includes(fieldName)
}

export const isRuleFieldRequired = (ruleType, fieldName) => {
  if (!ruleType) return false
  const config = RULE_FIELD_CONFIG[ruleType]
  if (!config) return false
  return config.requiredRuleFields.includes(fieldName)
}
