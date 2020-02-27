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
import com.cogent.loginservice.requestDTO.UserDTO;
import com.cogent.loginservice.responseDTO.AdminResponseDTO;
import com.cogent.loginservice.service.LoginService;
import com.cogent.loginservice.utils.DateUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JsonObjectSerializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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

	
	@Value("${keycloak.public.key:''}")
	private String externalPublicKey;
	
	@Value("${keycloak.url:''}")
	private String keycloakUrl;
	
	@Value("${keycloak.client.secret:''}")
	private String keycloakClientSecret;
	
	@Value("${keycloak.client.id:''}")
	private String keycloakClientId;
	
	

	@Override
	public String login(LoginRequestDTO requestDTO, HttpServletRequest request) {

		LOGGER.info("LOGIN PROCESS STARTED ::::");
		String jwtToken;
		String roles;
		
		UserDTO userDTO = new UserDTO();
		
		long startTime = DateUtils.getTimeInMillisecondsFromLocalDate();
		boolean bret = true;

		if (("admin".equals(requestDTO.getUserCredential()) && ("admin".equals(requestDTO.getPassword())))) {

			roles = "ROLE_SUPER,ROLE_ADMIN";
			userDTO.setUsername(requestDTO.getUserCredential());
			userDTO.setRoles(roles);
			userDTO.setEmailAddress("admin@admin.it");
			

			// jwtToken =
			// jwtTokenProvider.createTokenWithRoles(requestDTO.getUserCredential(),roles,
			// request);
			// System.out.println("jwtoken:"+jwtToken);
		} else if (("user".equals(requestDTO.getUserCredential()) && ("user".equals(requestDTO.getPassword())))) {

			roles = "ROLE_SUPER,ROLE_USER";
			userDTO.setUsername(requestDTO.getUserCredential());
			userDTO.setRoles(roles);
			userDTO.setEmailAddress("user@user.it");

		} else {
			throw new UnauthorisedException(IncorrectPasswordAttempts.MESSAGE,
					IncorrectPasswordAttempts.DEVELOPER_MESSAGE);
		}
		
		jwtToken = jwtTokenProvider.createTokenWithRoles(userDTO, request);
		LOGGER.debug("jwtoken:" + jwtToken);

		return jwtToken;
	}

	@Override
	public String loginKeycloak(LoginRequestDTO requestDTO, HttpServletRequest request) {

		LOGGER.info("LOGIN KEYCLOAK PROCESS STARTED ::::");

		String jwtToken = null;
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("client_secret", keycloakClientSecret);
		map.add("client_id", keycloakClientId);
		map.add("username", requestDTO.getUserCredential());
		map.add("password", requestDTO.getPassword());
		map.add("grant_type", "password");

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<String> response = restTemplate.exchange(
				keycloakUrl, HttpMethod.POST, entity,
				String.class);

		String body = response.getBody();
		

		LOGGER.debug(body);

		try {
			ObjectNode node = new ObjectMapper().readValue(body, ObjectNode.class);

			if (node.has("access_token")) {
				LOGGER.debug("access_token: " + node.get("access_token"));
				String access_token = node.get("access_token").toString();
				access_token = access_token.replace("\"", "");

				if (validateTokenExt(access_token)) {
					UserDTO userDTO =  getAuthenticationExt(access_token);
					jwtToken = jwtTokenProvider.createTokenWithRoles(userDTO, request);
				}

				System.out.println("access_token:" + access_token);
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// jwtToken =
		// jwtTokenProvider.createTokenWithRoles(requestDTO.getUserCredential(),roles,
		// request);
		LOGGER.debug("jwtoken:" + jwtToken);

		return jwtToken;
	}

	public UserDTO getAuthenticationExt(String token) {
		
		UserDTO userDTO = new UserDTO();

		String roles = "";
		// String realmPublicKey =
		// "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgCDRTsMJpX13Jn+SmBuVv43z/zSqopZCbqmSztiaW398E/5+AmiHfd4o2tKhU279Z6QFkKLibIFt3q/mHAw+U22zcIxjoU3PhRuIWpqQf4XbLR6wPY/Q8h+ZjULcR813dybvGRvpfGIzU93mdNrSQXkhJy23l+N+7il0VSe6/orLtq0nPM7Ysg+YiKDS3L0skcv6dU0ZGR9Jzodm9ciP/SX2SqdsbA/vf13s6GlaPzzr1oldrJ8i8qn9rSqaeb9Kzti+sPIACCKMgOp/nca2QjNw5LfCrLtT6a8h2s0kCgfNVr6WHevvIDrYG1Q3iKkGFA8TIOVF+4mbV861RyRa3wIDAQAB";
		// String accessTokenString =
		// "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJPSFhBOTEzR3IzLTM2VnBQWHYzNGRUUjBGXzFrcjhMajZwZXdKdzUtNm9rIn0.eyJqdGkiOiJiNDk3NDdhYS02ZTQyLTRlZDctYmRmYy0yYzg0NTJiODJlN2QiLCJleHAiOjE1ODE4NzA1MzIsIm5iZiI6MCwiaWF0IjoxNTgxODcwMjMyLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwODAvYXV0aC9yZWFsbXMvamhpcHN0ZXIiLCJzdWIiOiI0Yzk3Mzg5Ni01NzYxLTQxZmMtODIxNy0wN2M1ZDEzYTAwNGIiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhY2NvdW50IiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiMDJmZTFiYTEtNTgwMC00MjY0LTgxOTAtNDk2ZGEzMjkxNjM2IiwiYWNyIjoiMSIsInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkFkbWluIEFkbWluaXN0cmF0b3IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiIsImdpdmVuX25hbWUiOiJBZG1pbiIsImZhbWlseV9uYW1lIjoiQWRtaW5pc3RyYXRvciIsImVtYWlsIjoiYWRtaW5AbG9jYWxob3N0In0.TV9m1syJKJ7PRAxvke0_UH7gBAORoEpvek9v-G4ktVKPUj5YTkx56Q8XFkHyqIX6pdMWx7u1odj8QPOnEtYMU2f-cwQhgvej_E1Z1Ei6PN3HAj3vsKHIQcBkxHaG1NJNWiiH02YErgP5dVq54OhXCgyD55n5yhBTESOIbkyjAe1jpuBuqS3BXzqNT_fj83obI7IUQ18jZKKOAX_53TT0DsgnLDjuiEWy3MHpoUhdasW1BWdeSajsalwDUiJwvDzTQ6GCca1Iex_AmnWEfFmhN4FlAJQc1XxtpTInS3gHsfGaDOIS5PfQDNaw7ElwFhESjPVO_yI26ZfHbZ5P0fGGOA";
		String realmPublicKey = externalPublicKey;
		String accessTokenString = token;

		try {
			PublicKey publicKey = decodePublicKey(pemToDer(realmPublicKey));

			Claims claims = Jwts.parser() //
					.setSigningKey(publicKey) //
					.parseClaimsJws(accessTokenString).getBody() //
			;

			Map ra = (Map) claims.get("resource_access");
			Map a = (Map) ra.get("account");
			String  preferred_username =  (String) claims.get("preferred_username"); 
			String  name =  (String) claims.get("name"); 
			String  email =  (String) claims.get("email"); 
			List<String> roleList = (List<String>) a.get("roles");

			for (String r : roleList) {
				roles =roles + r + " , ";

			}
			
			LOGGER.debug(roles);
			
			userDTO.setUsername(preferred_username);
			userDTO.setEmailAddress(email);
			userDTO.setRoles(roles);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return userDTO;

	}

	public boolean validateTokenExt(String authToken) {
		try {

			// String realmPublicKey =
			// "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgCDRTsMJpX13Jn+SmBuVv43z/zSqopZCbqmSztiaW398E/5+AmiHfd4o2tKhU279Z6QFkKLibIFt3q/mHAw+U22zcIxjoU3PhRuIWpqQf4XbLR6wPY/Q8h+ZjULcR813dybvGRvpfGIzU93mdNrSQXkhJy23l+N+7il0VSe6/orLtq0nPM7Ysg+YiKDS3L0skcv6dU0ZGR9Jzodm9ciP/SX2SqdsbA/vf13s6GlaPzzr1oldrJ8i8qn9rSqaeb9Kzti+sPIACCKMgOp/nca2QjNw5LfCrLtT6a8h2s0kCgfNVr6WHevvIDrYG1Q3iKkGFA8TIOVF+4mbV861RyRa3wIDAQAB";
			// String accessTokenString =
			// "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJPSFhBOTEzR3IzLTM2VnBQWHYzNGRUUjBGXzFrcjhMajZwZXdKdzUtNm9rIn0.eyJqdGkiOiJiNDk3NDdhYS02ZTQyLTRlZDctYmRmYy0yYzg0NTJiODJlN2QiLCJleHAiOjE1ODE4NzA1MzIsIm5iZiI6MCwiaWF0IjoxNTgxODcwMjMyLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwODAvYXV0aC9yZWFsbXMvamhpcHN0ZXIiLCJzdWIiOiI0Yzk3Mzg5Ni01NzYxLTQxZmMtODIxNy0wN2M1ZDEzYTAwNGIiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhY2NvdW50IiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiMDJmZTFiYTEtNTgwMC00MjY0LTgxOTAtNDk2ZGEzMjkxNjM2IiwiYWNyIjoiMSIsInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkFkbWluIEFkbWluaXN0cmF0b3IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiIsImdpdmVuX25hbWUiOiJBZG1pbiIsImZhbWlseV9uYW1lIjoiQWRtaW5pc3RyYXRvciIsImVtYWlsIjoiYWRtaW5AbG9jYWxob3N0In0.TV9m1syJKJ7PRAxvke0_UH7gBAORoEpvek9v-G4ktVKPUj5YTkx56Q8XFkHyqIX6pdMWx7u1odj8QPOnEtYMU2f-cwQhgvej_E1Z1Ei6PN3HAj3vsKHIQcBkxHaG1NJNWiiH02YErgP5dVq54OhXCgyD55n5yhBTESOIbkyjAe1jpuBuqS3BXzqNT_fj83obI7IUQ18jZKKOAX_53TT0DsgnLDjuiEWy3MHpoUhdasW1BWdeSajsalwDUiJwvDzTQ6GCca1Iex_AmnWEfFmhN4FlAJQc1XxtpTInS3gHsfGaDOIS5PfQDNaw7ElwFhESjPVO_yI26ZfHbZ5P0fGGOA";
			String realmPublicKey = externalPublicKey;
			String accessTokenString = authToken;

			try {
				PublicKey publicKey = decodePublicKey(pemToDer(realmPublicKey));

				Jws<Claims> claimsJws = Jwts.parser() //
						.setSigningKey(publicKey) //
						.parseClaimsJws(accessTokenString) //
				;
				
			} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			// Jwts.parser().setSigningKey(key).parseClaimsJws(authToken);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			LOGGER.info("Invalid JWT token.");
			LOGGER.debug("Invalid JWT token trace.", e);
		}
		return false;
	}

	public static PublicKey decodePublicKey(byte[] der)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {

		X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

		KeyFactory kf = KeyFactory.getInstance("RSA"
		// , "BC" //use provider BouncyCastle if available.
		);

		return kf.generatePublic(spec);
	}

	public static byte[] pemToDer(String pem) throws IOException {
		return Base64.getDecoder().decode(stripBeginEnd(pem));
	}

	public static String stripBeginEnd(String pem) {

		String stripped = pem.replaceAll("-----BEGIN (.*)-----", "");
		stripped = stripped.replaceAll("-----END (.*)----", "");
		stripped = stripped.replaceAll("\r\n", "");
		stripped = stripped.replaceAll("\n", "");

		return stripped.trim();
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
