package com.github.tobiasmiosczka.cinema.KDMManager.gui;

import com.github.tobiasmiosczka.cinema.KDMManager.helper.EmailHelper;
import com.github.tobiasmiosczka.cinema.KDMManager.helper.FtpHelper;
import com.github.tobiasmiosczka.cinema.KDMManager.helper.XmlHelper;
import com.github.tobiasmiosczka.cinema.KDMManager.pojo.*;
import org.jdom2.JDOMException;

import javax.mail.MessagingException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Container;
import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

public class Window extends JFrame {

    private JList<EmailLogin>   lEmailLoginList;
    private JList<FtpLogin>     lFtpLoginList;

    private final DefaultListModel<FtpLogin> dlmFtpLogins = new DefaultListModel<>();
    private final DefaultListModel<EmailLogin> dlmEmailLogin = new DefaultListModel<>();

    private JLabel lbResult;

    private Config config = new Config();

    private void loadConfig() throws IOException, JDOMException {
        this.config = XmlHelper.loadConfig(new FileInputStream("config.xml"));
        updateEmailLoginList();
        updateFtpLoginList();
    }

    private void saveConfig() {
        try {
            XmlHelper.saveConfig(config, new FileOutputStream("config.xml"));
        } catch (IOException e) {
            //TODO: implement
            e.printStackTrace();
        }
    }

    public Window() throws IOException, JDOMException {
        loadConfig();
        this.init();
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    private void init() {
        this.setTitle("KDMManager");
        this.setLayout(null);
        this.setResizable(false);
        Container c = this.getContentPane();
        c.setPreferredSize(new Dimension(600, 635));

        JLabel lbEmailLogins = new JLabel("Email Logins");
        lbEmailLogins.setBounds(5, 5, 190, 30);
        c.add(lbEmailLogins);

        lEmailLoginList = new JList<>();
        lEmailLoginList.setModel(dlmEmailLogin);
        lEmailLoginList.setBounds(5, 40, 590, 200);
        c.add(lEmailLoginList);

        JButton btAddEmailLogin = new JButton("Add");
        btAddEmailLogin.addActionListener(a -> {
            EmailLogin emailLogin = EmailLoginDialog.getEmailLogin(new EmailLogin("", 21, "", "", "pop3s", "INNOX", false));
            if (emailLogin != null){
                config.getEmailLogins().add(emailLogin);
                saveConfig();
                updateEmailLoginList();
            }
        });
        btAddEmailLogin.setBounds(5, 245, 190, 30);
        c.add(btAddEmailLogin);

        JButton btEditEmailLogin = new JButton("Edit");
        btEditEmailLogin.addActionListener(a -> {
            if (lEmailLoginList.getSelectedIndices().length != 1)
                return;
            EmailLogin emailLogin = EmailLoginDialog.getEmailLogin(lEmailLoginList.getSelectedValue());
            if (emailLogin != null) {
                config.getEmailLogins().set(lEmailLoginList.getSelectedIndex(), emailLogin);
                saveConfig();
                updateEmailLoginList();
            }
        });
        btEditEmailLogin.setBounds(205, 245, 190, 30);
        c.add(btEditEmailLogin);

        JButton btDeleteEmailLogin = new JButton("Delete");
        btDeleteEmailLogin.addActionListener(a -> {
            lEmailLoginList.getSelectedValuesList().forEach(config.getEmailLogins()::remove);
            saveConfig();
            updateEmailLoginList();
        });
        btDeleteEmailLogin.setBounds(405, 245, 190, 30);
        c.add(btDeleteEmailLogin);

        JLabel lbFtpLogins = new JLabel("FTP Logins");
        lbFtpLogins.setBounds(5, 280, 190, 30);
        c.add(lbFtpLogins);

        lFtpLoginList = new JList<>();
        lFtpLoginList.setModel(dlmFtpLogins);
        lFtpLoginList.setBounds(5, 315, 590, 200);
        c.add(lFtpLoginList);

        JButton btAddFtpLogin = new JButton("Add");
        btAddFtpLogin.addActionListener(a -> {
            FtpLogin ftpLogin = FtpLoginDialog.getFtpLogin(new FtpLogin("", 995, "", "", ""));
            if (ftpLogin != null) {
                config.getFtpLogins().add(ftpLogin);
                saveConfig();
                updateFtpLoginList();
            }
        });
        btAddFtpLogin.setBounds(5, 520, 190, 30);
        c.add(btAddFtpLogin);

        JButton btEditFtpLogin = new JButton("Edit");
        btEditFtpLogin.addActionListener(a -> {
            if (lFtpLoginList.getSelectedIndices().length != 1)
                return;
            FtpLogin ftpLogin = FtpLoginDialog.getFtpLogin(lFtpLoginList.getSelectedValue());
            if (ftpLogin != null) {
                config.getFtpLogins().set(lFtpLoginList.getSelectedIndex(), ftpLogin);
                saveConfig();
                updateFtpLoginList();
            }
        });
        btEditFtpLogin.setBounds(205, 520, 190, 30);
        c.add(btEditFtpLogin);

        JButton btDeleteFtpLogin = new JButton("Delete");
        btDeleteFtpLogin.addActionListener(a -> {
            lFtpLoginList.getSelectedValuesList().forEach(config.getFtpLogins()::remove);
            saveConfig();
            updateFtpLoginList();
        });
        btDeleteFtpLogin.setBounds(405, 520, 190, 30);
        c.add(btDeleteFtpLogin);

        lbResult = new JLabel();
        lbResult.setBounds(5, 555, 590, 30);
        c.add(lbResult);

        JButton btLoadKdms = new JButton("Load KDMs");
        btLoadKdms.addActionListener(a -> loadKdms());
        btLoadKdms.setBounds(5, 600, 590, 30);
        c.add(btLoadKdms);
    }

    private void loadKdms() {
        Collection<KDM> kdms = null;
        long start = System.currentTimeMillis();
        try {
            kdms = EmailHelper.getKdmsFromEmail(config.getEmailLogins());
        } catch (MessagingException | JDOMException | ParseException | IOException e) {
            //TODO: implement
            e.printStackTrace();
        }
        if(kdms == null) {
            //TODO: implement
            return;
        }
        try {
            FtpHelper ftpHelper = new FtpHelper(config.getFtpLogins());
            ftpHelper.uploadFiles(kdms);
        } catch (IOException | FtpException e) {
            //TODO: implement
            e.printStackTrace();
        }
        long diff = System.currentTimeMillis() - start;
        lbResult.setText("Loaded " + kdms.size() + " KDMs after " + diff / 1000 + " seconds.");
    }

    private void updateEmailLoginList() {
        dlmEmailLogin.removeAllElements();
        config.getEmailLogins().forEach(dlmEmailLogin::addElement);
    }

    private void updateFtpLoginList() {
        dlmFtpLogins.removeAllElements();
        config.getFtpLogins().forEach(dlmFtpLogins::addElement);
    }
}
