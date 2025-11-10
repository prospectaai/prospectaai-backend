package br.com.prospectaai.shared.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailEnvelope {
    private String recipient;
    private String title;
    private String emailContent;
    private String emailType;
    private EmailEnvelopeMetadata metadata;
}
