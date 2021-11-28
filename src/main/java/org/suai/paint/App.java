package org.suai.paint;

import java.awt.*;
import javax.swing.*;

import org.suai.paint.client.Client;
import org.suai.paint.server.Server;

public class App {
    public static void main(String[] args) {
        // первый аргумент при вызове определяет, вызываем мы сервер или клиент
        if (args.length != 0) {
            if (args[0].toUpperCase().equals("-S")) {
                System.out.println("SERVER");
                Server server = new Server(false);
            } else if (args[0].toUpperCase().equals("-C")) {
                // 2-ой и 3-й - адрес и порт сервера, к которому будет подключаться клиент
				UIManager.put("OptionPane.cancelButtonText", "Отмена");
				UIManager.put("OptionPane.okButtonText", "Окей");
				
				JPanel ipPortPanel = new JPanel();
				ipPortPanel.setLayout(new GridLayout(2, 2, 5, 12));
				
				JTextField text1 = new JTextField(10);
				JTextField text2 = new JTextField(10);
				
				ipPortPanel.add(new JLabel("Введите ip: "));
				ipPortPanel.add(text1);
				ipPortPanel.add(new JLabel("Введите порт: "));
				ipPortPanel.add(text2);
				JOptionPane.showMessageDialog(null, ipPortPanel, "Конфигурация", JOptionPane.PLAIN_MESSAGE);
				
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Client client = new Client(text1.getText(), Integer.parseInt(text2.getText()));
                    }
                });
            } else {
                System.out.println("(-S/-С)");
            }
        } else {
            System.out.println("(-S/-C)");
        }
    }
}