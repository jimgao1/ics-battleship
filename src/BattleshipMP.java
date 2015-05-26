import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class BattleshipMP extends JFrame implements ActionListener, Runnable{

	/*
	 * 		TCP/IP stuff
	 */
	public ServerSocket serverSock;
	
	public Socket sock;
	public BufferedReader reader;
	public PrintWriter writer;
	
	/*
	 * 		GUI components
	 */
	
	public JButton[][] opponentButtonGrid = new JButton[10][10];
	public JButton[][] playerButtonGrid = new JButton[10][10];

	public JPanel pnlPlayer = new JPanel();
	public JPanel pnlOpponent = new JPanel();
	public JPanel pnlControls = new JPanel();
	public JPanel pnlHeader = new JPanel();

	public JLabel lblStatus = new JLabel("This is the current status");

	public JLabel lblLeft = new JLabel("Opponent's Grid", JLabel.CENTER);
	public JLabel lblRight = new JLabel("Your Grid", JLabel.CENTER);
	public JLabel lblCenter = new JLabel("Controls", JLabel.CENTER);
	
	/*
	 * 		Game Information
	 */
	
	/*
	 * 	~~~GRID LEGEND~~~
	 * 
	 * 		0			Default
	 * 		1 ~ n+1		Hit
	 * 		-1			Previous Turn
	 * 		-2			Miss
	 */
	
	public int[][] playerGrid = new int[10][10];
	public int[][] opponentGrid = new int[10][10];
	
	public int[][] playerStatusGrid = new int[10][10];
	public int[][] opponentStatusGrid = new int[10][10];
	
	public boolean playerTurn;
	
	/*
	 * 	Constructor as SERVER
	 */
	public BattleshipMP(int port){
		try{
			serverSock = new ServerSocket(port);
			
			JOptionPane.showMessageDialog(null, "<html>Waiting for client to connect. IP: <b>" + serverSock.getInetAddress() + "</b></html>");
			
			sock = serverSock.accept();
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new PrintWriter(sock.getOutputStream(), true);
			
			System.out.println("[INFO]\tConnection Established. IP:" + sock.getInetAddress());
			playerTurn = true;
			
			initGUI();
		} catch (IOException ex){
			System.out.println("[ERROR]\tConnection Error. " + ex.getLocalizedMessage());
			JOptionPane.showMessageDialog(null, "Connection cannot be established.");
			this.dispose();
		}
	}
	
	/*
	 * 	Constructor as CLIENT
	 */
	public BattleshipMP(String serverIP, int port){
		try{
			sock = new Socket(serverIP, port);
			
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new PrintWriter(sock.getOutputStream(), true);
			
			System.out.println("[INFO]\tConnection Established (CLIENT). IP: " + sock.getInetAddress());
			playerTurn = false;
			
			initGUI();
		} catch (IOException ex){
			System.out.println("[ERROR]\tConnection Error. " + ex.getLocalizedMessage());
			JOptionPane.showMessageDialog(null, "Connection cannot be established.");
			this.dispose();
		}
	}
	
	public void initGUI(){
		this.setTitle("Battleship");
		this.setSize(1200, 500);
		this.setVisible(true);
		this.setLayout(new BorderLayout(20, 5));
		this.setLocation(100, 100);
		this.setResizable(false);

		pnlHeader.setLayout(new BorderLayout());

		lblLeft.setFont(new Font("Courier New", Font.BOLD, 16));
		lblRight.setFont(new Font("Courier New", Font.BOLD, 16));
		lblCenter.setFont(new Font("Courier New", Font.BOLD, 16));

		pnlHeader.add(lblLeft, BorderLayout.WEST);
		pnlHeader.add(lblRight, BorderLayout.EAST);
		pnlHeader.add(lblCenter, BorderLayout.CENTER);

		this.add(pnlHeader, BorderLayout.NORTH);

		pnlOpponent.setLayout(new GridLayout(10, 10));
		pnlOpponent.setPreferredSize(new Dimension(400, 400));

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				opponentButtonGrid[i][j] = new JButton();
				opponentButtonGrid[i][j].setActionCommand(i + " " + j);
				opponentButtonGrid[i][j].addActionListener(this);
				pnlOpponent.add(opponentButtonGrid[i][j]);
			}
		}

		this.add(pnlOpponent, BorderLayout.WEST);

		pnlPlayer.setLayout(new GridLayout(10, 10));
		pnlPlayer.setPreferredSize(new Dimension(400, 400));

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				playerButtonGrid[i][j] = new JButton();
				playerButtonGrid[i][j].setActionCommand(i + " " + j);
				playerButtonGrid[i][j].setEnabled(false);
				pnlPlayer.add(playerButtonGrid[i][j]);
			}
		}

		this.add(pnlPlayer, BorderLayout.EAST);

		pnlControls.setLayout(new GridLayout(2, 1));
		
		this.add(pnlControls, BorderLayout.CENTER);

		lblStatus.setFont(new Font("Courier New", Font.BOLD, 16));
		this.add(lblStatus, BorderLayout.SOUTH);
		
		this.repaint();
		this.revalidate();
	}
	
	public void generateRandomPlacement() {
		for (int i = 0; i < 10; i++)
			for (int j = 0; j < 10; j++)
				opponentGrid[i][j] = 0;

		for (int i = 0; i < AlgorithmMain.shipCount; i++) {
			int pX = (int) (Math.random() * 10), pY = (int) (Math.random() * 10);
			boolean vert = (Math.random() >= 0.5);
			boolean valid = true;

			if (vert) {
				if (pY + AlgorithmMain.shipSize[i][0] - 1 < 10) {
					for (int j = pY; j < pY + AlgorithmMain.shipSize[i][0]; j++)
						if (opponentGrid[pX][j] != 0) {
							valid = false;
							break;
						}
				} else {
					valid = false;
				}

				if (valid) {
					for (int j = pY; j < pY + AlgorithmMain.shipSize[i][0]; j++)
						opponentGrid[pX][j] = i + 1;
				} else {
					i--;
					continue;
				}
			} else {
				if (pX + AlgorithmMain.shipSize[i][0] - 1 < 10) {
					for (int j = pX; j < pX + AlgorithmMain.shipSize[i][0]; j++)
						if (opponentGrid[j][pY] != 0) {
							valid = false;
							break;
						}
				} else {
					valid = false;
				}

				if (valid) {
					for (int j = pX; j < pX + AlgorithmMain.shipSize[i][0]; j++)
						opponentGrid[j][pY] = i + 1;
				} else {
					i--;
					continue;
				}
			}
		}

		System.out.println("[INFO]\tGenerated Opponent Placement");
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++)
				System.out.printf("%d ", opponentGrid[j][i]);
			System.out.println();
		}
	}
	
	public void nextTurn(){
		if (playerTurn){
			lblStatus.setText("Your Turn");
			
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					opponentButtonGrid[i][j].setEnabled(false);
					
					if (opponentStatusGrid[i][j] == -2){
						opponentButtonGrid[i][j].setIcon(new ImageIcon("water.png"));
					} else if (opponentStatusGrid[i][j] == -1 || 
										opponentStatusGrid[i][j] >= 1){
						opponentButtonGrid[i][j].setIcon(new ImageIcon("fire.png"));
					} else {
						opponentButtonGrid[i][j].setEnabled(true);
					}
				}
			}
		} else {
			lblStatus.setText("Enemy's Turn");
			
			String cmd = "";
			
			try {
				cmd = reader.readLine();
			} catch (IOException e) {
				System.out.println("[ERROR]\tRead Error: " + e.getLocalizedMessage());
				JOptionPane.showMessageDialog(null, "Socket read error");
				this.dispose();
			}
			
			int hitLocationX = Integer.parseInt(cmd.split(" ")[0]);
			int hitLocationY = Integer.parseInt(cmd.split(" ")[1]);
			
			if (playerGrid[hitLocationX][hitLocationY] == 0){
				writer.println("miss");
				playerButtonGrid[hitLocationX][hitLocationY].setIcon(new ImageIcon("water.png"));
			} else {
				writer.println("hit");
				playerButtonGrid[hitLocationX][hitLocationY].setIcon(new ImageIcon("fire.png"));
			}
			
			nextTurn();
		}
	}
	
	

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	
}
