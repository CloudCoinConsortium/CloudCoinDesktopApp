/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Александр
 */
public class EmailUI {
    public EmailUI() {
        
    }
    
    public File createMessage(String subject, String body, List<String> attachments) {
        File rfile;
        
        rfile = null;
        
        try {
            Message message = new MimeMessage(Session.getInstance(System.getProperties()));
            //message.setFrom(new InternetAddress(from));
            //message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
        
            // create the message part 
            MimeBodyPart content = new MimeBodyPart();
            // fill message
            content.setText(body);
        
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(content);
        
            // add attachments
            for (String file : attachments) {
                MimeBodyPart attachment = new MimeBodyPart();
                File f = new File(file);
            
                DataSource source = new FileDataSource(f);
                attachment.setDataHandler(new DataHandler(source));
                attachment.setFileName(f.getName());
                multipart.addBodyPart(attachment);
            }
            
            // integration
            message.setContent(multipart);
        
            String tmpDir = System.getProperty("java.io.tmpdir") ;
            if (tmpDir == null)
                return null;
        
            rfile = new File(tmpDir + File.separator + "ccmail.eml");
            
            // store file
            message.writeTo(new FileOutputStream(rfile));
        } catch (MessagingException ex) {
            System.out.println("Exception " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Exception2 " + ex.getMessage());
        }
        
        return rfile;
    }
}
