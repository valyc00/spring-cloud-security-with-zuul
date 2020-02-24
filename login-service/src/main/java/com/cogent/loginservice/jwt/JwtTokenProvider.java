package com.cogent.loginservice.jwt;

import com.cogent.loginservice.responseDTO.NetworkResponseDTO;
import com.cogent.loginservice.service.serviceImpl.LoginServiceImpl;
import com.cogent.loginservice.utils.NetworkUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.cogent.loginservice.constants.JWTConstants.JWT_KEY;

@Component
public class JwtTokenProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Autowired
    JwtProperties jwtProperties;

    private String secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(jwtProperties.getSecretKey().getBytes());
    }

    public String createTokenWithRoles(String username,String roles, HttpServletRequest request) {
    	LOGGER.debug("vale1");
//        NetworkResponseDTO network = NetworkUtils.getDeviceAddresses.apply(request);
        LOGGER.debug("vale2");
        
//        List<GrantedAuthority> grantedAuths =  AuthorityUtils.commaSeparatedStringToAuthorityList(roles);
//        
//        List<String> roles2 = new ArrayList<String>();
//       
//		roles2.add("ROLE_PIPPO");
//		roles2.add("ROLE_ADMIN");
        
        String ret = Jwts.builder()
                .setSubject(username)
  //              .claim("mac", network.getMacAddress())
  //              .claim("ip", network.getIpAddress())
                .claim("scopes", roles)
                .setIssuer(JWT_KEY)
                .setExpiration(calculateExpirationDate())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
        
        LOGGER.debug("vale3");
        return ret;
    }
    
    
    public String createToken(String username, HttpServletRequest request) {
        NetworkResponseDTO network = NetworkUtils.getDeviceAddresses.apply(request);

        return Jwts.builder()
                .setSubject(username)
                .claim("mac", network.getMacAddress())
                .claim("ip", network.getIpAddress())
                .setIssuer(JWT_KEY)
                .setExpiration(calculateExpirationDate())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    private Date calculateExpirationDate() {
        Date now = new Date();
        return new Date(now.getTime() + jwtProperties.getValidityInMilliseconds());
    }
}
