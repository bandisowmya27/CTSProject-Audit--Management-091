package com.mfpe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mfpe.model.AuthenticationRequest;
import com.mfpe.model.AuthenticationResponse;
import com.mfpe.model.ProjectManagerDetails;
import com.mfpe.service.JwtService;
import com.mfpe.service.ProjectManagerDetailsService;

@RestController
@RequestMapping("/auth")	//Context Root
public class AuthController {
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private ProjectManagerDetailsService projectManagerDetailsService;
	
	@Autowired
	private JwtService jwtService;
	
	@GetMapping("/health-check")
	public ResponseEntity<String> healthCheck(){	// for Health check [PERMITTED FOR ALL]
		return new ResponseEntity<String>("Audit-Authorization MS Running Fine!!", HttpStatus.OK);
	}
	
	// authentication - for the very first login
	@PostMapping("/authenticate")
	public ResponseEntity<String> generateJwt(@RequestBody AuthenticationRequest request){
		ResponseEntity<String> response = null;
		
		// authenticating the User-Credentials
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
			// else when it authenticates successfully
			final ProjectManagerDetails projectManagerDetails = projectManagerDetailsService
																	.loadUserByUsername(request.getUsername());
			
			final String jwt = jwtService.generateToken(projectManagerDetails);	// returning the token as response
			
			//test
			System.out.println("Authenticated User :: " + projectManagerDetails);
			
			response = new ResponseEntity<String>(jwt, HttpStatus.OK);
		}catch (Exception e) {
			response = new ResponseEntity<String>("Not Authorized Project Manager", HttpStatus.FORBIDDEN);
		}

		return response;
	}
	
	// validate - for every request it validates the user-credentials from the provided Jwt token in Authorization req. header
	@PostMapping("/validate")
	public ResponseEntity<AuthenticationResponse> validateJwt(@RequestHeader("Authorization") String jwt){
		
		AuthenticationResponse authenticationResponse = new AuthenticationResponse("Invalid", "Invalid", false);
		ResponseEntity<AuthenticationResponse> response = null;

		//first remove Bearer from Header
		jwt = jwt.substring(7);
		
		//check token
		System.out.println("--------\nJWT :: "+jwt);
		
		
		// check the jwt is proper or not
		// getting user-name from session
		final ProjectManagerDetails projectManagerDetails = projectManagerDetailsService
															.loadUserByUsername(jwtService.extractUsername(jwt));
		
		// now validating the jwt
		try {
			if(jwtService.validateToken(jwt, projectManagerDetails)) {
				authenticationResponse.setName(projectManagerDetails.getName());
				authenticationResponse.setProjectName(projectManagerDetails.getProjectName());
				authenticationResponse.setValid(true);
				response = new ResponseEntity<AuthenticationResponse>(authenticationResponse, HttpStatus.OK);
			}
			else {
				response = new ResponseEntity<AuthenticationResponse>(authenticationResponse, HttpStatus.FORBIDDEN);
			}
		}catch (Exception e) {
			e.printStackTrace();
			response = new ResponseEntity<AuthenticationResponse>(authenticationResponse, HttpStatus.BAD_REQUEST);
		}
		System.out.println("-------- Validated");
		return response;
	}
	
}
