package com.ussdauto.android.worker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatusNotifierRetryPolicyTest {

    @Test
    fun `retries after the first attempt`() {
        assertThat(StatusNotifierRetryPolicy.shouldRetry(runAttemptCount = 0)).isTrue()
    }

    @Test
    fun `retries after the second attempt`() {
        assertThat(StatusNotifierRetryPolicy.shouldRetry(runAttemptCount = 1)).isTrue()
    }

    @Test
    fun `gives up after the third attempt (3 tentatives au total)`() {
        assertThat(StatusNotifierRetryPolicy.shouldRetry(runAttemptCount = 2)).isFalse()
    }
}
