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

/**
 * Les regex utilisées ici reproduisent celles de res/raw/sms_parser_patterns.json pour
 * l'opérateur MTN — SmsParserConfig lui-même (chargement du fichier via les resources
 * Android) n'est pas exercé, seule la logique de rapprochement dans RegexBasedSmsParser l'est.
 */
class MtnSmsParserTest {

    private val config: SmsParserConfig = mock()
    private lateinit var parser: MtnSmsParser

    @Before
    fun setUp() {
        val patterns = OperatorSmsPatterns(
            senders = listOf("MTN", "36160"),
            operationKeywords = mapOf(
                OperationType.DEPOSIT to Regex("d[ée]p[oô]t", RegexOption.IGNORE_CASE),
                OperationType.WITHDRAW to Regex("retrait", RegexOption.IGNORE_CASE)
            ),
            successPattern = Regex(
                "(?is)vous avez (?:re[cç]u un|effectu[ée] un[e]?)\\s+(?<operation>d[ée]p[oô]t|retrait)" +
                    ".*?(?<montant>[0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:XAF|FCFA)" +
                    ".*?(?:ID|Id|r[ée]f(?:[ée]rence)?)\\s*[:\\s]\\s*(?<reference>[A-Za-z0-9.\\-]+)"
            ),
            failurePattern = Regex("(?is)(?:[ée]chec|solde insuffisant|transaction annul[ée]e|refus[ée]e)")
        )
        whenever(config.forOperateur("MTN")).thenReturn(patterns)
        parser = MtnSmsParser(config)
    }

    @Test
    fun `parses a successful DEPOSIT confirmation`() {
        val sms = "Vous avez recu un depot de 5000 XAF. Id: MP240916.1000.A12345"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("5000"))

        assertThat(result).isInstanceOf(SmsParseResult.Success::class.java)
        val success = result as SmsParseResult.Success
        assertThat(success.operatorReference).isEqualTo("MP240916.1000.A12345")
        assertThat(success.confirmedAmount).isEqualTo(BigDecimal("5000"))
    }

    @Test
    fun `parses a successful WITHDRAW confirmation`() {
        val sms = "Vous avez effectue un retrait de 2000 XAF. Id: MP240916.1005.B67890"

        val result = parser.parse(sms, OperationType.WITHDRAW, BigDecimal("2000"))

        assertThat(result).isInstanceOf(SmsParseResult.Success::class.java)
        val success = result as SmsParseResult.Success
        assertThat(success.operatorReference).isEqualTo("MP240916.1005.B67890")
        assertThat(success.confirmedAmount).isEqualTo(BigDecimal("2000"))
    }

    @Test
    fun `reports an operator-side failure`() {
        val sms = "Echec: solde insuffisant pour cette operation"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("5000"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.OPERATOR_ERROR_SMS)
    }

    @Test
    fun `reports unrecognized format for unrelated content`() {
        val sms = "Votre forfait internet arrive a expiration"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("5000"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.UNRECOGNIZED_FORMAT)
    }

    @Test
    fun `reports a mismatch when the SMS confirms a different operation type`() {
        val sms = "Vous avez effectue un retrait de 5000 XAF. Id: MP240916.1000.A12345"

        val result = parser.parse(sms, OperationType.DEPOSIT, BigDecimal("5000"))

        assertThat(result).isInstanceOf(SmsParseResult.Failure::class.java)
        assertThat((result as SmsParseResult.Failure).reason).isEqualTo(FailureReason.OPERATION_TYPE_MISMATCH)
    }
}
