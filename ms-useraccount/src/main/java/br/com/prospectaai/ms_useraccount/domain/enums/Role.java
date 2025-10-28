package br.com.prospectaai.ms_useraccount.domain.enums;

public enum Role {
    SimpleUser("simple_user");
    
    private String tag;

    Role(String tag) { this.tag = tag; }

    public String getTag() { return this.tag; }

}
