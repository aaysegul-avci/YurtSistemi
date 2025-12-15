package com.yurt.view;

import com.yurt.db.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class OgrenciEkrani extends JFrame {
    private String kullaniciAdi; // Giriş yapan öğrencinin kullanıcı adı
    private JTable tblIzinler;
    private DefaultTableModel model;

    public OgrenciEkrani(String kullaniciAdi) {
        this.kullaniciAdi = kullaniciAdi;

        setTitle("Öğrenci Paneli");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        // --- 1. KİŞİSEL BİLGİLER BÖLÜMÜ ---
        JLabel lblBaslik = new JLabel("Hoşgeldin, " + kullaniciAdi);
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        lblBaslik.setBounds(20, 10, 300, 30);
        add(lblBaslik);

        JLabel lblBilgi = new JLabel("Bilgileriniz yükleniyor...");
        lblBilgi.setBounds(20, 40, 400, 20);
        add(lblBilgi);

        // Veritabanından Ad-Soyad çekelim
        ogrenciBilgileriniGetir(lblBilgi);

        // --- 2. İZİN İSTEME BUTONU ---
        JButton btnIzinIste = new JButton("Yeni İzin Talebi Oluştur");
        btnIzinIste.setBounds(20, 80, 200, 30);
        btnIzinIste.setBackground(Color.ORANGE);
        add(btnIzinIste);

        // --- 3. GEÇMİŞ İZİNLER TABLOSU ---
        JLabel lblGecmis = new JLabel("İzin Geçmişim:");
        lblGecmis.setBounds(20, 130, 200, 20);
        add(lblGecmis);

        model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Başlangıç");
        model.addColumn("Bitiş");
        model.addColumn("Durum"); // State Deseni burada görünecek

        tblIzinler = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(tblIzinler);
        scrollPane.setBounds(20, 160, 540, 180);
        add(scrollPane);

        // Tabloyu doldur
        izinleriListele();

        // Butona Tıklama Olayı (Basit Hali)
        btnIzinIste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                izinIste();
            }
        });
    }

    private void ogrenciBilgileriniGetir(JLabel label) {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT ad, soyad, tc_no FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, kullaniciAdi);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String ad = rs.getString("ad");
                String soyad = rs.getString("soyad");
                String tc = rs.getString("tc_no");
                label.setText("Ad Soyad: " + ad + " " + soyad + " | TC: " + tc);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void izinleriListele() {
        model.setRowCount(0); // Tabloyu temizle
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            // Kullanıcı ID'sini bul
            String idSql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement psId = conn.prepareStatement(idSql);
            psId.setString(1, kullaniciAdi);
            ResultSet rsId = psId.executeQuery();

            if (rsId.next()) {
                int userId = rsId.getInt("id");

                // İzinleri çek
                String izinSql = "SELECT * FROM izinler WHERE ogrenci_id = ?";
                PreparedStatement psIzin = conn.prepareStatement(izinSql);
                psIzin.setInt(1, userId);
                ResultSet rsIzin = psIzin.executeQuery();

                while (rsIzin.next()) {
                    model.addRow(new Object[]{
                            rsIzin.getInt("id"),
                            rsIzin.getDate("baslangic_tarihi"),
                            rsIzin.getDate("bitis_tarihi"),
                            rsIzin.getString("durum") // BEKLEMEDE, ONAYLANDI vs.
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void izinIste() {
        // Şimdilik basit bir ekleme yapalım, sonra State deseni ile güzelleştireceğiz
        try {
            Connection conn = DBConnection.getInstance().getConnection();

            // Önce ID'yi al
            String idSql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement psId = conn.prepareStatement(idSql);
            psId.setString(1, kullaniciAdi);
            ResultSet rsId = psId.executeQuery();

            if (rsId.next()) {
                int userId = rsId.getInt("id");

                // Rastgele bir tarihle izin ekle (Test amaçlı)
                String insertSql = "INSERT INTO izinler (ogrenci_id, baslangic_tarihi, bitis_tarihi, durum) VALUES (?, CURDATE(), CURDATE() + 2, 'BEKLEMEDE')";
                PreparedStatement psIns = conn.prepareStatement(insertSql);
                psIns.setInt(1, userId);
                psIns.executeUpdate();

                JOptionPane.showMessageDialog(this, "İzin talebi gönderildi!");
                izinleriListele(); // Tabloyu yenile
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}