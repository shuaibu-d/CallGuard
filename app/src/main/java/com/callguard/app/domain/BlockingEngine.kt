package com.callguard.app.domain

import com.callguard.app.data.MatchType
import com.callguard.app.data.RuleEntity

sealed class BlockingDecision {
    data object Allow : BlockingDecision()
    data class Block(val ruleId: Long) : BlockingDecision()
}

object PhoneNumberNormalizer {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val digits = trimmed.filter { it.isDigit() }
        return when {
            trimmed.startsWith("+") -> "+$digits"
            digits.startsWith("234") && digits.length >= 13 -> "+$digits"
            digits.startsWith("0") -> "+234" + digits.drop(1)
            else -> digits
        }
    }

    fun candidates(raw: String): List<String> {
        val n = normalize(raw)
        if (n.isEmpty()) return emptyList()
        val local = if (n.startsWith("+234")) "0" + n.drop(4) else n
        return listOf(n, local, raw.trim())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}

class BlockingEngine {
    fun evaluate(rawNumber: String, rules: List<RuleEntity>): BlockingDecision {
        val candidates = PhoneNumberNormalizer.candidates(rawNumber)
        for (rule in rules) {
            if (!rule.enabled) continue
            val pattern = rule.pattern.trim()
            if (pattern.isEmpty()) continue

            val matched = candidates.any { value ->
                when (rule.matchType) {
                    MatchType.PREFIX -> value.startsWith(pattern)
                    MatchType.EXACT -> value == pattern
                    MatchType.CONTAINS -> value.contains(pattern)
                }
            }

            if (matched) return BlockingDecision.Block(rule.id)
        }
        return BlockingDecision.Allow
    }
}
