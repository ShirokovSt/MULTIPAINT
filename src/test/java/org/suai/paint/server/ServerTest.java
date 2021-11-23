package org.suai.paint.server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class ServerTest {
	@Test
	public void testPort() { //тест, создан ли сервер
        Server server = new Server(true);
        assertTrue(server.isCreated()); //если создан, то порт > 0
    }
	
	@Test 
	public void testLogger() {
		Server server = new Server(true);
		assertTrue(server.testLog());
	}
	
	@Test
	public void testCheckBoard() {
        Server server = new Server(true);
        assertFalse(server.checkBoards("TestBoard"));
        assertFalse(server.checkBoards(null));
    }
	
	@Test
    public void testBoardsEmpty(){
        Server server = new Server(true);
        assertTrue(server.isBoardsEmpty());
    }

    @Test
    public void testClientsEmpty(){
        Server server = new Server(true);
        assertTrue(server.isClientsEmpty());
    }
	
	 @Test
    public void testAutoName(){
        Server server = new Server(true);
        assertFalse(server.isNotAutoName());
    }
}