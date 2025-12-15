package com.yurt.view;

import com.yurt.db.DBConnection;
import com.yurt.patterns.IObserver;      // Observer Interface
import com.yurt.patterns.OgrenciObserver; // Somut Observer Sınıfı

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonelEkrani extends JFrame {
    private JTable tblTalepler;
    private DefaultTableModel model;

    public PersonelEkrani() {
        // --- PENCERE AYARLARI ---
        setTitle("Yurt Yönetimi - Personel Paneli");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblBaslik = new JLabel("Bekleyen İzin Talepleri");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 18));
        lblBaslik.setBounds(20, 20, 300, 30);
        add(lblBaslik);

        // --- TABLO OLUŞTURMA ---
        model = new DefaultTableModel();
        model.addColumn("ID"); // İzin ID'si (Gizli anahtarımız)
        model.addColumn("Öğrenci Adı");
        model.addColumn("Soyadı");
        model.addColumn("Başlangıç");
        model.addColumn("Bitiş");
        model.addColumn("Durum");

        tblTalepler = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(tblTalepler);
        scrollPane.setBounds(20, 60, 640, 300);
        add(scrollPane);

        // --- BUTONLAR ---
        JButton btnOnayla = new JButton("ONAYLA ✅");
        btnOnayla.setBounds(20, 380, 150, 40);
        btnOnayla.setBackground(new Color(144, 238, 144)); // Açık yeşil
        add(btnOnayla);

        JButton btnReddet = new JButton("REDDET ❌");
        btnReddet.setBounds(190, 380, 150, 40);
        btnReddet.setBackground(new Color(255, 182, 193)); // Açık kırmızı
        add(btnReddet);

        JButton btnYenile = new JButton("Yenile 🔄");
        btnYenile.setBounds(510, 380, 150, 40);
        add(btnYenile);

        // Verileri Yükle
        talepleriListele();

        // --- BUTON İŞLEVLERİ ---

        // Onayla Butonu
        btnOnayla.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                durumGuncelle("ONAYLANDI");
            }
        });

        // Reddet Butonu
        btnReddet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                durumGuncelle("REDDEDILDI");
            }
        });

        // Yenile Butonu
        btnYenile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                talepleriListele();
            }
        });
    }

    private void talepleriListele() {
        model.setRowCount(0); // Tabloyu temizle
        try {
            Connection conn = DBConnection.getInstance().getConnection();

            // JOIN sorgusu ile öğrencinin adını da çekiyoruz
            String sql = "SELECT i.id, u.ad, u.soyad, i.baslangic_tarihi, i.bitis_tarihi, i.durum " +
                    "FROM izinler i " +
                    "JOIN users u ON i.ogrenci_id = u.id";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ad"),
                        rs.getString("soyad"),
                        rs.getDate("baslangic_tarihi"),
                        rs.getDate("bitis_tarihi"),
                        rs.getString("durum")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void durumGuncelle(String yeniDurum) {
        // Tablodan seçili satırı al
        int seciliSatir = tblTalepler.getSelectedRow();

        if (seciliSatir == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen tablodan bir işlem seçiniz!");
            return;
        }

        // Seçili satırdaki ID'yi al (0. sütun)
        int izinId = (int) model.getValueAt(seciliSatir, 0);

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            String sql = "UPDATE izinler SET durum = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, yeniDurum);
            ps.setInt(2, izinId);

            int sonuc = ps.executeUpdate();
            if (sonuc > 0) {
                talepleriListele(); // Listeyi güncelle

                // --- OBSERVER (GÖZLEMCİ) DESENİ BURADA DEVREYE GİRİYOR ---

                // Onaylanan öğrencinin adını tablodan alalım
                String ogrenciAd = (String) model.getValueAt(seciliSatir, 1);
                String ogrenciSoyad = (String) model.getValueAt(seciliSatir, 2);

                // Gözlemciyi (Observer) oluştur
                IObserver gozlemci = new OgrenciObserver(ogrenciAd + " " + ogrenciSoyad);

                // Bildirimi tetikle
                if (yeniDurum.equals("ONAYLANDI")) {
                    gozlemci.update("İzin talebiniz ONAYLANMIŞTIR. İyi yolculuklar! ✅");
                } else {
                    gozlemci.update("İzin talebiniz REDDEDİLMİŞTİR. Lütfen idareye başvurunuz. ❌");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
        }
    }
}