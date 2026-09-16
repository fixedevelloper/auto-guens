package com.ussdauto.android.data.sms.parser

import com.google.common.truth.Truth.assertThat
import com.ussdauto.android.data.sms.config.OperatorSmsPatterns
import com.ussdauto.android.data.sms.config.SmsParserConfig
import com.ussdauto.android.domain.model.FailureReason
import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.SmsParseResult
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class OrangeSmsParserTest {

    private val config: SmsParserConfig = mock()
    private lateinit var parser: OrangeSmsParser

    @Before
    fun setUp() {
        val patterns = OperatorSmsPatterns(
            senders = listOf("ORANGE", "8300"),
            operationKeywords = mapOf(
                OperationType.DEPOSIT to Regex("d[ée]p[oô]t|rechargement", RegexOption.IGNORE_CASE),
                OperationType.WITHDRAW to Regex("retrait", RegexOption.IGNORE_CASE)
            ),
            successPattern = Regex(
                "(?is)(?<operation>d[ée]p[oô]t|retrait) de\\s+(?<montant>[0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:XAF|FCFA)\\s+effectu[ée]" +
                    ".*?(?:transaction|r[ée]f[ée]rence)\\s*(?:n[°o]?)?\\s*[:\\s]\\s*(?<reference>[A-Za-z0-9.\\-]+)"
            ),
            failurePattern = Regex("(?is)(?:[ée]chec|solde insuffisant|transaction annul[ée]e|refus[ée]e)")
        )
        whenever(config.forOperateur("ORANGE")).thenReturn(patterns)
        parser = OrangeSmsParser(config)
    }

    @Test
    fun `parses a successful DEPOSIT confirmation`() {
        val sms = "Depot de 3000 XAF effectue avec succes. Reference: OM12345"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("3000"))

        assertThat(result).isInstanceOf(SmsParseResult.Success::class.java)
        val success = result as SmsParseResult.Success
        assertThat(success.operatorReference).isEqualTo("OM12345")
        assertThat(success.confirmedAmount).isEqualTo(BigDecimal("3000"))
    }

    @Test
    fun `parses a successful WITHDRAW confirmation`() {
        val sms = "Retrait de 1500 XAF effectue avec succes. Reference: OM67890"

        val result = parser.parse(sms, OperationType.WITHDRAW, BigDecimal("1500"))

        assertThat(result).isInstanceOf(SmsParseResult.Success::class.java)
        val success = result as SmsParseResult.Success
        assertThat(success.operatorReference).isEqualTo("OM67890")
        assertThat(success.confirmedAmount).isEqualTo(BigDecimal("1500"))
    }

    @Test
    fun `reports an operator-side failure`() {
        val sms = "Echec: solde insuffisant sur votre compte Orange Money"

        val result = parser.parse(sms, OperationType.WITHDRAW, BigDecimal("1500"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.OPERATOR_ERROR_SMS)
    }

    @Test
    fun `reports unrecognized format for unrelated content`() {
        val sms = "Bienvenue chez Orange, decouvrez nos nouveaux forfaits"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("3000"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.UNRECOGNIZED_FORMAT)
    }

    @Test
    fun `reports a mismatch when the SMS confirms a different operation type`() {
        val sms = "Retrait de 3000 XAF effectue avec succes. Reference: OM12345"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("3000"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.OPERATION_TYPE_MISMATCH)
    }
}
