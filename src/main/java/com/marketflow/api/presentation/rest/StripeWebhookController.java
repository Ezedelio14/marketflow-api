package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.payment.PaymentService;
import com.marketflow.api.application.payment.PaymentWebhookEvent;
import com.marketflow.api.application.payment.PaymentWebhookParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirements
@RequestMapping(path = "/api/v1/webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Webhooks", description = "Payment provider callbacks (authenticated by signature, not JWT)")
public class StripeWebhookController {

    private final PaymentWebhookParser webhookParser;
    private final PaymentService paymentService;

    @PostMapping(path = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Stripe webhook",
            description = """
                    Receives Stripe events. The raw body is verified against the `Stripe-Signature` header with
                    `Webhook.constructEvent` (HMAC-SHA256 + timestamp tolerance); unsigned or tampered payloads are
                    rejected with 400. Processing is idempotent through the `processed_events` table, so Stripe
                    retries are safe.

                    Handled: `payment_intent.succeeded` (order → PAID, stock committed),
                    `payment_intent.payment_failed`, `payment_intent.canceled` (order → CANCELLED, stock released),
                    `charge.refunded` (order → REFUNDED). Other types are acknowledged and ignored.""",
            operationId = "stripeWebhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event acknowledged (PROCESSED, DUPLICATE or IGNORED)",
                    content = @Content(schema = @Schema(example = "{\"eventId\":\"evt_1PqXyZ\",\"result\":\"PROCESSED\"}"))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid signature / malformed payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Processing failed; Stripe will retry",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Map<String, String> handle(
            @Parameter(in = ParameterIn.HEADER, name = "Stripe-Signature", required = true,
                    description = "Signature header set by Stripe", example = "t=1728800000,v1=5257a869e7ecebeda32affa62cdca3fa51cad7e77a0e56ff536d0ce8e108d8bd")
            @RequestHeader(name = "Stripe-Signature", required = false) String signature,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Raw Stripe event JSON (must not be re-serialised)", required = true)
            @RequestBody String payload) {
        PaymentWebhookEvent event = webhookParser.parse(payload, signature);
        PaymentService.Outcome outcome = paymentService.handleWebhook(event);
        return Map.of("eventId", event.eventId(), "result", outcome.name());
    }
}
