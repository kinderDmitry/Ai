package com.jarvis.nextgen;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommunicationRequestTest {
    @Test public void telegramMessageWithColonIsParsed() {
        CommunicationRequest r = CommunicationRequest.parse("Джарвис, напиши в Telegram Мария: Я скоро буду дома");
        assertEquals(CommunicationRequest.Channel.TELEGRAM, r.channel);
        assertEquals("Мария", r.target);
        assertEquals("Я скоро буду дома", r.message);
    }
    @Test public void smsMessageWithDashIsParsed() {
        CommunicationRequest r = CommunicationRequest.parse("напиши Иван — буду через 20 минут");
        assertEquals(CommunicationRequest.Channel.SMS, r.channel);
        assertEquals("Иван", r.target);
        assertEquals("буду через 20 минут", r.message);
    }
    @Test public void callKeepsNumberAsTarget() {
        CommunicationRequest r = CommunicationRequest.parse("позвони +491234567890");
        assertEquals(CommunicationRequest.Channel.CALL, r.channel);
        assertTrue(r.target.contains("491234567890"));
    }
}
