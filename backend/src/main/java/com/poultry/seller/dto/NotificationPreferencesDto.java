package com.poultry.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDto {
    private boolean emailNewOrders;
    private boolean emailOrderUpdates;
    private boolean emailPaymentReceived;
    private boolean emailSettlementCompleted;
    private boolean emailDisputeRaised;
    private boolean emailPromotions;

    private boolean smsNewOrders;
    private boolean smsOrderUpdates;
    private boolean smsPaymentReceived;
    private boolean smsSettlementCompleted;
    private boolean smsDisputeRaised;

    private boolean pushNewOrders;
    private boolean pushOrderUpdates;
    private boolean pushPaymentReceived;
    private boolean pushSettlementCompleted;
    private boolean pushDisputeRaised;

    public static NotificationPreferencesDto defaultPreferences() {
        return NotificationPreferencesDto.builder()
                .emailNewOrders(true)
                .emailOrderUpdates(true)
                .emailPaymentReceived(true)
                .emailSettlementCompleted(true)
                .emailDisputeRaised(true)
                .emailPromotions(false)
                .smsNewOrders(true)
                .smsOrderUpdates(false)
                .smsPaymentReceived(true)
                .smsSettlementCompleted(true)
                .smsDisputeRaised(true)
                .pushNewOrders(true)
                .pushOrderUpdates(true)
                .pushPaymentReceived(true)
                .pushSettlementCompleted(true)
                .pushDisputeRaised(true)
                .build();
    }
}
