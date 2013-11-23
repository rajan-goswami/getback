package com.codeperf.getback.core;

import java.security.Security;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class EmailSender extends javax.mail.Authenticator {
	private String mailhost = "smtp.gmail.com";
	private String user;
	private String password;
	private Session session;
	private Multipart multipart;
	private ISendEmailCallback callback;

	static {
		Security.addProvider(new JSSEProvider());
	}

	public EmailSender(String user, String password, ISendEmailCallback cb) {
		this.user = user;
		this.password = password;
		this.callback = cb;

		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", mailhost);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.quitwait", "false");

		session = Session.getDefaultInstance(props, this);
		multipart = new MimeMultipart();

		MailcapCommandMap mc = (MailcapCommandMap) CommandMap
				.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(user, password);
	}

	public synchronized boolean sendMail(String subject, String body,
			String sender, String receipients) {
		boolean bReturn = false;
		try {
			MimeMessage message = new MimeMessage(session);
			message.setSender(new InternetAddress(sender));
			message.setSubject(subject);

			// setup message body
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);
			multipart.addBodyPart(messageBodyPart);

			message.setContent(multipart);

			if (receipients.indexOf(',') > 0)
				message.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse(receipients));
			else
				message.setRecipient(Message.RecipientType.TO,
						new InternetAddress(receipients));
			Transport.send(message);
			bReturn = true;
			callback.onEmailSent();
		} catch (Exception e) {
			Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception: ", e);
			callback.onEmailError(-1);
		}
		return bReturn;
	}

	public void addAttachment(String filename) {
		BodyPart messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(filename);
		try {
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(filename);

			multipart.addBodyPart(messageBodyPart);
		} catch (MessagingException e) {
			Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception: ", e);
			callback.onEmailError(-1);
		}
	}
}
