package org.suai.paint.server;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.awt.image.*;
import java.text.*;

public class Server {
    private ServerSocket serverSocket = null;
    private HashMap<String, BufferedImage> boards = null;
    private ArrayList<ClientThread> clients = null;
    private Object consoleSynch = null;
	private HashMap<String, ClientThread> usersForChat = null;//для чата
	private int autoName = 0;//автоимена
	private Logger logger = null;
	
	public class Logger {
		private FileWriter fileWriter = null;
		private SimpleDateFormat formatForDateNow = null; //формат даты для логов
		private Date date = null;
		public Logger() {
			formatForDateNow = new SimpleDateFormat("yyyy.MM.dd ' time ' hh:mm:ss a zzz");
		}
		public synchronized boolean toLog(String mess) {
			try {
				fileWriter = new FileWriter("log.txt", true);
				date = new Date();
				fileWriter.write(formatForDateNow.format(date) + ": " + mess + "\n");
				fileWriter.close();
				return true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return false;
			}
		}
	}
	
	public class ClientThread extends Thread {
        //Переменные сети
        private Socket clientSocket = null;
        private BufferedReader readSocket = null;
        private BufferedWriter writeSocket = null;
        private String boardName = null;
		private String name;
        //Переменные графики
        private Color mainColor = null;
        private Graphics2D graphics = null;

        public ClientThread(Socket clientSocket, String name) {
			this.name = name;
            this.clientSocket = clientSocket;
            try {
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				writeSocket.flush();
            } catch (IOException err) {
                synchronized (consoleSynch) {
					//log
					logger.toLog("ERROR:" + err.getMessage());
                    System.out.println(err.getMessage());
                }
            }
        }

        public void run() {
            synchronized (consoleSynch) {
				//log
				logger.toLog("Client (name: " + name + ") connected");
                System.out.println("Client " + name + " connected\n");
                synchronized (usersForChat) {
					//log
					logger.toLog("Number of clients: " + usersForChat.size());
                    System.out.println("Number of clients: " + usersForChat.size() + "\n");
                }
            }
			for (String to : usersForChat.keySet()) {
				synchronized (usersForChat.get(to)) {
					ClientThread toUser = usersForChat.get(to);
					if(to.equals(name))
						continue;
					 try {
						toUser.writeSocket.write("MESSAGE @SERVER: The user " + name + " has been added to the chat\n");
						toUser.writeSocket.flush();
					 } catch (Exception err) {
						synchronized (consoleSynch) {
							//log
							logger.toLog("ERROR:" + err.toString());
							System.out.println(err.toString() + "\n");
						}
					}
				}
			}
            try {
                try {
                    while (true) {
                        String message = readSocket.readLine();
						if(message == null)
							continue;
                        String[] splitMessage = message.split(" ", 2);
						
						if(splitMessage[0].equals("MESSAGE")) { //MESSAGE mess //MESSAGE @senduser Ivan mess
							synchronized (usersForChat) {
								if(splitMessage[1].startsWith("@quit")) {
									writeSocket.write("MESSAGE @quit");
									writeSocket.flush();
									break;
								}
								if(splitMessage[1].contains("@senduser ")) {
									String[] split = splitMessage[1].split(" ", 3);
									ClientThread toUser = usersForChat.get(split[1]);
									if(toUser != null) {
										toUser.writeSocket.write("MESSAGE @" + name + " (private): " + split[2] + "\n");
										toUser.writeSocket.flush();
									}
									else {
										writeSocket.write("MESSAGE @SERVER: This user does not exist\n");
										writeSocket.flush();
									}
								}
								else {
									for (String to : usersForChat.keySet()) {
										synchronized (usersForChat.get(to)) {
											ClientThread toUser = usersForChat.get(to);
											if(to.equals(name))
												continue;
											toUser.writeSocket.write("MESSAGE @" + name + ": " + splitMessage[1] + "\n");
											toUser.writeSocket.flush();
										}
									}
								}
                            }
						}
                        if (splitMessage[0].equals("CREATE")) {
							
                            //Создание доски
                            boolean isContains;
                            synchronized (boards) {
                                isContains = boards.containsKey(splitMessage[1]);
                            }
                            if (isContains) {
                                synchronized (this) {
                                    writeSocket.write("CREATE EXISTS\n");
                                    writeSocket.flush();
                                }
                            } else {
                                synchronized (this) {
                                    writeSocket.write("CREATE OK\n");
                                    writeSocket.flush();
                                }
                                String boardNameOld = boardName;

                                boardName = splitMessage[1];
                                synchronized (boards) {
                                    boards.put(boardName, new BufferedImage(800, 700, BufferedImage.TYPE_INT_RGB));
                                    graphics = boards.get(boardName).createGraphics();
                                }
                                synchronized (boards.get(boardName)) {
                                    graphics.setColor(Color.white);
                                    graphics.fillRect(0, 0, 800, 700);
                                }
                                synchronized (consoleSynch) {
									//log
									logger.toLog("Desk \"" + boardName + "\" created");
                                    System.out.println("Desk \"" + boardName + "\" created");
                                    synchronized (boards) {
										//log
										logger.toLog("Number of desks: " + boards.size());
                                        System.out.println("Number of desks: " + boards.size() + "\n");
                                    }
                                }

                                checkBoards(boardNameOld);
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
                            
                            //Подключение к доске
                            boolean isContains;
                            synchronized (boards) {
                                isContains = boards.containsKey(splitMessage[1]);
                            }
                            if (isContains) {
                                synchronized (this) {
                                    writeSocket.write("CONNECT OK\n");
                                    writeSocket.flush();
                                }
                                String boardNameOld = boardName;

                                boardName = splitMessage[1];
                                synchronized (boards.get(boardName)) {
                                    graphics = boards.get(boardName).createGraphics();
                                }
                                int[] rgbArray = new int[4800000];
                                synchronized (boards.get(boardName)) {
                                    boards.get(boardName).getRGB(0, 0, 800, 700, rgbArray, 0, 800);
                                }
                                synchronized (this) {
                                    for (int i = 0; i < rgbArray.length; i++) {
                                        writeSocket.write(rgbArray[i] + "\n");
                                        writeSocket.flush();
                                    }
                                }
                                checkBoards(boardNameOld);
                            } else {
                                synchronized (this) {
                                    writeSocket.write("CONNECT NOT FOUND\n");
                                    writeSocket.flush();
                                }
                            }
                        } else if (boardName != null) {
                            if(splitMessage[0].equals("MESSAGE"))
								continue;
                            //Рисование на доске
                            splitMessage = message.split(" ", 4);
                            int color = Integer.parseInt(splitMessage[0]);
                            int coordX = Integer.parseInt(splitMessage[1]);
                            int coordY = Integer.parseInt(splitMessage[2]);
                            int size = Integer.parseInt(splitMessage[3]);
                            synchronized (boards.get(boardName)) {
                                graphics.setColor(new Color(color));
                                graphics.fillOval(coordX, coordY, size, size);
                            }

                            //Всем, кто подключён
                            synchronized (usersForChat) {
                                for (String name : usersForChat.keySet()) {
									ClientThread iClient = usersForChat.get(name);
                                    if (iClient.boardName != null && iClient.boardName.equals(boardName)) {
                                        synchronized (iClient) {
                                            iClient.writeSocket.write(message + "\n");
                                            iClient.writeSocket.flush();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    synchronized (usersForChat) {
						usersForChat.remove(name);
                        synchronized (consoleSynch) {
							//log
							logger.toLog("Client (name: " + name + ") is unavailable");
                            System.out.println("Client is unavailable");
							//log
							logger.toLog("Number of clients: " + usersForChat.size());
                            System.out.println("Number of clients: " + usersForChat.size());
							if (usersForChat.size() == 0) {
								try {
									//log
									logger.toLog("SERVER WAS CLOSED");
									clientSocket.close();
									readSocket.close();
									writeSocket.close();
								} catch (Exception e) {
									System.out.println(e.getMessage());
								}
								System.exit(0);
							}
							
							for (String to : usersForChat.keySet()) {
								ClientThread toUser = usersForChat.get(to);
								toUser.writeSocket.write("MESSAGE @SERVER: User @" + name + " is unavailable\n");
								toUser.writeSocket.flush();
							}
                        }
						clientSocket.close();
						readSocket.close();
						writeSocket.close();
                    }
                    checkBoards(boardName);
                }
            } catch (Exception err) {
                synchronized (consoleSynch) {
					//log
					logger.toLog("ERROR: " + err.toString());
                    System.out.println(err.toString() + "\n");
                }
            }
        }

    }
	
	public Server(boolean test) {
		logger = new Logger();
		//запись в log
		if(test) {
			logger.toLog("TEST SERVER has been created");
			System.out.println("TEST SERVER has been created");
		}
		else
			logger.toLog("SERVER has been created");
		boards = new HashMap<String, BufferedImage>();
		usersForChat = new HashMap<String, ClientThread>();
		consoleSynch = new Object();	
		try {
			serverSocket = new ServerSocket(0);
			
			//log
			if (test) {
				logger.toLog("TEST SERVER PORT:" + serverSocket.getLocalPort());
				System.out.println("TEST SERVER PORT: " + serverSocket.getLocalPort());
				return;
			}
			logger.toLog("SERVER PORT:" + serverSocket.getLocalPort());
			System.out.println("PORT: " + serverSocket.getLocalPort());
			while (true) {
				Socket clientSocket = serverSocket.accept();
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String fromClient = in.readLine();
				synchronized(usersForChat) {
					if(fromClient.contains("@name ")) 
						fromClient = fromClient.replace("@name ", "");
					if((usersForChat.get(fromClient) != null) || (fromClient.equals("SERVER"))) { //если такой пользователь есть
						fromClient = "USER" + Integer.toString(autoName); //меняем имя
						autoName++;
					}
					ClientThread newClient = new ClientThread(clientSocket, fromClient);
					
					//log
					logger.toLog("User " + fromClient + " was added");
					
					out.write(fromClient + "\n");
					out.flush();
					usersForChat.put(fromClient, newClient);
					usersForChat.get(fromClient).start();
				}
			}
		} catch (IOException err) {
			
			//log
			logger.toLog("ERROR:" + err.getMessage());
			System.out.println(err.getMessage());
		}
	}
	
	public boolean isCreated() {
        return serverSocket.getLocalPort() > 0;
    }
	
	public boolean testLog() {
		return logger.toLog("TEST LOG");
	}
	
    public boolean checkBoards(String boardName) {
        if (boardName == null) {
            return false;
        }
        boolean boardUsed = false;
        for (String name : usersForChat.keySet()) {
				ClientThread iClient = usersForChat.get(name);
            synchronized (iClient) {
                if (iClient.boardName != null && iClient.boardName.equals(boardName)) {
                    boardUsed = true;
                    break;
                }
            }
        }
        if (!boardUsed) {
            synchronized (boards) {
                boards.remove(boardName);
                synchronized (consoleSynch) {
					//log
					logger.toLog("Desk \"" + boardName + "\" is not used and has been removed");
                    System.out.println("Desk \"" + boardName + "\" is not used and has been removed");
					//log
					logger.toLog("Number of desks: " + boards.size());
                    System.out.println("Number of desks: " + boards.size() + "\n");
                }
            }
			return false;
        }
		return true;
    }
	
	public boolean isClientsEmpty() {
        return usersForChat.size() == 0;
    }

    public boolean isBoardsEmpty() {
        return boards.size() == 0;
    }
	
	public boolean isNotAutoName() {
		if(autoName == 0)
			return false;
		return true;
	}
}