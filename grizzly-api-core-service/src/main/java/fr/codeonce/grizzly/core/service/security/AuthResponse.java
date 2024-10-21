package fr.codeonce.grizzly.core.service.security;

public class AuthResponse {

    private String access_token;
    private String tokenType = "Bearer ";
    private String email;

    public AuthResponse(String access_token) {
        super();
        this.access_token = access_token;
    }

    public AuthResponse(String access_token, String email) {
        super();
        this.access_token = access_token;
        this.email = email;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
