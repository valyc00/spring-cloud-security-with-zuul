package com.cogent.loginservice.service;

import com.cogent.loginservice.requestDTO.LoginRequestDTO;

import javax.servlet.http.HttpServletRequest;

public interface LoginService {

     String login(LoginRequestDTO requestDTO, HttpServletRequest request);

	String loginKeycloak(LoginRequestDTO requestDTO, HttpServletRequest request);

	//String loginOld(LoginRequestDTO requestDTO, HttpServletRequest request);

}
