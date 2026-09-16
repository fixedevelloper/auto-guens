package com.ussdauto.android.data.sms.parser

import com.ussdauto.android.data.sms.config.SmsParserConfig
import javax.inject.Inject

class MtnSmsParser @Inject constructor(
    config: SmsParserConfig
) : RegexBasedSmsParser(operateurKey = "MTN", config = config)
