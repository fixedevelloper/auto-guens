package com.ussdauto.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.repository.TransactionRepository;
import com.ussdauto.api.service.TransactionTimeoutJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout côté API : crée une transaction, simule les callbacks successifs qu'un
 * device Android enverrait à chaque changement de statut, et vérifie l'historique,
 * l'idempotence, la libération du device et le job de timeout. FCM est mocké (voir
 * FirebaseTestConfig) — seule la couche HTTP/sécurité/DB/service est exercée réellement,
 * l'exécution USSD/SMS côté Android n'est pas simulable dans cet environnement (pas
 * d'émulateur ni de credentials Firebase réels disponibles ici).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionLifecycleIntegrationTest {

    private static final String MERCHANT_KEY = "changeme-dev-only";
    private static final String DEVICE_KEY = "device-key-1";
    private static final String OTHER_DEVICE_KEY = "device-key-2";

    @TestConfiguration
    static class FirebaseTestConfig {
        @Bean
        FirebaseMessaging firebaseMessaging() throws Exception {
            FirebaseMessaging mock = Mockito.mock(FirebaseMessaging.class);
            Mockito.when(mock.send(Mockito.any())).thenReturn("mock-message-id");
            return mock;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionTimeoutJob transactionTimeoutJob;

    private UUID deviceId;

    @BeforeEach
    void setUp() throws Exception {
        Device device = deviceRepository.save(Device.builder()
                .apiKey(DEVICE_KEY)
                .statut(DeviceStatus.ONLINE)
                .fcmToken("tok-1")
                .lastSeenAt(Instant.now())
                .build());
        deviceId = device.getId();

        deviceRepository.save(Device.builder()
                .apiKey(OTHER_DEVICE_KEY)
                .statut(DeviceStatus.ONLINE)
                .fcmToken("tok-2")
                .lastSeenAt(Instant.now())
                .build());

        createUssdTemplate("MTN", "DEPOSIT", "*126*1*{amount}*{phone}#");
        createUssdTemplate("MTN", "WITHDRAW", "*126*2*{amount}*{phone}#");
        createUssdTemplate("ORANGE", "DEPOSIT", "#150*50*{amount}*{phone}#");

        replaceSimSlots(deviceId, "[{\"slotIndex\":0,\"operator\":\"MTN\",\"phoneNumberOnSim\":\"677000000\"}]");
    }

    @Test
    void depositLifecycle_progressesThroughHistoryIdempotentlyAndReleasesDevice() throws Exception {
        String transactionId = createTransaction("DEPOSIT", "MTN", "677000000", 5000);

        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatut()).isEqualTo(DeviceStatus.BUSY);

        postStatus(transactionId, DEVICE_KEY, "RECEIVED_BY_DEVICE", null, null, null);
        postStatus(transactionId, DEVICE_KEY, "EXECUTING", null, null, null);
        postStatus(transactionId, DEVICE_KEY, "EXECUTING", null, null, null); // doublon -> ignoré
        postStatus(transactionId, DEVICE_KEY, "SUCCESS", "Vous avez recu un depot de 5000 XAF", "OP-REF-1", null);

        mockMvc.perform(get("/api/transactions/{id}", transactionId).header("X-Api-Key", MERCHANT_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"))
                .andExpect(jsonPath("$.operatorReference").value("OP-REF-1"))
                .andExpect(jsonPath("$.statusHistory.length()").value(5))
                .andExpect(jsonPath("$.statusHistory[0].statut").value("PENDING"))
                .andExpect(jsonPath("$.statusHistory[1].statut").value("SENT_TO_DEVICE"))
                .andExpect(jsonPath("$.statusHistory[2].statut").value("RECEIVED_BY_DEVICE"))
                .andExpect(jsonPath("$.statusHistory[3].statut").value("EXECUTING"))
                .andExpect(jsonPath("$.statusHistory[4].statut").value("SUCCESS"));

        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatut()).isEqualTo(DeviceStatus.ONLINE);

        mockMvc.perform(get("/api/transactions")
                        .header("X-Api-Key", MERCHANT_KEY)
                        .param("operationType", "DEPOSIT")
                        .param("statut", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transactionId));
    }

    @Test
    void depositAndWithdrawResolveDifferentUssdTemplatesForTheSameOperator() throws Exception {
        String depositId = createTransaction("DEPOSIT", "MTN", "677000000", 5000);
        postStatus(depositId, DEVICE_KEY, "SUCCESS", null, "REF-1", null);
        // libère le device pour la transaction suivante
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatut()).isEqualTo(DeviceStatus.ONLINE);

        String withdrawId = createTransaction("WITHDRAW", "MTN", "677000000", 2000);

        mockMvc.perform(get("/api/transactions/{id}", depositId).header("X-Api-Key", MERCHANT_KEY))
                .andExpect(jsonPath("$.ussdCodeUsed").value("*126*1*5000*677000000#"));
        mockMvc.perform(get("/api/transactions/{id}", withdrawId).header("X-Api-Key", MERCHANT_KEY))
                .andExpect(jsonPath("$.ussdCodeUsed").value("*126*2*2000*677000000#"));
    }

    @Test
    void rejectsAStatusReservedForTheApiItself() throws Exception {
        String transactionId = createTransaction("DEPOSIT", "MTN", "677000000", 5000);

        mockMvc.perform(post("/api/transactions/{id}/status", transactionId)
                        .header("X-Device-Api-Key", DEVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SENT_TO_DEVICE\",\"timestamp\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCallbackFromADeviceNotAssignedToTheTransaction() throws Exception {
        String transactionId = createTransaction("DEPOSIT", "MTN", "677000000", 5000);

        mockMvc.perform(post("/api/transactions/{id}/status", transactionId)
                        .header("X-Device-Api-Key", OTHER_DEVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EXECUTING\",\"timestamp\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsTransactionCreationWhenOperationTypeIsMissing() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("X-Api-Key", MERCHANT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"677000000\",\"operator\":\"MTN\",\"amount\":5000,\"senderName\":\"Jean Dupont\",\"countryCode\":\"CM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.operationType").exists());
    }

    @Test
    void returns503WhenNoDeviceHasAnActiveSlotForTheOperator() throws Exception {
        // ORANGE a un template mais aucun device n'a de slot ORANGE actif configuré
        mockMvc.perform(post("/api/transactions")
                        .header("X-Api-Key", MERCHANT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationType\":\"DEPOSIT\",\"phone\":\"690000000\",\"operator\":\"ORANGE\",\"amount\":1000,\"senderName\":\"Jean Dupont\",\"countryCode\":\"CM\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void timeoutJobMarksAStuckExecutingTransactionAsTimeoutAndReleasesTheDevice() throws Exception {
        String transactionId = createTransaction("DEPOSIT", "MTN", "677000000", 5000);
        postStatus(transactionId, DEVICE_KEY, "EXECUTING", null, null, null);

        Transaction transaction = transactionRepository.findById(UUID.fromString(transactionId)).orElseThrow();
        transaction.setExpiresAt(Instant.now().minusSeconds(5));
        transactionRepository.save(transaction);

        transactionTimeoutJob.timeoutStaleTransactions();

        mockMvc.perform(get("/api/transactions/{id}", transactionId).header("X-Api-Key", MERCHANT_KEY))
                .andExpect(jsonPath("$.statut").value("TIMEOUT"));

        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatut()).isEqualTo(DeviceStatus.ONLINE);
    }

    private String createTransaction(String operationType, String operator, String phone, int amount) throws Exception {
        String body = "{\"operationType\":\"%s\",\"phone\":\"%s\",\"operator\":\"%s\",\"amount\":%d,\"senderName\":\"Jean Dupont\",\"countryCode\":\"CM\"}"
                .formatted(operationType, phone, operator, amount);

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("X-Api-Key", MERCHANT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SENT_TO_DEVICE"))
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return created.get("id").asText();
    }

    private void postStatus(String transactionId, String deviceApiKey, String status,
                             String rawSmsContent, String operatorReference, String failureReason) throws Exception {
        StringBuilder json = new StringBuilder("{\"status\":\"").append(status)
                .append("\",\"timestamp\":\"").append(Instant.now()).append("\"");
        if (rawSmsContent != null) json.append(",\"rawSmsContent\":\"").append(rawSmsContent).append("\"");
        if (operatorReference != null) json.append(",\"operatorReference\":\"").append(operatorReference).append("\"");
        if (failureReason != null) json.append(",\"failureReason\":\"").append(failureReason).append("\"");
        json.append("}");

        mockMvc.perform(post("/api/transactions/{id}/status", transactionId)
                        .header("X-Device-Api-Key", deviceApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isOk());
    }

    private void createUssdTemplate(String operator, String operationType, String template) throws Exception {
        String body = "{\"operator\":\"%s\",\"operationType\":\"%s\",\"template\":\"%s\"}"
                .formatted(operator, operationType, template);

        mockMvc.perform(post("/api/ussd-templates")
                        .header("X-Api-Key", MERCHANT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void replaceSimSlots(UUID deviceId, String itemsJson) throws Exception {
        mockMvc.perform(post("/api/devices/{id}/sim-slots", deviceId)
                        .header("X-Api-Key", MERCHANT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson))
                .andExpect(status().isOk());
    }
}
