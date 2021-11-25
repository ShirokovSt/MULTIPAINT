//package org.suai.paint.client;

import java.net.*;
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
    public JFrame frame;
    public JToolBar toolbar; // кнопки
	public JButton menuButton;
    public JPanel menu; // меню
    public JLabel existLabel; // доска существует
    public JLabel notFoundLabel; // доска не неайдена
	public JLabel connectionLabel; // когда идёт присоединение к доске
	public JLabel alreadyConnected; //если пытаешься подключиться к доске на которой ты находишься
    public BoardPanel boardPanel; // отображение доски
    public BufferedImage board = null; // доска
    public Graphics2D graphics;
    public Color mainColor;
    public int size = 10; // размер кисти
	public String name = null; //имя пользователя
	private JTextField textField = null;
	
	//Переменные чата (граф. интерфейс)
	public JFrame frameChat;
	public JTextField messageTextFieldForChat;
	public JTextArea contentPanelChat = null;
	public JButton sendChat;
	public JScrollPane scroll;
	
    class BoardPanel extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(board, 0, 0, this);
        }
    }
	
	private static class Sender extends Thread { //класс отправитель
		BufferedWriter out;
		BufferedReader stdIn;
		String fromClient;
		String nameClient;
		String message;
		//Sost sost;
		public Sender(BufferedWriter out, BufferedReader stdIn, String nameClient) {
				this.out = out;
				this.stdIn = stdIn;
				this.nameClient = nameClient;
				this.start();
		}
		@Override
		public void run () {
			while (true) {
				try {
					fromClient = stdIn.readLine();
				} catch(Exception e) {
					e.printStackTrace();
				}
				if(fromClient != null) {
					if(fromClient.startsWith("@quit")) {
						try {
							out.write("MESSAGE " + fromClient + "\n");
							out.flush();
						} catch(Exception e) {
							e.printStackTrace();
						}
						break;
					}
					if(fromClient.contains("@senduser ")) {
						message = "MESSAGE " + fromClient + "\n";
						try {
							out.write(message);
							out.flush();
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
					else
					{
						message = "MESSAGE " + fromClient + "\n";
						try {						
							out.write(message);
							out.flush();
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
					
			}
		}
	}
	
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
			button.addMouseMotionListener(new MouseMotionAdapter() { //00
				public void mouseDragged(MouseEvent e) {			//11
					if(!e.isMetaDown()){								
						Point p = button.getLocation(); //x < 0, y < 0; x > 100 (60), y > 600 (660) (00, 01, 10, 11)
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
	
	//Класс считывание данных от сервера
    class NetDraw extends Thread {
        String message;
        String[] splitMessage;

        public NetDraw() {
			// JToolBar toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
            this.start();
        }

        public void run() {
            try {
                try {
                    while (true) {
                        message = readSocket.readLine();
						if(message == null)
							continue;
                        splitMessage = message.split(" ", 2);
						if(splitMessage[0].equals("MESSAGE")) {//для чата
							if(splitMessage[1].equals("@quit")) {
								contentPanelChat.append("@\u0053\u0045\u0052\u0056\u0045\u0052\u003a \u0412\u044b \u043f\u043e\u043a\u0438\u043d\u0443\u043b\u0438 \u0441\u0435\u0440\u0432\u0435\u0440\u0021\n");
								frameChat.repaint();
								this.sleep(1000);
								System.out.println("@SERVER: you have left the server");
								System.exit(0);
							}
							String chatMessage = splitMessage[1];
							contentPanelChat.append(chatMessage + "\n");
							frameChat.repaint();
							System.out.println(chatMessage);
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
					
								menu.add(connectionLabel);
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
								menu.remove(connectionLabel);
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
        try {
            try {
                this.serverHost = serverHost;
                this.serverPort = serverPort;
                clientSocket = new Socket(serverHost, serverPort);
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Get name, example: @name Vanya\nYour name: ");
				
				name = stdIn.readLine();				
				writeSocket.write(name + "\n");
				writeSocket.flush();
				String fromServer = readSocket.readLine();
				name = fromServer;
				System.out.println("@SERVER: Your name - " + name + "\n");
                new NetDraw();
				new Sender(writeSocket, stdIn, name);
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
		frameChat = new JFrame("Chat");
		frameChat.setSize(400, 600);
		frameChat.setResizable(false); // нельзя менять размер окна
        frameChat.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); //скрыть кадр, но сохранить приложение запущено.
		frameChat.setLocationRelativeTo(null);
        frameChat.setLayout(null);
        frameChat.setVisible(true);
		
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
		sendChat = new JButton("Send");
		sendChat.setBounds(290, 500, 80, 50); // размещение
		sendChat.setBorderPainted(true); // рисовать рамку
		sendChat.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
		sendChat.setOpaque(true); // не прозрачность
		sendChat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
		
		frameChat.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"clickButton");

		frameChat.getRootPane().getActionMap().put("clickButton",new AbstractAction(){
				public void actionPerformed(ActionEvent ae)
				{
					sendChat.doClick();
				}
		});
		frameChat.setVisible(false);//изначально чат не виден
		
        //Графика (окно рисования)
        frame = new JFrame("MultiPaint");
        frame.setSize(900, 700); // размер окна
        frame.setResizable(false); // нельзя менять размер окна 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
		frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setVisible(true);

        //Панель рисования
        boardPanel = new BoardPanel();
        boardPanel.setBounds(100, 0, 800, 700);
        boardPanel.setOpaque(true);
       // mainColor = Color.white; // Color нынешний цвет

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
        JLabel inviteLabel = new JLabel( name + ", \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u0434\u043e\u0441\u043a\u0438");
        inviteLabel.setBounds(20, 5, 200, 30);
        menu.add(inviteLabel);
		
		//Панель с инструментами
		toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        toolbar.setBounds(0, 40, 100, 660); // размещение
        toolbar.setLayout(null); // элементы размещаем сами
        toolbar.setFloatable(false); // нельзя перетаскивать
        toolbar.setBorderPainted(false); // без рамок
        toolbar.setBackground(mainColor); // устанавливаем цвет панели
		
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
        EraseButton.setBounds(0, 160, 40, 40);
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
                //toolbar.setBackground(mainColor);
                //menu.setBackground(mainColor);
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
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
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
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
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
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
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
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
				}
            }
        });
		toolbar.add(yellowButton);
		new MouseMoution(yellowButton);
		
        //Цвет зелёный
        JButton greenButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("green.png")));
        greenButton.setBounds(0, 360, 40, 40);
        greenButton.setBorderPainted(false);
        greenButton.setBackground(Color.lightGray);
        greenButton.setOpaque(false);
        greenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.green;
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
				}
            }
        });
		toolbar.add(greenButton);
		new MouseMoution(greenButton);
		
        //Цвет голубой
        JButton cyanButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("cyan.png")));
        cyanButton.setBounds(0, 400, 40, 40);
        cyanButton.setBorderPainted(false);
        cyanButton.setBackground(Color.lightGray);
        cyanButton.setOpaque(false);
        cyanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.cyan;
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
				}
            }
        });
		toolbar.add(cyanButton);
		new MouseMoution(cyanButton);

        //Цвет синий
        JButton blueButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("blue.png")));
        blueButton.setBounds(0, 440, 40, 40);
        blueButton.setBorderPainted(false);
        blueButton.setBackground(Color.lightGray);
        blueButton.setOpaque(false);
        blueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.blue;
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
				}
            }
        });
		toolbar.add(blueButton);
		new MouseMoution(blueButton);

        //Цвет фиолетовый
        JButton magentaButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("magenta.png")));
        magentaButton.setBounds(0, 480, 40, 40);
        magentaButton.setBorderPainted(false);
        magentaButton.setBackground(Color.lightGray);
        magentaButton.setOpaque(false);
        magentaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				if ((!flag1) && ((flag2) || (flag3))) { 
					mainColor = Color.magenta;
					//toolbar.setBackground(mainColor);
					//menu.setBackground(mainColor);
				}
            }
        });
		toolbar.add(magentaButton);
		new MouseMoution(magentaButton);

		//Визуализация кисти/карандаша
		
		//Карандаш
		JButton PencilButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Pencil.png")));
        PencilButton.setBounds(0, 520, 40, 40);
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
        BrushButton.setBounds(0, 560, 40, 40);
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
                        frame.add(boardPanel);
						frame.add(toolbar);
                        frame.repaint();
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
                //Нет имени доски
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

		//Открыть/скрыть чат
		//String string = new String("\u0427\u0430\u0442");//"Чат"
        JButton chatButton = new JButton("\u0427\u0430\u0442");
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
                //Нет имени доски
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
		
		//сохранение доски
		JButton save = new JButton("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0434\u043e\u0441\u043a\u0443");
		save.addActionListener(e -> save(boardPanel));
		save.setBounds(545, 40, 210, 40); // размещение
        save.setBorderPainted(true); // рисовать рамку
        save.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        save.setOpaque(true); // не_прозрачность
		menu.add(save);
		
        //Слушатели
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
		
	private void save(JPanel panel) {
		BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		panel.print(img.getGraphics());
		
		JFileChooser chooser = new JFileChooser();
		JLabel imageLabel = new JLabel();
		imageLabel.setIcon(new ImageIcon(img));
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter("*.jpeg","*.*");
		chooser.setFileFilter(filter);
		
		int result = chooser.showSaveDialog(imageLabel);
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