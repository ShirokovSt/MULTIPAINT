package org.suai.paint.client;

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
    boolean isConnected = false;
    String serverHost = null;
    int serverPort;
    Socket clientSocket;
    BufferedReader readSocket;
    BufferedWriter writeSocket;

    //Переменные графики
	boolean flag1 = false;
	boolean flag2 = false;
	boolean flag3 = false;
    JFrame frame;
    JToolBar toolbar; // кнопки
    JPanel menu; // меню
    JLabel existLabel; // доска существует
    JLabel notFoundLabel; // доска не неайдена
    BoardPanel boardPanel; // отображение доски
    BufferedImage board = null; // доска
    Graphics2D graphics;
    Color mainColor;
    int size = 10; // размер кисти
	String name;
	
	//Переменные чата (граф. интерфейс)
	JFrame frameChat;
	JTextField messageTextFieldForChat;
	JTextArea contentPanelChat = null;
	JButton sendChat;
	JScrollPane scroll;
	
    class BoardPanel extends JPanel implements Serializable {
        private static final long serialVersionUID = -109728024865681281L;

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
	
	
	//Класс считывание данных от сервера
    class NetDraw extends Thread {
        String message;
        String[] splitMessage;

        public NetDraw() {
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
								this.sleep(3000);
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
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("EXISTS")) {
                                menu.add(existLabel);
                                frame.repaint();
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
							//Подключение к доске
                            if (splitMessage[1].equals("OK")) {
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
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("NOT FOUND")) {
                                menu.add(notFoundLabel);
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
        
		//Сеть
        try {
            try {
                this.serverHost = serverHost;
                this.serverPort = serverPort;
                clientSocket = new Socket(serverHost, serverPort);
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Get name, example: @name Vanya\nYour name: ");
				
				String nameClient = stdIn.readLine();				
				writeSocket.write(nameClient + "\n");
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
        frameChat.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
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
		
        //Графика (окно рисования)
        frame = new JFrame("MultiPaint");
        frame.setSize(800, 700); // размер окна
        frame.setResizable(false); // нельзя менять размер окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
		frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setVisible(true);

        //Панель рисования
        boardPanel = new BoardPanel();
        boardPanel.setBounds(40, 0, 800, 700);
        boardPanel.setOpaque(true);
       // mainColor = Color.white; // Color нынешний цвет

        //Панель меню
        menu = new JPanel();
        menu.setBounds(40, 0, 800, 700);
        menu.setBackground(mainColor);
        menu.setLayout(null);
        frame.add(menu);
		
        //Доска с таким именем уже существует UTF-16
        existLabel = new JLabel("\u0414\u043e\u0441\u043a\u0430 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442");
        existLabel.setBounds(20, 85, 300, 30);

        //Доска с таким именем не существует UTF-16
        notFoundLabel = new JLabel("\u0414\u043e\u0441\u043a\u0430 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442");
        notFoundLabel.setBounds(20, 85, 300, 30);

        //Приглашение
        //Введите имя доски UTF-16
        JLabel inviteLabel = new JLabel( name + ", \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u0434\u043e\u0441\u043a\u0438");
        inviteLabel.setBounds(20, 5, 200, 30);
        menu.add(inviteLabel);
		
        //Текстовое поле
        JTextField textField = new JTextField();
        textField.setBounds(20, 45, 100, 30);
        textField.setText("\u0414\u043e\u0441\u043a\u0430 \u0031"); //доска 1
        menu.add(textField);
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

                try {
                    try {
                        writeSocket.write("CREATE " + nameBoard + "\n");
                        writeSocket.flush();
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

                //Подключение
                try {
                    try {
                        writeSocket.write("CONNECT " + nameBoard + "\n");
                        writeSocket.flush();
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
		save.setBounds(535, 40, 210, 40); // размещение
        save.setBorderPainted(true); // рисовать рамку
        save.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        save.setOpaque(true); // не_прозрачность
		menu.add(save);
		
		
        //Панель с инструментами
        JToolBar toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        toolbar.setBounds(0, 0, 40, 700); // размещение
        toolbar.setLayout(null); // элементы размещаем сами
        toolbar.setFloatable(false); // нельзя перетаскивать
        toolbar.setBorderPainted(false); // без рамок
        toolbar.setBackground(mainColor); // устанавливаем цвет панели
		frame.add(toolbar);
		
        //Меню
        JButton menuButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("menu.png")));
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
                        frame.repaint();
                    } else {
                        frame.remove(boardPanel);
                        frame.add(menu);
                        frame.repaint();
                    }
                }
            }
        });
		toolbar.add(menuButton);
		
        //Размер 10
        JButton size4Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size10.png")));
        size4Button.setBounds(0, 40, 40, 40);
        size4Button.setBorderPainted(false);
        size4Button.setBackground(Color.lightGray);
        size4Button.setOpaque(false);
        size4Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 10;
				if (flag2) {
					size = 2;
				}
            }
        });	
		toolbar.add(size4Button);

        //Размер 20
        JButton size10Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size20.png")));
        size10Button.setBounds(0, 80, 40, 40);
        size10Button.setBorderPainted(false);
        size10Button.setBackground(Color.lightGray);
        size10Button.setOpaque(false);
        size10Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 20;
				if (flag2) {
					size = 4;
				}
            }
        });
		toolbar.add(size10Button);
		
        //Размер 40
        JButton size20Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size40.png")));
        size20Button.setBounds(0, 120, 40, 40);
        size20Button.setBorderPainted(false);
        size20Button.setBackground(Color.lightGray);
        size20Button.setOpaque(false);
        size20Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 40;
				if (flag2) {
					size = 6;
				}
            }
        });
		toolbar.add(size20Button);
		
        //Размер 80
        JButton size30Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size80.png")));
        size30Button.setBounds(0, 160, 40, 40);
        size30Button.setBorderPainted(false);
        size30Button.setBackground(Color.lightGray);
        size30Button.setOpaque(false);
        size30Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 80;
				if (flag2) {
					size = 8;
				}
            }
        });
		toolbar.add(size30Button);
		
        //Ластик
        JButton whiteButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Erasor.png")));
        whiteButton.setBounds(0, 200, 40, 40);
        whiteButton.setBorderPainted(false);
        whiteButton.setBackground(Color.lightGray);
        whiteButton.setOpaque(false);
        whiteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
				flag2 = false;
				flag3 = false;
				flag1 = true;
                mainColor = Color.white;
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = (new ImageIcon(this.getClass().getClassLoader().getResource("Erasor.png"))).getImage();
				Point hotSpot = new Point(10,30);
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
		toolbar.add(whiteButton);
		
        //Цвет чёрный
        JButton blackButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("black.png")));
        blackButton.setBounds(0, 240, 40, 40);
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
		
        //Цвет красный
        JButton redButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("red.png")));
        redButton.setBounds(0, 280, 40, 40);
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
		
        //Цвет оранжевый
        JButton orangeButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("orange.png")));
        orangeButton.setBounds(0, 320, 40, 40);
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
		
        //Цвет жёлтый
        JButton yellowButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("yellow.png")));
        yellowButton.setBounds(0, 360, 40, 40);
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
		
        //Цвет зелёный
        JButton greenButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("green.png")));
        greenButton.setBounds(0, 400, 40, 40);
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
		
        //Цвет голубой
        JButton cyanButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("cyan.png")));
        cyanButton.setBounds(0, 440, 40, 40);
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

        //Цвет синий
        JButton blueButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("blue.png")));
        blueButton.setBounds(0, 480, 40, 40);
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

        //Цвет фиолетовый
        JButton magentaButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("magenta.png")));
        magentaButton.setBounds(0, 520, 40, 40);
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

		//Визуализация кисти/карандаша
		
		//Карандаш
		JButton PencilButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Pencil.png")));
        PencilButton.setBounds(0, 560, 40, 40);
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
					Point hotSpot = new Point(0,30);
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

		//Кисть
		JButton BrushButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Brush.png")));
        BrushButton.setBounds(0, 600, 40, 40);
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
					Point hotSpot = new Point(30,0);
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

		
        //Слушатели
        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
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