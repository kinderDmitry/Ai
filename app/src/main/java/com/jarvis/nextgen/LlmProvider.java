package com.jarvis.nextgen;

/** Provider abstraction for online LLMs. Implementations must never expose credentials to UI/logs. */
public interface LlmProvider {
    String id();
    String displayName();
    boolean configured();
    void complete(String system, String user, CompletionCallback callback);
    void clearCredentials();

    interface CompletionCallback {
        void success(String text);
        void failure(String reason);
    }
}
