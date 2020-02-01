package global.cloudcoin.ccbank.Emailer;

import global.cloudcoin.ccbank.Emailer.EmailerResult;
import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Emailer extends Servant {
    public EmailerResult ger;
    String ltag = "Emailer";
    
    String host;
    int port;
    String username, password, from, mail_from;
    int maxConcurrent;
    
    public Emailer(String rootDir, GLogger logger) {
        super("Emailer", rootDir, logger);
        ger = new EmailerResult();
    }

    public void launch(String[] emails, String[] subjects, String[] bodies, String[][] attachments, CallbackInterface icb) {
        this.cb = icb;

        final String[] femails = emails;
        final String[] fsubjects = subjects;
        final String[] fbodies = bodies;
        final String[][] fattachments = attachments;

        this.maxConcurrent = Config.DEFAULT_MAX_EMAILS_PER_RUN;
        this.host = Config.DEFAULT_EMAIL_HOST;
        this.port = Config.DEFAULT_EMAIL_PORT;
         
        ger = new EmailerResult();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Emailer");

                doEmail(femails, fsubjects, fbodies, fattachments, cb);
                if (cb != null) 
                    cb.callback(ger);
            }
        });
    }    

    public void copyFromMainResult(EmailerResult er) {
        er.sentEmails = ger.sentEmails;
        er.errText = ger.errText;
        er.totalEmails = ger.totalEmails;
    }
    
    public void setError(String errText) {
        EmailerResult er = new EmailerResult();
        ger.errText = errText;
        ger.status = EmailerResult.STATUS_ERROR;
    }
    
    public boolean readConfig() {
        String filename = AppCore.getMailConfigFilename();
        File f = new File(filename);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {}
            logger.error(ltag, "Failed to read config file. It doesn't exist");
            setError("Failed to read mail config: " + AppCore.getMailConfigFilename());
            return false;
        }

        FileReader fr;
        try {
            fr = new FileReader(f);
        } catch (Exception e) {
            setError("Failed to read mail config: " + AppCore.getMailConfigFilename());
            return false;
        }
        Map<String, Properties> data;
        try {
            data = AppCore.parseINI(fr);
        } catch (IOException e) {
            setError("Failed to parse mail config: " + AppCore.getMailConfigFilename());
            return false;
        }
        
        Properties smtp = data.get("smtp");
        if (smtp == null) {
            setError("Failed to parse mail config: smtp section not defined");
            return false;
        }
        
        String port = smtp.getProperty("port");
        if (port != null) {
            try {
                this.port = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                setError("Failed to parse mail config: invalid port");
                return false;
            }
        }
        
        if (this.port < 1 || this.port > 65535) {
            setError("Failed to parse mail config: invalid port");
            return false;
        }
        
        String rr = smtp.getProperty("maxEmailsPerRun");
        if (rr != null) {
            try {
                this.maxConcurrent = Integer.parseInt(rr);
            } catch (NumberFormatException e) {
                setError("Failed to parse mail config: maxEmailsPerRun");
                return false;
            }
        }
        
        String host = smtp.getProperty("smptServerAddress");
        if (host != null)
            this.host = host;
        
        String from = smtp.getProperty("from");
        if (smtp.get("from") == null) {
            setError("Failed to parse mail config: from address not defined");
            return false;
        }       
        this.from = from;
        
        String mail_from = smtp.getProperty("mail_from");
        if (smtp.get("mail_from") == null) {
            setError("Failed to parse mail config: mail_from address not defined");
            return false;
        }       
        this.mail_from = mail_from;
        
        String username = smtp.getProperty("username");
        if (smtp.get("username") == null) {
            setError("Failed to parse mail config: username not defined");
            return false;
        }       
        this.username = username;
        
        String password = smtp.getProperty("password");
        if (smtp.get("password") == null) {
            setError("Failed to parse mail config: password not defined");
            return false;
        }       
        this.password = password;
        
        return true;
    }
    
    public boolean doChecks() {
        logger.debug(ltag, "Doing Email checks");
        if (!this.readConfig())
            return false;
        
        if (!sendEmail(null, null, null, null, true))
            return false;
        
        return true;
    }
    
    public void doEmail(String[] emails, String[] subjects, String[] bodies, String[][] attachments, CallbackInterface cb) {
        if (!this.readConfig()) {
            logger.error(ltag, "Failed to read config");
            return;
        }
    
        ger.totalEmails = emails.length;
        ger.sentEmails = 0;
        
        ExecutorService executor = Executors.newFixedThreadPool(this.maxConcurrent);
        for (int i = 0; i < emails.length; i++) {
            final String femail = emails[i];
            final String fsubject = subjects[i];
            final String fbody = bodies[i];
            final String[] fattachments = attachments[i];
            final CallbackInterface fcb = cb;
            final int fi = i;
            Thread t = new Thread() {
                public void run() {
                    sendEmail(femail, fsubject, fbody, fattachments, false);
                    if (!ger.errText.isEmpty()) {
                        logger.error(ltag, "Terminating sending to " + femail);
                        return;
                    }
       
                    AppCore.moveToFolderNoTs(fattachments[0], Config.DIR_SENT, user, true);
                    ger.sentEmails++;
                    EmailerResult er = new EmailerResult();
                    copyFromMainResult(er);
                    if (cb != null)
                        cb.callback(er);
                }
            };
            executor.execute(t);
        }
             
        try {
            executor.shutdown();
            executor.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.debug(ltag, "Interrupted");
        } finally {
            if (!executor.isTerminated()) {
                logger.debug(ltag, "Cancel non-finished");
            }
            executor.shutdownNow();
        }

        if (!ger.errText.isEmpty())
            ger.status = EmailerResult.STATUS_ERROR;
        else
            ger.status = EmailerResult.STATUS_FINISHED; 
    }
    
    public boolean sendEmail(String email, String subject, String body, String[] attachments, boolean checkOnly) {
        String fileData = null;
        if (!checkOnly) {
            logger.debug(ltag, "Sedning " + email + " s=" + subject + " a=" + attachments[0] + " total="+attachments.length);
            fileData = AppCore.loadFile(attachments[0]);
            if (fileData == null) {
                setError("Failed to load file: " + attachments[0]);
                return false;
            }
        } 
        
        try {
            logger.debug(ltag, "Connecting to " + this.host + ":" + this.port);
            Socket socket = new Socket(this.host, this.port);
 
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String line; 
            line = reader.readLine();
            if (!isResponseOk(line, 220)) {
                setError("Invalid response from Protonmail. Please check the logs");
                socket.close();
                return false;
            }
            
            writer.println("EHLO localhost");
            logger.debug(ltag, "EHLO localhost");            
            while ((line = reader.readLine()) != null) {
                if (isInterimResponse(line))
                    continue;
                
                if (!isResponseOk(line, 250)) {
                    setError("Invalid response from Protonmail (EHLO). Please check the logs");
                    socket.close();
                    return false;
                }
                
                break;
            }

            String login = Base64.getEncoder().encodeToString((this.username).getBytes());
            String password = Base64.getEncoder().encodeToString((this.password).getBytes());

            writer.println("AUTH LOGIN");
            logger.debug(ltag, "AUTH LOGIN");

            line = reader.readLine();
            if (!isResponseOk(line, 334)) {
                setError("Invalid response from Protonmail (Login). Please check the logs");
                socket.close();
                return false;
            }
            
            writer.println(login);
            logger.debug(ltag, login);
            
            line = reader.readLine();
            if (!isResponseOk(line, 334)) {
                setError("Invalid response from Protonmail (Login). Please check the logs");
                socket.close();
                return false;
            }
            
            writer.println(password);
            logger.debug(ltag, password);
            
            line = reader.readLine();
            if (!isResponseOk(line, 235)) {
                setError("Proton auth failed. Please check your credentials");
                socket.close();
                return false;
            }

            writer.println("MAIL FROM: " + this.mail_from);
            logger.debug(ltag, "MAIL FROM: " + this.mail_from);
            
            line = reader.readLine();
            if (!isResponseOk(line, 250)) {
                setError("Invalid response from Protonmail (MAIL FROM). Please check the logs");
                socket.close();
                return false;
            }
            
                        
            if (checkOnly) {
                writer.println("QUIT");
                logger.debug(ltag, "QUIT");
                socket.close();
                return true;
            }
            
            
            writer.println("RCPT TO: " + email);
            logger.debug(ltag, "RCPT TO: " + email);
            
            line = reader.readLine();
            if (!isResponseOk(line, 250)) {
                setError("Invalid response from Protonmail (RCPT TO). Please check the logs");
                socket.close();
                return false;
            }
            
            writer.println("DATA");
            logger.debug(ltag, "DATA");
            
            line = reader.readLine();
            if (!isResponseOk(line, 354)) {
                setError("Invalid response from Protonmail (DATA). Please check the logs");
                socket.close();
                return false;
            }
            
            String fname = new File(attachments[0]).getName().replaceAll("@", "_");
            String attachment = Base64.getEncoder().encodeToString((fileData).getBytes());
            String boundary = this.generateBoundary();
            String msg = "Subject: " + subject + "\r\n" +
                    "Date: " + AppCore.getDate("" + (System.currentTimeMillis() /1000)) +"\r\n"+
                    "To: " + email + "\r\n" +
                    "From: " + this.from + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-type: multipart/mixed; boundary=\"" + boundary + "\"\r\n" +
                    "\r\n" +
                    "This is a multipart message in MIME format.\r\n\r\n" +
                    "--" + boundary  + "\r\n" +
                    "Content-Type: text/plain; charset=\"UTF-8\"\r\n\r\n" +
                    "" + body + "\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Disposition: attachment; filename=" + fname + "\r\n" +
                    "Content-Transfer-Encoding: Base64\r\n\r\n" +
                    "" + attachment + "\r\n" +
                    "--" + boundary + "--";
            

            writer.println(msg);   
            logger.debug(ltag, msg);
            writer.println(".");
            logger.debug(ltag, ".");
            
            line = reader.readLine();
            if (!isResponseOk(line, 250)) {
                setError("Proton rejected the message. Please check the logs");
                socket.close();
                return false;
            }

            writer.println("QUIT");
            logger.debug(ltag, "QUIT");
            socket.close();
        } catch (UnknownHostException ex) {
            logger.error(ltag, "Unknown network error: " + ex.getMessage());
            ger.status = EmailerResult.STATUS_ERROR;
            setError("Unknown network error. Please check the logs");
            return false;
        } catch (IOException ex) {
            setError("Failed to connect to the ProtonMail bridge. Make sure it is running");
            logger.error(ltag, "Failed to connect to the ProtonMail bridge: " + ex.getMessage());
            return false; 
        }

        return true;
    }
    
    protected String generateBoundary() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        int count = rand.nextInt(11) + 30; 
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        
        return buffer.toString();
    }

    private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    
    public boolean isInterimResponse(String response) {
        logger.debug(ltag, response);
        if (response.length() > 3 && response.charAt(3) == '-')
            return true;
        
        return false;
    }
    
    public boolean isResponseOk(String response, int dcode) {
        String[] v = response.split(" ");

        logger.debug(ltag, response);
        try {
            int rcode = Integer.parseInt(v[0]);
            if (rcode != dcode) {
                logger.error(ltag, "Invalid code returned from Protonmail: " + rcode);
                return false;
            }
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse response: " + response);
            return false;
        }
        
        return true;
        
    }

    
}
