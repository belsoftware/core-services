package org.egov.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.egov.domain.model.Category;
import org.egov.domain.model.OtpRequest;
import org.egov.domain.service.LocalizationService;
import org.egov.persistence.contract.SMSRequest;
import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static java.lang.String.format;


@Service
@Slf4j
public class OtpSMSRepository {

    private static final String LOCALIZATION_KEY_REGISTER_SMS = "sms.register.otp.msg";
    private static final String LOCALIZATION_KEY_LOGIN_SMS = "sms.login.otp.msg";
    private static final String LOCALIZATION_KEY_PWD_RESET_SMS = "sms.pwd.reset.otp.msg";

    @Value("${expiry.time.for.otp: 4000}")
    private long maxExecutionTime=2000L;


    private CustomKafkaTemplate<String, SMSRequest> kafkaTemplate;
    private String smsTopic;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    public OtpSMSRepository(CustomKafkaTemplate<String, SMSRequest> kafkaTemplate,
                            @Value("${sms.topic}") String smsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.smsTopic = smsTopic;
    }


    public void send(OtpRequest otpRequest, String otpNumber) {
		Long currentTime = System.currentTimeMillis() + maxExecutionTime;
		final SMSRequest message = getMessage(otpNumber, otpRequest);
        kafkaTemplate.send(smsTopic, new SMSRequest(otpRequest.getMobileNumber(), message.getMessage(), Category.OTP, currentTime, message.getTemplateId()));
    }

    private SMSRequest getMessage(String otpNumber, OtpRequest otpRequest) {
        final SMSRequest messageFormat = getMessageFormat(otpRequest);
        messageFormat.setMessage(format(messageFormat.getMessage(), otpNumber));
        return messageFormat;
    }

    private SMSRequest getMessageFormat(OtpRequest otpRequest) {
        Map<String, SMSRequest> localisedMsgs = localizationService.getLocalisedMessages(otpRequest.getTenantId(), "en_IN", "egov-user");
        if (localisedMsgs.isEmpty()) {
            log.info("Localization Service didn't return any msgs so using default...");
            localisedMsgs.put(LOCALIZATION_KEY_REGISTER_SMS, new SMSRequest("", "Dear Citizen, Your OTP to complete Registration is %s.", null, 0, ""));
            localisedMsgs.put(LOCALIZATION_KEY_LOGIN_SMS, new SMSRequest("", "Dear Citizen, Your Login OTP is %s.", null, 0, ""));
            localisedMsgs.put(LOCALIZATION_KEY_PWD_RESET_SMS, new SMSRequest("","Dear Citizen, Your OTP for recovering password is %s.",null,0,""));
        }
        SMSRequest message = null;

        if (otpRequest.isRegistrationRequestType())
            message = localisedMsgs.get(LOCALIZATION_KEY_REGISTER_SMS);
        else if (otpRequest.isLoginRequestType())
            message = localisedMsgs.get(LOCALIZATION_KEY_LOGIN_SMS);
        else
            message = localisedMsgs.get(LOCALIZATION_KEY_PWD_RESET_SMS);

        return message;
    }
}
