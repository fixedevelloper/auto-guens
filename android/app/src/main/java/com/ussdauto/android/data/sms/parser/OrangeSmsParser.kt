package com.ussdauto.android.data.sms.parser

import com.ussdauto.android.data.sms.config.SmsParserConfig
import javax.inject.Inject

class OrangeSmsParser @Inject constructor(
    config: SmsParserConfig
) : RegexBasedSmsParser(operateurKey = "ORANGE", config = config)
