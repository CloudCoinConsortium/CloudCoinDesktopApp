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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Emailer extends Servant {
    EmailerResult ger;
    String ltag = "Emailer";
    
    String host;
    int port;
    String username, password, from;
    int maxConcurrent;
    
    public Emailer(String rootDir, GLogger logger) {
        super("Emailer", rootDir, logger);
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
        
        System.out.println("eee");
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
    
    
    
    public void doEmail(String[] emails, String[] subjects, String[] bodies, String[][] attachments, CallbackInterface cb) {
        System.out.println("GGGGG");
    
        if (!this.readConfig()) {
            logger.error(ltag, "Failed to read config");
            return;
        }
    
        ger.totalEmails = emails.length;
        ger.sentEmails = 0;
        
        ExecutorService executor = Executors.newFixedThreadPool(this.maxConcurrent);
        for (int i = 0; i < emails.length; i++) {
            System.out.println("sending: " + emails[i]);
            final String femail = emails[i];
            final CallbackInterface fcb = cb;
            final int fi = i;
            Thread t = new Thread() {
                public void run() {
                    System.out.println("runn " + femail);
                    try {
                        Thread.sleep(fi *1000);
                        
                    } catch(InterruptedException e) {}
                    
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
            System.out.println("shutting down");
            executor.shutdown();
            System.out.println("wait");
            executor.awaitTermination(1000, TimeUnit.SECONDS);
            System.out.println("waited");
        } catch (InterruptedException e) {
            logger.debug(ltag, "Interrupted");
        } finally {
            if (!executor.isTerminated()) {
                logger.debug(ltag, "Cancel non-finished");
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
        }
        System.out.println("shutdown finished");


        ger.status = EmailerResult.STATUS_FINISHED;
        
        
    }
    
    public void sendEmail(String email, String subject, String body, String[] attachment) {
 
        try {
            logger.debug(ltag, "Connecting to " + this.host + ":" + this.port);
            Socket socket = new Socket(this.host, this.port);
 
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String line; 
            
            line = reader.readLine();
            System.out.println(line);
            logger.debug(ltag, line);
            if (!isResponseOk(line, 220)) {
                setError("Invalid response from Protonmail. Please check the logs");
                return;
            }
            
            writer.println("EHLO localhost");
            logger.debug(ltag, "EHLO localhost");            
            while ((line = reader.readLine()) != null) {
                System.out.println("l="+ line);
                if (isInterimResponse(line))
                    continue;
                
                if (!isResponseOk(line, 250)) {
                    setError("Invalid response from Protonmail (EHLO). Please check the logs");
                    return;
                }
                
                System.out.println("fl="+ line);
                break;
            }

            System.out.println("fini");

            
            String login = Base64.getEncoder().encodeToString(("CloudCoinBanker@protonmail.com").getBytes());
            String password = Base64.getEncoder().encodeToString(("dummy").getBytes());

            writer.println("AUTH LOGIN");

            line = reader.readLine();
            System.out.println("L1="+line);
            if (!isResponseOk(line, 334)) {
                setError("Invalid response from Protonmail (Login). Please check the logs");
                return;
            }
            
            writer.println(login);
            line = reader.readLine();
            System.out.println("L2="+line);
            if (!isResponseOk(line, 334)) {
                setError("Invalid response from Protonmail (Login). Please check the logs");
                return;
            }
            
            writer.println(password);
            line = reader.readLine();
             System.out.println("L3="+line);
            if (!isResponseOk(line, 235)) {
                setError("Proton auth failed. Please check your credentials");
                return;
            }
            
            writer.println("MAIL FROM: <CloudCoinBanker@protonmail.com>");
            line = reader.readLine();
            System.out.println("L4="+line);
            if (!isResponseOk(line, 250)) {
                setError("Invalid response from Protonmail (MAIL FROM). Please check the logs");
                return;
            }
            
            writer.println("RCPT TO: miroch.alexander@gmail.com");
            line = reader.readLine();
            System.out.println("L5="+line);
            if (!isResponseOk(line, 250)) {
                setError("Invalid response from Protonmail (RCPT TO). Please check the logs");
                return;
            }
            
            writer.println("DATA");
            line = reader.readLine();
            System.out.println("L6="+line);
            if (!isResponseOk(line, 354)) {
                setError("Invalid response from Protonmail (DATA). Please check the logs");
                return;
            }
            
            String msg = "Subject: mysubj\r\n" +
                    "Date: 12 Apr, 2020\r\n"+
                    "To: miroch.alexander@gmail.com\r\n" +
                    "From: CloudCoinBanker@protonmail.com\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-type: multipart/mixed; boundary=\"XXXBoundary\"\r\n" +
                    "\r\n" +
                    "This is a multipart message in MIME format.\r\n\r\n" +
                    "--XXXBoundary\r\n" +
                    "Content-Type: text/plain; charset=\"UTF-8\"\r\n\r\n" +
                    "Please do not reply. Мне please\r\n" +
                    "--XXXBoundary\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Disposition: attachment; filename=cloudcoins.stack\r\n" +
                    "Content-Transfer-Encoding: Base64\r\n\r\n" +
                    "ewoJImNlbGVicml1bSI6IFt7CgkJIm5uIjogIjIiLAoJCSJzbiI6ICI4IiwKCQkiYW4iOiBbCgkJ" +
"CSI3YWRiYzgyNWQxMThjMDcxNGY0OWJlYTY1NDExNjU3ZCIsICIxZWVhZTllMjYyYjMxNzhiMTJk" +
"NTU3MWYxNGRkMjdmZiIsICI0MmY2Y2MwOTA2NzY3ZGEzOTExYjYxOTg5ODY5MzcyYSIsICJlYjEx" +
"MmYyOWZkODJmNjM0OTkyNDljNDAxYWIxZjM3MCIsICJiOTliYzNhM2YxZDRmNzE4MTZmYWRhN2Y2" +
"YjNlYzg3NiIsCgkJCSI3OGIzOGJkYzYyZTZhYTQ2YWI3MGI4YjdmZGNiMjViZSIsICI0Y2FhMjgz" +
"NmE5ODc1YTNmYzkxYTc1NGZmNDRhYmRmNCIsICIwMmMxNGM1MTIxZmMzMjVjM2M4ZTY5YmNjZTg3" +
"YWZhYSIsICI4MGNiM2IyODVlMjNmNzU1ZmNlN2Q2ODRmYmM1YjExNiIsICI5NzQwMzc2ZWUzMTYx" +
"Mzk3NDY2NDU2ZDAzZjZiNmZlNyIsCgkJCSJjNDdmZmJmYTIwNGVkODBiZWMzMDEwZjM2MDVmNDdk" +
"MCIsICIyYzJkZjIyOTc2NDhmYzc0NWUwYWFhNGQ3ODkxYmMwNCIsICIxOWZhMzIxM2Y3MGU5YmQ0" +
"ZDA4YTllNTIxYWJjMTJkMCIsICJhZmNiYmY1NDkzOWQwNjk0ODUxMGUzMTJhZWNhMjJmOCIsICIz" +
"OTU3MTI4ZDBhNzkwMjliNzA4MmQ3OWFmODhhZTVlNCIsCgkJCSI4MTQzYWU0NTAxNWViNGVmYzk3" +
"NGI3MDE3YjVhN2JlYSIsICJiOWNhNmYwMTA5NzA2Y2U4NWRhMWNhOGNiOWE5MDI5YiIsICI4ZDVi" +
"OTIzM2M1YTRlMjE5YWUxZWYwY2E3NjVkODg2MCIsICJmNGIxYjdkNGZmMDU0YzEzZmQ4MWQ4ODQ0" +
"ZGFhN2NlMiIsICJjODFkNmIyOThhMjJjYTdjODU0NGUwYTE1NDY3MTk0YyIsCgkJCSIxNThkM2Qz" +
"Y2I1OWNjNzZjZDM4MTdlMTJkMzBkMTUyZSIsICIwY2FmODU2N2U4NzYwYjM1N2M3ZDFmYTJlMWQ3" +
"NzBkNSIsICIwYTVlMTc2YjMwZTNmY2I1MTRiMWIxMTE0N2U0MTQ0ZCIsICJiN2QxN2ZhNzEzZTEw" +
"MTk3OGE0Yjg1OWQ0M2UyY2EzOSIsICI2MmY1OTc5ODA4MzkwODE5MGVjOTcxMzM4OTIzZTFlYyIK" +
"CQldLAoJCSJwb3duIjogInBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHAiLAoJCSJlZCI6ICIwLTAw" +
"MDAiLAoJCSJhb2lkIjogW10KCX1dfQo=\r\n" +
                    "--XXXBoundary--";
            
            System.out.println("w=" + msg);
            writer.println(msg);
            
            writer.println(".");
            
                    
            line = reader.readLine();
            System.out.println("L7="+line);  
            if (!isResponseOk(line, 250)) {
                setError("Proton rejected the message. Please check the logs");
                return;
            }
            
            writer.println("QUIT");
                    
                    
                    
            

            System.out.flush();
            if (1==1)
                return;

            

            writer.println("MAIL FROM: \"<CloudCoinBanker@protonmail.com>\"");
            logger.debug(ltag, "EHLO localhost");
            
            line = reader.readLine();
            System.out.println(line);
            logger.debug(ltag, line);

 
            System.out.println("done");
 
 
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
            logger.error(ltag, "Unknown network error: " + ex.getMessage());
            ger.status = EmailerResult.STATUS_ERROR;
            setError("Unknown network error. Please check the logs");
            return;
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
            setError("Failed to connect to the ProtonMail bridge. Make sure it is running");
            logger.error(ltag, "Failed to connect to the ProtonMail bridge: " + ex.getMessage());
            return;
            
        }
    }
    
    public boolean isInterimResponse(String response) {
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
            System.out.println("code="+rcode);
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse response: " + response);
            return false;
        }
        
        return true;
        
    }

    
}
