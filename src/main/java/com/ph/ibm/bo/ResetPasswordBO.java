package com.ph.ibm.bo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.ph.ibm.model.Email;
import com.ph.ibm.model.ResetPassword;
import com.ph.ibm.model.ResetPasswordToken;
import com.ph.ibm.opum.exception.OpumException;
import com.ph.ibm.repository.EmployeeRepository;
import com.ph.ibm.repository.impl.EmployeeRepositoryImpl;
import com.ph.ibm.util.MD5HashEncrypter;
import com.ph.ibm.util.OpumConfig;
import com.ph.ibm.util.OpumConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class ResetPasswordBO {

	private Logger logger = Logger.getLogger(ResetPasswordBO.class);

	/**
	 * EmployeeRepository is a Data Access Object which contain methods to add,
	 * register, login, view, validate field/s stored in employee table - opum
	 * database
	 */
	private EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

	public Response resetPassword(ResetPassword resetPassword) throws Exception {
		resetPassword.setNewPassword(MD5HashEncrypter.computeMD5Digest(resetPassword.getNewPassword()));
		employeeRepository.updatePassword(resetPassword);
		
		return Response.status(Status.OK)
				.header("Location", "" + "employee/")
				.entity("email sent successfully")
				.build();
		
	}
	
	public Response emailResetPasswordLink(Email email) throws IOException {
		
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props,
			new javax.mail.Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("onlinepumsender@gmail.com","onlinepum");
				}
			});

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(email.getSenderAddress()));
			message.setSubject(email.getSubject());
			for (String recipientAddress : email.getRecipientAddresses()) {
				message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientAddress));
				String emailResetPasswordLink = generateEmailResetPasswordLink(recipientAddress);
				message.setText(String.format(email.getText(), emailResetPasswordLink) + "\n\n"
						+ OpumConstants.EMAIL_CLOSING + "\n" + OpumConstants.EMAIL_SIGNATURE);
				Transport.send(message);
			}
			
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		return Response.status(Status.OK)
				.header("Location", "" + "employee/")
				.entity("email sent successfully")
				.build();
	}
	
	public boolean validateToken(ResetPasswordToken ResetPasswordToken) throws SQLException, OpumException {
		boolean tokenValidated = false;
		String token = ResetPasswordToken.getToken();
		String email = ResetPasswordToken.getEmail();
		
		String salt = employeeRepository.retrieveSalt(email);		
		if (token != null) {
			Jwts.parser().setSigningKey(salt.getBytes()).parseClaimsJws(ResetPasswordToken.getToken()).getSignature();
		    Jws<Claims> parseClaimsJws = Jwts.parser().setSigningKey(salt.getBytes()).parseClaimsJws(ResetPasswordToken.getToken());        
		    tokenValidated = email.equals(parseClaimsJws.getBody().getSubject()) ? true : false;
		}
	    return tokenValidated;
	}
	
	public String generateToken(String email) throws SQLException, OpumException {
		String salt = employeeRepository.retrieveSalt(email);
		
		Claims claims = Jwts.claims().setSubject(email);
        claims.put("salt", salt);
        Date currentTime = new Date();
        currentTime.setTime(currentTime.getTime() + 1440 * 60000);
        return Jwts.builder()
          .setClaims(claims)
          .setExpiration(currentTime)
          .signWith(SignatureAlgorithm.HS512, salt.getBytes())
          .compact();
	}
	
	private String generateEmailResetPasswordLink(String email) throws UnsupportedEncodingException {
		String resetPasswordHomeLink = OpumConfig.getConfigProperties().getProperty("SERVER_URL")
				+ "/online-pum-ui/resetPassword/resetPasswordLink";
		String token = null;
		try {
			token = generateToken(email);
		} catch (SQLException | OpumException e) {
			e.printStackTrace();
		}
		return new StringBuilder(resetPasswordHomeLink)
				.append("?email=")
				.append(email != null ? URLEncoder.encode(email, "UTF-8") : "")
				.append("&token=")
				.append(token != null ? URLEncoder.encode(token, "UTF-8") : "")
				.toString();
	}
	
}