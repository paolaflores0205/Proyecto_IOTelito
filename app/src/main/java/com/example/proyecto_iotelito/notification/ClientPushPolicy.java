package com.example.proyecto_iotelito.notification;

/** Contrato independiente de Android para rechazar avisos de otra cuenta o sesión. */
public final class ClientPushPolicy {
    private ClientPushPolicy() { }
    public static boolean accepts(String recipient, String binding, String authenticated,
                                  String registered, String registeredBinding) {
        return recipient != null && !recipient.isEmpty() && binding != null && !binding.isEmpty()
                && recipient.equals(authenticated) && recipient.equals(registered)
                && binding.equals(registeredBinding);
    }
}
