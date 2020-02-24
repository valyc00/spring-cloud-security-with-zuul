package com.cogent.loginservice.service.serviceImpl;

import com.cogent.loginservice.constants.ErrorMessageConstants.ForgetPassword;
import com.cogent.loginservice.constants.ErrorMessageConstants.IncorrectPasswordAttempts;
import com.cogent.loginservice.constants.ErrorMessageConstants.InvalidAdminStatus;
import com.cogent.loginservice.constants.ErrorMessageConstants.InvalidAdminUsername;
import com.cogent.loginservice.constants.PatternConstants.EmailConstants;
import com.cogent.loginservice.exceptions.UnauthorisedException;
import com.cogent.loginservice.feignInterface.AdminInterface;
import com.cogent.loginservice.jwt.JwtTokenProvider;
import com.cogent.loginservice.requestDTO.AdminRequestDTO;
import com.cogent.loginservice.requestDTO.LoginRequestDTO;
import com.cogent.loginservice.responseDTO.AdminResponseDTO;
import com.cogent.loginservice.service.LoginService;
import com.cogent.loginservice.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional("transactionManager")
public class LoginServiceImpl implements LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServiceImpl.class);

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    

    @Override
    public String login(LoginRequestDTO requestDTO, HttpServletRequest request) {

        LOGGER.info("LOGIN PROCESS STARTED ::::");
        String jwtToken;
        long startTime = DateUtils.getTimeInMillisecondsFromLocalDate();
        boolean bret= true;
        
        if(("admin".equals(requestDTO.getUserCredential()) && ("admin".equals(requestDTO.getPassword())) )) {
        	
        	
        	 String roles = "ROLE_SUPER,ROLE_ADMIN";
             
        	
			
        	//claims.put("scopes", userContext.getAuthorities().stream().map(s -> s.toString()).collect(Collectors.toList()));
        	
        	
        	 jwtToken = jwtTokenProvider.createTokenWithRoles(requestDTO.getUserCredential(),roles, request);
        	 System.out.println("jwtoken:"+jwtToken);
        }
        else {
        	 throw new UnauthorisedException(IncorrectPasswordAttempts.MESSAGE,
                     IncorrectPasswordAttempts.DEVELOPER_MESSAGE);
        }


        return jwtToken;
    }
    
    
//    @Override
//    public String loginOld(LoginRequestDTO requestDTO, HttpServletRequest request) {
//
//        LOGGER.info("LOGIN PROCESS STARTED ::::");
//
//        long startTime = DateUtils.getTimeInMillisecondsFromLocalDate();
//
//        AdminResponseDTO admin = fetchAdminDetails.apply(requestDTO);
//
//        validateAdminUsername.accept(admin);
//
//        validateAdminStatus.accept(admin);
//
//        validatePassword.accept(requestDTO, admin);
//
//        String jwtToken = jwtTokenProvider.createToken(requestDTO.getUserCredential(), request);
//
//        LOGGER.info("LOGIN PROCESS COMPLETED IN ::: " + (DateUtils.getTimeInMillisecondsFromLocalDate() - startTime)
//                + " ms");
//
//        return jwtToken;
//    }

//    private Function<LoginRequestDTO, AdminResponseDTO> fetchAdminDetails = (loginRequestDTO) -> {
//
//        Pattern pattern = Pattern.compile(EmailConstants.EMAIL_PATTERN);
//        Matcher m = pattern.matcher(loginRequestDTO.getUserCredential());
//
//        return m.find() ? adminInterface.searchAdmin
//                (AdminRequestDTO.builder().username(null).emailAddress(loginRequestDTO.getUserCredential()).build())
//                : adminInterface.searchAdmin
//                (AdminRequestDTO.builder().username(loginRequestDTO.getUserCredential()).emailAddress(null).build());
//    };

    private Consumer<AdminResponseDTO> validateAdminUsername = (admin) -> {
        if (Objects.isNull(admin))
            throw new UnauthorisedException(InvalidAdminUsername.MESSAGE, InvalidAdminUsername.DEVELOPER_MESSAGE);
        LOGGER.info(":::: ADMIN USERNAME VALIDATED ::::");
    };

    private Consumer<AdminResponseDTO> validateAdminStatus = (admin) -> {

        switch (admin.getStatus()) {
            case 'B':
                throw new UnauthorisedException(InvalidAdminStatus.MESSAGE_FOR_BLOCKED,
                        InvalidAdminStatus.DEVELOPER_MESSAGE_FOR_BLOCKED);

            case 'N':
                throw new UnauthorisedException(InvalidAdminStatus.MESSAGE_FOR_INACTIVE,
                        InvalidAdminStatus.DEVELOPER_MESSAGE_FOR_INACTIVE);
        }
        LOGGER.info(":::: ADMIN STATUS VALIDATED ::::");
    };

//    private BiConsumer<LoginRequestDTO, AdminResponseDTO> validatePassword = (requestDTO, admin) -> {
//
//        LOGGER.info(":::: ADMIN PASSWORD VALIDATION ::::");
//
//        if (BCrypt.checkpw(requestDTO.getPassword(), admin.getPassword())) {
//            admin.setLoginAttempt(0);
//            adminInterface.updateAdmin(admin);
//        } else {
//            admin.setLoginAttempt(admin.getLoginAttempt() + 1);
//
//            if (admin.getLoginAttempt() >= 3) {
//                admin.setStatus('B');
//                adminInterface.updateAdmin(admin);
//
//                LOGGER.debug("ADMIN IS BLOCKED DUE TO MULTIPLE WRONG ATTEMPTS...");
//                throw new UnauthorisedException(IncorrectPasswordAttempts.MESSAGE,
//                        IncorrectPasswordAttempts.DEVELOPER_MESSAGE);
//            }
//
//            LOGGER.debug("INCORRECT PASSWORD...");
//            throw new UnauthorisedException(ForgetPassword.MESSAGE, ForgetPassword.DEVELOPER_MESSAGE);
//        }
//
//        LOGGER.info(":::: ADMIN PASSWORD VALIDATED ::::");
//    };

}

