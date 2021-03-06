package com.cogent.loginservice.controller;

import com.cogent.loginservice.constants.WebResourceKeyConstants;
import com.cogent.loginservice.requestDTO.LoginRequestDTO;
import com.cogent.loginservice.service.LoginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(value = WebResourceKeyConstants.BASE_API)
@Api(value = "This is login controller")
public class MicroController {

	
	// Inject which port we were assigned
	@Value("${server.port}")
	int port;
	
	
    private final LoginService loginService;

    public MicroController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(value = WebResourceKeyConstants.LOGIN)
    @ApiOperation(value = "This is login api", notes = "Request contains username and password")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequestDTO requestDTO, HttpServletRequest request) {

        String token = loginService.login(requestDTO, request);
        System.out.println("token:"+token);
        return ok().body(token);
    }

    @GetMapping("/test")
    public String test() {
        return "test done: port:"+port;
    }
    
    @GetMapping("/sayHello/{name}")
    public String sayHello(@PathVariable("name") String name) {
		
		return "hello!"+name +" server port:" + port;
		
	}
    
    @GetMapping("/sayHello")
    public String sayHello1() {
		
		return "HELLO..";
		
	}
    
}
