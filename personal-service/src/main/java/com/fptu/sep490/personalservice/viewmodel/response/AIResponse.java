package com.fptu.sep490.personalservice.viewmodel.response;

public abstract class AIResponse {
    
    /**
     * Get the main content/text from the AI response
     * @return the response content as a string
     */
    public abstract String getContent();
    
    /**
     * Check if the response was successful
     * @return true if successful, false otherwise
     */
    public abstract boolean isSuccess();
}
