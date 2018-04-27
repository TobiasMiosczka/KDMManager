package com.github.tobiasmiosczka.cinema.KDMManager.helper;

import com.github.tobiasmiosczka.cinema.KDMManager.gui.IUpdate;
import com.github.tobiasmiosczka.cinema.KDMManager.pojo.EmailLogin;
import com.github.tobiasmiosczka.cinema.KDMManager.pojo.KDM;
import org.jdom2.JDOMException;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

public class EmailHelper {

    private static Collection<KDM> getKdmsFromZip(InputStream inputStream) throws IOException, JDOMException, ParseException {
        Collection<KDM> files = ZipHelper.unzip(inputStream);
        for (KDM file : files) {
            if (!file.getFileName().endsWith(".xml")) {
                files.remove(file);
            }
        }
        return files;
    }

    private static Collection<KDM> handleMessages(Message[] messages, IUpdate iUpdate) throws IOException, JDOMException, ParseException, MessagingException {
        Collection<KDM> kdms = new HashSet<>();
        int current = 0;
        for (Message message : messages) {
            iUpdate.onUpdateEmailLoading(current++, messages.length);
            if (message.getContentType().contains("multipart")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); ++i) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String fileName = bodyPart.getFileName();
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
                            || !StringHelper.isBlank(fileName)) {
                        if(fileName == null)
                            continue;
                        if (fileName.endsWith(".zip"))
                            kdms.addAll(getKdmsFromZip(bodyPart.getInputStream()));
                        if (fileName.endsWith(".xml"))
                            kdms.add(new KDM(bodyPart.getInputStream(), fileName));
                    }
                }
            }
        }
        iUpdate.onUpdateEmailLoading(current, messages.length);
        return kdms;
    }

    public static Collection<KDM> getKdmsFromEmail(Collection<EmailLogin> emailLogins, IUpdate iUpdate) throws MessagingException, IOException, JDOMException, ParseException {
        Collection<KDM> kdms = new HashSet<>();
        int current = 0;
        for (EmailLogin emailLogin : emailLogins) {
            iUpdate.onUpdateEmailBox(current++, emailLogins.size(), emailLogin.toString());
            Properties properties = new Properties();
            properties.put("mail.pop3.host", emailLogin.getHost());
            properties.put("mail.pop3.port", emailLogin.getPort());
            properties.put("mail.pop3.starttls.enable", emailLogin.isTls());
            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore(emailLogin.getProtocol());
            store.connect(emailLogin.getHost(), emailLogin.getUser(), emailLogin.getPassword());
            Folder emailFolder = store.getFolder(emailLogin.getFolder());
            emailFolder.open(Folder.READ_ONLY);
            kdms.addAll(handleMessages(emailFolder.getMessages(),iUpdate));
            emailFolder.close(false);
            store.close();
        }
        iUpdate.onUpdateEmailBox(current, emailLogins.size(), "");
        return kdms;
    }
}
