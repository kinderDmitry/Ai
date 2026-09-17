package com.jarvis.nextgen;

import org.junit.Test;
import static org.junit.Assert.*;

public class SmsCommunicationTest {
    @Test public void parsesNamedRecipientAndMessage() {
        CommunicationRequest r = CommunicationRequest.parse("напиши Ивану: буду дома через 20 минут");
        assertEquals(CommunicationRequest.Channel.SMS, r.channel);
        assertEquals("Ивану", r.target);
        assertEquals("буду дома через 20 минут", r.message);
    }
    @Test public void parsesNumberAndMessage() {
        CommunicationRequest r = CommunicationRequest.parse("sms +491234567890: встречаемся в 8");
        assertEquals(CommunicationRequest.Channel.SMS, r.channel);
        assertEquals("+491234567890", r.target);
        assertEquals("встречаемся в 8", r.message);
    }
}
