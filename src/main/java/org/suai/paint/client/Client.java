package org.suai.paint.client;

import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.EmptyBorder;

public class Client {
    //Переменные сети
    public boolean isConnected = false;
    public String serverHost = null;
    public int serverPort;
    public Socket clientSocket;
    public BufferedReader readSocket;
    public BufferedWriter writeSocket;
	public static int x;
    public static int y;

    //Переменные графики
	boolean flag = false;
	public boolean flag1 = false;
	public boolean flag2 = false;
	public boolean flag3 = false;
	public boolean flag4 = false;
    public JFrame frame;
    public JToolBar toolbar; // кнопки
	public JButton menuButton;
    public JPanel menu; // меню
    public JLabel existLabel; // доска существует
    public JLabel notFoundLabel; // доска не неайдена
	public JLabel connectionLabel; // когда идёт присоединение к доске
	public JLabel connectionLabel1;
	public JLabel alreadyConnected; //если пытаешься подключиться к доске на которой ты находишься
	public JLabel inviteLabel = null;//приглашение
    public BoardPanel boardPanel; // отображение доски
    public BufferedImage board = null; // доска
    public Graphics2D graphics;
    public Color mainColor;
    public int size = 10; // размер кисти
	public String name = null; //имя пользователя
	private JTextField textField = null;
	private final ArrayList<String> nameOfBoards = new ArrayList<>();
	JFrame boardsFrame;
	
	//Переменные чата (граф. интерфейс)
	public JFrame frameChat;
	public JTextField messageTextFieldForChat;
	public JTextArea contentPanelChat = null;
	public JButton sendChat;
	public JScrollPane scroll;
	
	//Панель где всё отображается
    class BoardPanel extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(board, 0, 0, this);
        }
    }
	
	//Класс, чтобы нельзя было элементы рисования перенести за необходимую линию
	private class MouseMoution {
		public MouseMoution(JButton button) {
			button.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if(!e.isMetaDown()){
						x = e.getX();
						y = e.getY();
					}
				}
			});
			button.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) {			
					if(!e.isMetaDown()){								
						Point p = button.getLocation();
						if (((p.x + e.getX() - x) < 0) && (((p.y + e.getY() - y) < 0)))
							button.setLocation(0, 0);
						else if (((p.x + e.getX() - x) > 60) && (((p.y + e.getY() - y) < 0)))
							button.setLocation(60, 0);
						else if (((p.x + e.getX() - x) > 60) && (((p.y + e.getY() - y) > 580)))
							button.setLocation(60, 580);
						else if (((p.x + e.getX() - x) < 0) && (((p.y + e.getY() - y) > 580)))
							button.setLocation(0, 580);
						else if ((p.x + e.getX() - x) < 0)
							button.setLocation(0, p.y + e.getY() - y);
						else if ((p.x + e.getX() - x) > 60)
							button.setLocation(60, p.y + e.getY() - y);
						else if ((p.y + e.getY() - y) > 580)
							button.setLocation(p.x + e.getX() - x, 580);
						else if ((p.y + e.getY() - y) < 0)
							button.setLocation(p.x + e.getX() - x, 0);
						else
							button.setLocation(p.x + e.getX() - x, p.y + e.getY() - y);
					}
				}
			});
		}
	}

	//Класс считывания данных с сервера
    class Receiver extends Thread {
        String message;
        String[] splitMessage;
		
        public Receiver() {
            this.start();
        }
		
        public void run() {
            try {
                try {
                    while (true) {
                        message = readSocket.readLine();
						if(message == null)
							continue;
						 if (message.contains("NAMES:")) {
                            nameOfBoards.clear();
                            message = message.replaceFirst("NAMES:", "");
                            splitMessage = message.split(";");
                            for (int i = 0; i < splitMessage.length; ++i) {
                                nameOfBoards.add(splitMessage[i]);
                            }
                            continue;
                        }
                        splitMessage = message.split(" ", 2);
						if(splitMessage[0].equals("MESSAGE")) {//для чата
							if(splitMessage[1].equals("@quit")) { //Информация о том, что пользователь покинул чат
								contentPanelChat.append("@\u0053\u0045\u0052\u0056\u0045\u0052\u003a \u0412\u044b \u043f\u043e\u043a\u0438\u043d\u0443\u043b\u0438 \u0441\u0435\u0440\u0432\u0435\u0440\u0021\n");
								frameChat.repaint();
								this.sleep(1000);
								//System.out.println("@SERVER: you have left the server");
								System.exit(0);
							} else if(splitMessage[1].startsWith("@name ")) {
								String[] split = splitMessage[1].split(" ", 2);
								name = split[1]; 
								inviteLabel.setText(name + ", \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u0434\u043e\u0441\u043a\u0438");
								//Информация пользователю о том, что он успешно переименован
								contentPanelChat.append("@SERVER: \u0422\u0435\u043f\u0435\u0440\u044c \u0412\u0430\u0448\u0435 \u0438\u043c\u044f @" + name + "\n");
								frameChat.repaint();
								frame.repaint();
							} else {
								String chatMessage = splitMessage[1];
								contentPanelChat.append(chatMessage + "\n");
								frameChat.repaint();
							}
							//System.out.println(chatMessage);
						} else if (splitMessage[0].equals("CREATE")) {
							//Созданиие доски
                            if (splitMessage[1].equals("OK")) {
                                board = new BufferedImage(800, 700, BufferedImage.TYPE_INT_RGB);
                                graphics = board.createGraphics();
                                graphics.setColor(Color.white);
                                graphics.fillRect(0, 0, 800, 700);
                                isConnected = true;
                                frame.remove(menu);
								frame.add(menuButton);
								frame.add(toolbar);
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("EXISTS")) {
                                menu.add(existLabel);
								frame.add(menuButton);
								frame.remove(toolbar);
                                frame.repaint();
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
							//Подключение к доске
                            if (splitMessage[1].equals("OK")) {
								connectionLabel = new JLabel("\u041f\u0440\u0438\u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u0435\u0020\u043a\u0020\u0434\u043e\u0441\u043a\u0435\u0020\"" + textField.getText() + "\"...");
								connectionLabel.setBounds(20, 85, 300, 30);
								if (!flag4) menu.add(connectionLabel);
								else menu.add(connectionLabel1);
                                int[] rgbArray = new int[560000];
                                for (int i = 0; i < rgbArray.length; i++) {
                                    message = readSocket.readLine();
                                    rgbArray[i] = Integer.parseInt(message);
                                }
                                board = new BufferedImage(800, 700, BufferedImage.TYPE_INT_RGB);
                                board.setRGB(0, 0, 800, 700, rgbArray, 0, 800);
                                graphics = board.createGraphics();
                                isConnected = true;
                                frame.remove(menu);
								if (!flag4) menu.remove(connectionLabel);
								else { 
									menu.remove(connectionLabel1);
									flag4 = false;
								}
								boardsFrame.setVisible(false);
                                frame.add(boardPanel);
								frame.add(toolbar);
                                frame.repaint();
                            } else if (splitMessage[1].equals("NOT FOUND")) {
                                menu.add(notFoundLabel);
								frame.remove(toolbar);
								frame.add(menuButton);
                                frame.repaint();
                            } else if (splitMessage[1].equals("THIS")) {
								menu.add(alreadyConnected);
								frame.repaint();
							}	
                        } else {
							//Рисование на доске
							if(splitMessage[0].equals("MESSAGE"))
								continue;
                            splitMessage = message.split(" ", 4);
                            int color = Integer.parseInt(splitMessage[0]);
                            int coordX = Integer.parseInt(splitMessage[1]);
                            int coordY = Integer.parseInt(splitMessage[2]);
                            int size = Integer.parseInt(splitMessage[3]);

                            graphics.setColor(new Color(color));
                            graphics.fillOval(coordX, coordY, size, size);
                            boardPanel.repaint();
                        }
                    }
                } catch (Exception err) {
                    System.out.println(err.toString());
                    readSocket.close();
                    writeSocket.close();
                }
            } catch (IOException err) {
                System.out.println(err.toString());
            }
        }
    }
	
    public Client(String serverHost, int serverPort) {
		
		UIManager.put("OptionPane.yesButtonText", "Да");
		UIManager.put("OptionPane.noButtonText", "Нет");
		UIManager.put("OptionPane.cancelButtonText", "Отмена");
		UIManager.put("OptionPane.okButtonText", "Окей");
		
		//Диалоговое окно ввода имени
		while(name == null) {
			name = JOptionPane.showInputDialog(null, "Введите имя:" , "Авторизация", JOptionPane.QUESTION_MESSAGE);
			if(name == null)
				System.exit(0);
				//JOptionPane.showMessageDialog(null, "Вы ничего не ввели, повторите попытку", "Ошибка", JOptionPane.WARNING_MESSAGE);
		}
		//Подключение сети
        try {
            try {
                this.serverHost = serverHost;
                this.serverPort = serverPort;
                clientSocket = new Socket(serverHost, serverPort);
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				//BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, "cp866"));
				//System.out.println("Введите имя, например: \"@name Иван\" или просто \"Иван\"\nВаше имя: ");
				//name = stdIn.readLine();
				writeSocket.write(name + "\n");
				writeSocket.flush();
				String fromServer = readSocket.readLine();
				name = fromServer;
				JOptionPane.showMessageDialog(null, "Ваше имя: " + name, "Полученное имя", JOptionPane.INFORMATION_MESSAGE);
				//System.out.println("@SERVER: Your name - " + name + "\n");
                new Receiver();
            } catch (IOException err) {
                System.out.println(err.toString());
                readSocket.close();
                writeSocket.close();
            }
        } catch (IOException err) {
            System.out.println(err.toString());
        }
		
		//Чат (граф. интерфейс)
		//Переменные чата (граф. интерфейс)
		frameChat = new JFrame("\u0427\u0430\u0442");//"Чат"
		frameChat.setSize(400, 600);
		frameChat.setResizable(false); // нельзя менять размер окна
        frameChat.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); //скрыть кадр, но сохранить приложение запущено.
		frameChat.setLocationRelativeTo(null);
        frameChat.setLayout(null);
		frameChat.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("chatIcon.png"))).getImage());
		
		//Панель для ввода данных
		messageTextFieldForChat = new JTextField();
		messageTextFieldForChat.setBounds(10, 500, 280, 50);
		frameChat.add(messageTextFieldForChat);
		
		//Окно чата
		contentPanelChat = new JTextArea();
		contentPanelChat.setBounds(10, 10, 350, 450);
		contentPanelChat.setEditable(false);
		contentPanelChat.setLineWrap(true);
		frameChat.add(contentPanelChat);
		
		//Cкролинг
		scroll= new JScrollPane(contentPanelChat, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setSize(350, 450);
        scroll.setLocation(10, 10);      
        frameChat.add(scroll);
		
		//Кнопка для отправки
		sendChat = new JButton("\u27a4");//Значок отправки (➤)
		sendChat.setBounds(290, 500, 80, 50); // размещение
		sendChat.setBorderPainted(true); // рисовать рамку
		sendChat.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
		sendChat.setOpaque(true); // не прозрачность
		sendChat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { //отправка сообщений
				String message = messageTextFieldForChat.getText();
				messageTextFieldForChat.setText("");
				if((message != null) && (!message.equals(" "))) {
					try {
						String allMessage = "MESSAGE " + message + "\n";
						contentPanelChat.append("\t\t\u0412\u044b: " + message + "\n");
						frameChat.repaint();
						writeSocket.write(allMessage);
						writeSocket.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		frameChat.add(sendChat);
		contentPanelChat.append("\u0414\u043e\u0431\u0440\u043e \u043f\u043e\u0436\u0430\u043b\u043e\u0432\u0430\u0442\u044c \u0432 \u0447\u0430\u0442\u002c " + name + "!\n");
		frameChat.repaint();
		
		//Код ниже позволяет отправлять сообщения с чата с помощью "Enter"
		frameChat.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"clickButton");

		frameChat.getRootPane().getActionMap().put("clickButton",new AbstractAction(){
				public void actionPerformed(ActionEvent ae)
				{
					sendChat.doClick();
				}
		});
		frameChat.setVisible(false);//изначально чат не виден
		
		//Графический интерфейс paint
        //Графика (окно рисования)
        frame = new JFrame("\u041c\u043d\u043e\u0433\u043e\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c\u0441\u043a\u0438\u0439 \u0050\u0061\u0069\u006e\u0074");
        frame.setSize(900, 700); // размер окна
        frame.setResizable(false); // нельзя менять размер окна 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
		frame.setLocationRelativeTo(null);
        frame.setLayout(null);
		frame.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("mainIcon.png"))).getImage());
        frame.setVisible(true);

        //Панель рисования
        boardPanel = new BoardPanel();
        boardPanel.setBounds(100, 0, 800, 700);
        boardPanel.setOpaque(true);

        //Панель меню
        menu = new JPanel();
        menu.setBounds(40, 0, 800, 700);
        menu.setBackground(mainColor);
        menu.setLayout(null);
        frame.add(menu);
		
		//Текстовое поле
        textField = new JTextField();
        textField.setBounds(20, 45, 100, 30);
        textField.setText("\u0414\u043e\u0441\u043a\u0430 \u0031"); //доска 1
        menu.add(textField);
		
        //Доска с таким именем уже существует UTF-16
        existLabel = new JLabel("\u0414\u043e\u0441\u043a\u0430 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442");
        existLabel.setBounds(20, 85, 300, 30);
		
        //Доска с таким именем не существует UTF-16
        notFoundLabel = new JLabel("\u0414\u043e\u0441\u043a\u0430 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442");
        notFoundLabel.setBounds(20, 85, 300, 30);
		
		//Вы уже подключены к этой доске
		alreadyConnected = new JLabel("\u0412\u044b\u0020\u0443\u0436\u0435\u0020\u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u044b\u0020\u043a\u0020\u044d\u0442\u043e\u0439\u0020\u0434\u043e\u0441\u043a\u0435");
		alreadyConnected.setBounds(20, 85, 300, 30);
	
        //Приглашение
        //Введите имя доски UTF-16
        inviteLabel = new JLabel(name + ", \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u0434\u043e\u0441\u043a\u0438");
        inviteLabel.setBounds(20, 5, 200, 30);
        menu.add(inviteLabel);
		
		//Панель с инструментами
		toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        toolbar.setBounds(0, 40, 100, 660); // размещение
        toolbar.setLayout(null); // элементы размещаем сами
        toolbar.setFloatable(false); // нельзя перетаскивать
        toolbar.setBorderPainted(false); // без рамок
        toolbar.setBackground(mainColor); // устанавливаем цвет панели
		
		//Палитра 
		JFrame paletteFrame = new JFrame("\u041f\u0430\u043b\u0438\u0442\u0440\u0430");
		paletteFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		paletteFrame.setSize(500, 400);
		paletteFrame.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("paletteIcon.png"))).getImage());
		paletteFrame.setVisible(false);
		
		JColorChooser paletteChooser = new JColorChooser();
		paletteFrame.add(paletteChooser, BorderLayout.CENTER);
		
		JButton colorChooseButton = new JButton("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0446\u0432\u0435\u0442"); //кнопка выбрать цвет
		colorChooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if(flag1) {
					mainColor = Color.white;
				} else if ((!flag1) && ((flag2) || (flag3))) 
					mainColor = paletteChooser.getColor();
				paletteFrame.setVisible(false);
            }
        });
		paletteFrame.add(colorChooseButton, BorderLayout.SOUTH);	
		
        JButton palette = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("palette.png")));
        palette.setBounds(45, 0, 40, 40);
        palette.setBorderPainted(false);
        palette.setBackground(Color.lightGray);
        palette.setOpaque(false);
        palette.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
					if(paletteFrame.isVisible())
						paletteFrame.setVisible(false);
					else
						paletteFrame.setVisible(true);
            }
        });
		toolbar.add(palette);
		new MouseMoution(palette);
		
		
		//Размер 10
        JButton size10Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size10.png")));
        size10Button.setBounds(0, 0, 40, 40);
        size10Button.setBorderPainted(false);
        size10Button.setBackground(Color.lightGray);
        size10Button.setOpaque(false);
        size10Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 10;
				if (flag2) {
					size = 2;
				}
            }
        });	
		toolbar.add(size10Button);
		new MouseMoution(size10Button);

        //Размер 20
        JButton size20Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size20.png")));
        size20Button.setBounds(0, 40, 40, 40);
        size20Button.setBorderPainted(false);
        size20Button.setBackground(Color.lightGray);
        size20Button.setOpaque(false);
        size20Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 20;
				if (flag2) {
					size = 4;
				}
            }
        });
		toolbar.add(size20Button);
		new MouseMoution(size20Button);
		
        //Размер 40
        JButton size40Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size40.png")));
        size40Button.setBounds(0, 80, 40, 40);
        size40Button.setBorderPainted(false);
        size40Button.setBackground(Color.lightGray);
        size40Button.setOpaque(false);
        size40Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 40;
				if (flag2) {
					size = 6;
				}
            }
        });
		toolbar.add(size40Button);
		new MouseMoution(size40Button);
		
        //Размер 80
        JButton size80Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size80.png")));
        size80Button.setBounds(0, 120, 40, 40);
        size80Button.setBorderPainted(false);
        size80Button.setBackground(Color.lightGray);
        size80Button.setOpaque(false);
        size80Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 80;
				if (flag2) {
					size = 8;
				}
            }
        });
		toolbar.add(size80Button);
		new MouseMoution(size80Button);
		
        //Ластик
        JButton EraseButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Erasor.png")));
        EraseButton.setBounds(45, 40, 40, 40);
        EraseButton.setBorderPainted(false);
        EraseButton.setBackground(Color.lightGray);
        EraseButton.setOpaque(false);
        EraseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				flag2 = false;
				flag3 = false;
				flag1 = true;
                mainColor = Color.white;
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = (new ImageIcon(this.getClass().getClassLoader().getResource("Erasor.png"))).getImage();
				Point hotSpot = new Point(10, 30);
				Cursor cursor1 = toolkit.createCustomCursor(image, hotSpot, "Erasor");
				boardPanel.setCursor(cursor1);
				if (size < 10) {
						if (size == 2)
							size = 10;
						if (size == 4)
							size = 20;
						if (size == 6)
							size = 40;
						if (size == 8)
							size = 80;
				}
            }
        });
		toolbar.add(EraseButton);
		new MouseMoution(EraseButton);
		
        //Цвет чёрный
        JButton blackButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("black.png")));
        blackButton.setBounds(0, 200, 40, 40);
        blackButton.setBorderPainted(false);
        blackButton.setBackground(Color.lightGray);
        blackButton.setOpaque(false);
        blackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.black;
				}
            }
        });
		toolbar.add(blackButton);
		new MouseMoution(blackButton);

		
        //Цвет красный
        JButton redButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("red.png")));
        redButton.setBounds(0, 240, 40, 40);
        redButton.setBorderPainted(false);
        redButton.setBackground(Color.lightGray);
        redButton.setOpaque(false);
        redButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.red;
				}
            }
        });
		toolbar.add(redButton);
		new MouseMoution(redButton);
		
        //Цвет оранжевый
        JButton orangeButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("orange.png")));
        orangeButton.setBounds(0, 280, 40, 40);
        orangeButton.setBorderPainted(false);
        orangeButton.setBackground(Color.lightGray);
        orangeButton.setOpaque(false);
        orangeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.orange;
				}
            }
        });
		toolbar.add(orangeButton);
		new MouseMoution(orangeButton);
		
        //Цвет жёлтый
        JButton yellowButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("yellow.png")));
        yellowButton.setBounds(0, 320, 40, 40);
        yellowButton.setBorderPainted(false);
        yellowButton.setBackground(Color.lightGray);
        yellowButton.setOpaque(false);
        yellowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.yellow;
				}
            }
        });
		toolbar.add(yellowButton);
		new MouseMoution(yellowButton);
		
        //Цвет зелёный
        JButton greenButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("green.png")));
        greenButton.setBounds(45, 200, 40, 40);
        greenButton.setBorderPainted(false);
        greenButton.setBackground(Color.lightGray);
        greenButton.setOpaque(false);
        greenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.green;
				}
            }
        });
		toolbar.add(greenButton);
		new MouseMoution(greenButton);
		
        //Цвет голубой
        JButton cyanButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("cyan.png")));
        cyanButton.setBounds(45, 240, 40, 40);
        cyanButton.setBorderPainted(false);
        cyanButton.setBackground(Color.lightGray);
        cyanButton.setOpaque(false);
        cyanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.cyan;
				}
            }
        });
		toolbar.add(cyanButton);
		new MouseMoution(cyanButton);

        //Цвет синий
        JButton blueButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("blue.png")));
        blueButton.setBounds(45, 280, 40, 40);
        blueButton.setBorderPainted(false);
        blueButton.setBackground(Color.lightGray);
        blueButton.setOpaque(false);
        blueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.blue;
				}
            }
        });
		toolbar.add(blueButton);
		new MouseMoution(blueButton);

        //Цвет фиолетовый
        JButton magentaButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("magenta.png")));
        magentaButton.setBounds(45, 320, 40, 40);
        magentaButton.setBorderPainted(false);
        magentaButton.setBackground(Color.lightGray);
        magentaButton.setOpaque(false);
        magentaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.magenta;
				}
            }
        });
		toolbar.add(magentaButton);
		new MouseMoution(magentaButton);
		
		//Цвет розовый
		JButton pink = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("pink.png")));
        pink.setBounds(45, 360, 40, 40);
        pink.setBorderPainted(false);
        pink.setBackground(Color.lightGray);
        pink.setOpaque(false);
        pink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.pink;
				}
            }
        });
		toolbar.add(pink);
		new MouseMoution(pink);
		
		//Цвет серый
		JButton gray = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("gray.png")));
        gray.setBounds(0, 360, 40, 40);
        gray.setBorderPainted(false);
        gray.setBackground(Color.lightGray);
        gray.setOpaque(false);
        gray.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = new Color(195, 195, 195);
				}
            }
        });
		toolbar.add(gray);
		new MouseMoution(gray);
		
		//Цвет коричневый
		JButton brown = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("brown.png")));
        brown.setBounds(0, 400, 40, 40);
        brown.setBorderPainted(false);
        brown.setBackground(Color.lightGray);
        brown.setOpaque(false);
        brown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = new Color(185, 122, 87);
				}
            }
        });
		toolbar.add(brown);
		new MouseMoution(brown);
		
		//Цвет сиреневый
		JButton lilac = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("lilac.png")));
        lilac.setBounds(45, 400, 40, 40);
        lilac.setBorderPainted(false);
        lilac.setBackground(Color.lightGray);
        lilac.setOpaque(false);
        lilac.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = new Color(200, 191, 231);
				}
            }
        });
		toolbar.add(lilac);
		new MouseMoution(lilac);
		
		//Визуализация кисти/карандаша
		//Карандаш
		JButton PencilButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Pencil.png")));
        PencilButton.setBounds(45, 80, 40, 40);
		PencilButton.setBorderPainted(false);
		PencilButton.setBackground(Color.lightGray);
		PencilButton.setOpaque(false);
        PencilButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if (!flag2) {
					//mainColor = Color.magenta;
					flag3 = false;
					flag2 = true;
					if (flag1) {
						flag1 = false;
						mainColor = null;
					}
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Image image = (new ImageIcon(this.getClass().getClassLoader().getResource("Pencil.png"))).getImage();
					Point hotSpot = new Point(0, 30);
					Cursor cursor2 = toolkit.createCustomCursor(image, hotSpot, "Pencil");
					boardPanel.setCursor(cursor2);
					if (size == 10)
						size = 2;
					if (size == 20)
						size = 4;
					if (size == 40)
						size = 6;
					if (size == 80)
						size = 8;
				}
			}
		 });
		toolbar.add(PencilButton);
		new MouseMoution(PencilButton);

		//Кисть
		JButton BrushButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Brush.png")));
        BrushButton.setBounds(45, 120, 40, 40);
		BrushButton.setBorderPainted(false);
		BrushButton.setBackground(Color.lightGray);
		BrushButton.setOpaque(false);
        BrushButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if (!flag3) {
					flag2 = false;
					flag3 = true;
					if (flag1) {
						flag1 = false;
						mainColor = null;
					}
					//mainColor = Color.magenta;
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Image image = (new ImageIcon(this.getClass().getClassLoader().getResource("Brush.png"))).getImage();
					Point hotSpot = new Point(30, 0);
					Cursor cursor3 = toolkit.createCustomCursor(image, hotSpot, "Brush");
					boardPanel.setCursor(cursor3);
					if (size < 10) {
						if (size == 2)
							size = 10;
						if (size == 4)
							size = 20;
						if (size == 6)
							size = 40;
						if (size == 8)
							size = 80;
					}
				}
			}
		 });
		toolbar.add(BrushButton);
		new MouseMoution(BrushButton);
		
		//Меню
        menuButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("menu.png")));
        menuButton.setBounds(0, 0, 40, 40); // размещение
        menuButton.setBorderPainted(false); // не рисовать рамку
        menuButton.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        menuButton.setOpaque(false); // прозрачность
        menuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isConnected) {
                    if (frame.isAncestorOf(menu)) {
                        frame.remove(menu);
						boardsFrame.setVisible(false);
                        frame.add(boardPanel);
						frame.add(toolbar);
                        frame.repaint();
						//Удаление предупреждений
						if (menu.isAncestorOf(existLabel)) {
							menu.remove(existLabel);
							frame.repaint();
						}
						if (menu.isAncestorOf(notFoundLabel)) {
							menu.remove(notFoundLabel);
							frame.repaint();
						}
						if (menu.isAncestorOf(alreadyConnected)) {
							menu.remove(alreadyConnected);
							frame.repaint();
						}
                    } else {
                        frame.remove(boardPanel);
						frame.remove(toolbar);
                        frame.add(menu);
                        frame.repaint();
                    }
                }
            }
        });
		frame.add(menuButton);
		
        //Создать доску (UTF-16)
        JButton createBoard = new JButton("\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0434\u043e\u0441\u043a\u0443");
        createBoard.setBounds(125, 40, 210, 40); // размещение
        createBoard.setBorderPainted(true); //рисовать рамку
        createBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        createBoard.setOpaque(true); // не прозрачность
        createBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                //Если нет имени доски
                if (nameBoard.equals("")) {
                    frame.repaint();
                    return;
                }
                //Удаление предупреждений
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }
				if (menu.isAncestorOf(alreadyConnected)) {
                    menu.remove(alreadyConnected);
                    frame.repaint();
                }
                try {
                    try {
                        writeSocket.write("CREATE " + nameBoard + "\n");
                        writeSocket.flush();
						frame.add(menuButton);
						frame.repaint();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
        menu.add(createBoard);

		//Кнопка Открыть/скрыть чат
        JButton chatButton = new JButton("\u0427\u0430\u0442");//"Чат"
        chatButton.setBounds(335, 80, 210, 40); // размещение
        chatButton.setBorderPainted(true); // рисовать рамку
        chatButton.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        chatButton.setOpaque(true); // не прозрачность
        chatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if(frameChat.isVisible())//если чат виден
					frameChat.setVisible(false);
				else
					frameChat.setVisible(true);
            }
        });
        menu.add(chatButton);
		
		//Получить список доступных для подключения досок
		JButton getBoardlist = new JButton("Список досок");
		getBoardlist.setBounds(545, 80, 210, 40); // размещение
        getBoardlist.setBorderPainted(true); // рисовать рамку
        getBoardlist.setBackground(Color.red); // цвет фона (убирает градиент при наведении)
        getBoardlist.setOpaque(true); // не прозрачность
        getBoardlist.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if(boardsFrame == null) {
					boardsFrame = new JFrame();
					boardsFrame.setVisible(false);
				}
				if(boardsFrame.isVisible()) {
					boardsFrame.setVisible(false);
				} else {
					try {
						try {
							writeSocket.write("GIVE BOARDS\n");
							writeSocket.flush();
							//граф. интерфейс выбора досок
							boardsFrame = new JFrame("Список досок");
							boardsFrame.setLayout(new FlowLayout());
							boardsFrame.setSize(300, 400);
							boardsFrame.setResizable(false);
							boardsFrame.setLocationRelativeTo(null);
							boardsFrame.setIconImage((new ImageIcon(this.getClass().getClassLoader().getResource("boardIcon.png"))).getImage());
							boardsFrame.setVisible(true);
							
							//
							JPanel boards = new JPanel();
							boards.setBounds(10, 10, 290, 390);
							
							JScrollPane scrollBoards = new JScrollPane(boards, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
							scrollBoards.setSize(290, 390);
							scrollBoards.setLocation(10, 10);      
							boardsFrame.add(scrollBoards);
		
							for (int i = 0; i < nameOfBoards.size(); i++) {
								if (!nameOfBoards.get(i).equals("")) {
									JButton button = new JButton(nameOfBoards.get(i));
									// button.setBounds(200, 40 * i, 200, 40);
									int finalI = i;
									button.addActionListener(new ActionListener() {
										public void actionPerformed(ActionEvent event) {
											flag4 = true;
											//Удаление предупреждений
											if (menu.isAncestorOf(existLabel)) {
												menu.remove(existLabel);
												frame.repaint();
											}
											if (menu.isAncestorOf(notFoundLabel)) {
												menu.remove(notFoundLabel);
												frame.repaint();
											}
											if (menu.isAncestorOf(alreadyConnected)) {
												menu.remove(alreadyConnected);
												frame.repaint();
											}
											try {
												try {
													writeSocket.write("CONNECT " + nameOfBoards.get(finalI) + "\n");
													writeSocket.flush();
													connectionLabel1 = new JLabel("\u041f\u0440\u0438\u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u0435\u0020\u043a\u0020\u0434\u043e\u0441\u043a\u0435\u0020\"" + nameOfBoards.get(finalI) + "\"...");
													connectionLabel1.setBounds(20, 85, 300, 30);
													frame.add(menuButton);
													frame.repaint();
												} catch (IOException exception) {
													System.out.println(exception.toString());
													readSocket.close();
													writeSocket.close();
												}
											} catch (IOException exception) {
												System.out.println(exception.toString());
											}
										}
									});
									boards.add(button);
								}
							}
						} catch (IOException exception) {
							System.out.println(exception.toString());
							readSocket.close();
							writeSocket.close();
						}
					} catch (IOException exception) {
						System.out.println(exception.toString());
					}
				}
			}
		});
        menu.add(getBoardlist);
		
		//Подключиться к доске
		//Cтрока "Присоединиться к доске" в UTF-16 для нормального вывода
		String string = new String("\u041f\u0440\u0438\u0441\u043e\u0435\u0434\u0438\u043d\u0438\u0442\u044c\u0441\u044f \u043a \u0434\u043e\u0441\u043a\u0435");
        JButton joinBoard = new JButton(string);
        joinBoard.setBounds(335, 40, 210, 40); // размещение
        joinBoard.setBorderPainted(true); // рисовать рамку
        joinBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        joinBoard.setOpaque(true); // не прозрачность
        joinBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                //Если нет имени доски
                if (nameBoard.equals("")) {
                    System.out.println();
                    frame.repaint();
                    return;
                }

                //Удаление предупреждений
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }
				if (menu.isAncestorOf(alreadyConnected)) {
                    menu.remove(alreadyConnected);
                    frame.repaint();
                }
				
                //Подключение
                try {
                    try {
                        writeSocket.write("CONNECT " + nameBoard + "\n");
                        writeSocket.flush();
						frame.add(menuButton);
						frame.repaint();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
        menu.add(joinBoard);
		
		//Сохранение доски
		JButton save = new JButton("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0434\u043e\u0441\u043a\u0443");
		save.addActionListener(e -> save(boardPanel));
		save.setBounds(545, 40, 210, 40); // размещение
        save.setBorderPainted(true); // рисовать рамку
        save.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        save.setOpaque(true); // не_прозрачность
		menu.add(save);
		
        //Рисование мышью
        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                try {
                    try {
						if(mainColor != null) {
							String message = mainColor.getRGB() + " " + (e.getX()- size / 2) + " " + (e.getY()- size / 2)
									+ " " + size;
							writeSocket.write(message + "\n");
							writeSocket.flush();
						}
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }

            }
        });

        boardPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    try {
						if(mainColor != null) {		
							String message = mainColor.getRGB() + " " + (e.getX() - size / 2) + " " + (e.getY() - size / 2)
									+ " " + size;
							writeSocket.write(message + "\n");
							writeSocket.flush();
						}
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
    }
	//сохранение доски
	private void save(JPanel panel) {
		BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		panel.print(img.getGraphics());
		UIManager.put("FileChooser.saveButtonText", "Сохранить");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.fileNameLabelText", "Наименование файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Типы файлов");
        UIManager.put("FileChooser.lookInLabelText", "Директория");
        UIManager.put("FileChooser.saveInLabelText", "Сохранить в директории");
        UIManager.put("FileChooser.folderNameLabelText", "Путь директории");
		UIManager.put("FileChooser.openButtonText", "Открыть");
		
		JFileChooser chooser = new JFileChooser();
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter("*.jpeg","*.*");
		chooser.setFileFilter(filter);
		
		int result = chooser.showDialog(frame, "Сохранить");
		if(result == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			String fname = file.getAbsolutePath();
			
			if(!fname.endsWith(".jpeg"))
				file = new File(fname + ".jpeg");
			
			try {
				ImageIO.write(img, "jpeg", file);
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
	
}