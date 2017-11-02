import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Ethermine implements ActionListener, WindowStateListener {

	static JFrame frame = new JFrame();
	static JPanel panel = new JPanel(new GridLayout(0, 1));
	static JLabel cH;
	static JLabel aH;
	static JLabel pO;
	static JLabel timeToNext;
	static JButton button;
	static JButton openEthermine;
	String publicKey = "";
	long prevTimeMillis = 0;
	TrayIcon icon;
	MenuItem maximizeItem;
	MenuItem quitItem;
	CheckboxMenuItem notifications;
	MenuItem currHashItem;
	MenuItem avgHashItem;
	MenuItem timeToNextItem;

	public static void main(String[] args) throws IOException {
		new Ethermine().run();
	}

	void run() throws IOException {
		publicKey = JOptionPane.showInputDialog("Enter your Ethereum public key:");
		URL url = new URL("https://api.ethermine.org/miner/" + publicKey + "/currentStats");
		HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
		httpsURLConnection.addRequestProperty("User-Agent", "Mozilla/4.76");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line).append("\n");
		}
		bufferedReader.close();
		System.out.println("API Response Worker: " + stringBuilder.toString());
		String output = stringBuilder.toString();

		HttpsURLConnection settingsConnection = (HttpsURLConnection) new URL(
				"https://api.ethermine.org/miner/" + publicKey + "/settings").openConnection();
		settingsConnection.addRequestProperty("User-Agent", "Mozilla/4.76");
		BufferedReader readerSettings = new BufferedReader(new InputStreamReader(settingsConnection.getInputStream()));
		StringBuilder settingsBuilder = new StringBuilder();
		String set;
		while ((set = readerSettings.readLine()) != null) {
			settingsBuilder.append(set).append("\n");
		}
		bufferedReader.close();
		System.out.println("API Response Settings: " + settingsBuilder.toString());
		String settings = settingsBuilder.toString();

		cH = new JLabel(
				"Current Hashrate: " + Double.parseDouble(getValue(output, "currentHashrate")) / 1000000.0 + " MH/s");
		aH = new JLabel(
				"Average Hashrate: " + Double.parseDouble(getValue(output, "averageHashrate")) / 1000000.0 + " MH/s");
		pO = new JLabel(
				"Min Payout: " + Double.parseDouble(getValue(settings, "minPayout")) / 1000000000000000000.0 + " ETH");
		prevTimeMillis = System.currentTimeMillis();
		timeToNext = new JLabel("Update in: " + formatTiming(210000 - System.currentTimeMillis() + prevTimeMillis,
				210000 - System.currentTimeMillis() + prevTimeMillis));
		button = new JButton("Refresh");
		openEthermine = new JButton("Open ethermine.org");
		button.addActionListener(this);
		openEthermine.addActionListener(this);
		panel.add(cH);
		panel.add(aH);
		panel.add(pO);
		panel.add(timeToNext);
		panel.add(button);
		panel.add(openEthermine);
		frame.add(panel);
		frame.setIconImage(new ImageIcon(getClass().getResource("bar.gif")).getImage());
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		prevTimeMillis = System.currentTimeMillis();
		PopupMenu popup = new PopupMenu();
		icon = new TrayIcon(createImage("tray.gif", "Ethereum Monitor"));
		icon.setImageAutoSize(true);
		maximizeItem = new MenuItem("Maximize");
		maximizeItem.addActionListener(this);
		notifications = new CheckboxMenuItem("Notify me of changes");
		notifications.setState(true);
		quitItem = new MenuItem("Exit Ethereum Monitor");
		quitItem.addActionListener(this);
		currHashItem = new MenuItem(
				"Curr: " + Math.floor(Double.parseDouble(getValue(output, "currentHashrate")) / 1000000.0 * 100) / 100
						+ " MH/s");
		avgHashItem = new MenuItem("Avg: "
				+ Math.floor(Double.parseDouble(getValue(output, "averageHashrate")) / 1000000.0 * 100) / 100 + " MH/s");
		timeToNextItem = new MenuItem("Update in: " + formatTiming(210000 - System.currentTimeMillis() + prevTimeMillis,
				210000 - System.currentTimeMillis() + prevTimeMillis));
		popup.add(quitItem);
		popup.add(maximizeItem);
		popup.addSeparator();
		popup.add(notifications);
		popup.add(timeToNextItem);
		popup.addSeparator();
		popup.add(currHashItem);
		popup.add(avgHashItem);
		icon.setPopupMenu(popup);
		frame.addWindowStateListener(this);
		frame.setResizable(false);
		frame.setTitle("Ethermine Monitor");
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (System.currentTimeMillis() - 210000 > prevTimeMillis) {
						prevTimeMillis = System.currentTimeMillis();
						try {
							rePanel(true);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						try {
							rePanel(false);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	public static String getValue(String main, String locator) {
		main = main.substring(main.indexOf(locator));
		main = main.substring(main.indexOf(":") + 1, main.indexOf(","));
		return main;
	}

	public void rePanel(boolean updateHashrates) throws IOException, InterruptedException {
		if (updateHashrates) {
			URL url = new URL("https://api.ethermine.org/miner/" + publicKey + "/currentStats");
			HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
			httpsURLConnection.addRequestProperty("User-Agent", "Mozilla/4.76");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(httpsURLConnection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}
			bufferedReader.close();
			System.out.println("API Response: " + stringBuilder.toString());
			String output = stringBuilder.toString();
			cH.setText("Current Hashrate: " + Double.parseDouble(getValue(output, "currentHashrate")) / 1000000.0
					+ " MH/s");
			aH.setText("Average Hashrate: " + Double.parseDouble(getValue(output, "averageHashrate")) / 1000000.0
					+ " MH/s");

			if (notifications.getState()) {
				icon.displayMessage(
						"Current Hashrate: " + Double.parseDouble(getValue(output, "currentHashrate")) / 1000000.0
								+ " MH/s",
						"Average Hashrate: " + Double.parseDouble(getValue(output, "averageHashrate")) / 1000000.0
								+ " MH/s",
						MessageType.INFO);
			}

			currHashItem.setLabel("Curr: "
					+ Math.floor(Double.parseDouble(getValue(output, "currentHashrate")) / 1000000.0 * 100) / 100
					+ " MH/s");

			avgHashItem.setLabel("Avg: "
					+ Math.floor(Double.parseDouble(getValue(output, "averageHashrate")) / 1000000.0 * 100) / 100
					+ " MH/s");
		}
		timeToNext.setText("Update in: " + formatTiming(210000 - System.currentTimeMillis() + prevTimeMillis,
				210000 - System.currentTimeMillis() + prevTimeMillis));
		timeToNextItem.setLabel("Update in: " + formatTiming(210000 - System.currentTimeMillis() + prevTimeMillis,
				210000 - System.currentTimeMillis() + prevTimeMillis));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(button)) {
			try {
				rePanel(false);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		} else if (e.getSource().equals(openEthermine)) {
			try {
				Desktop.getDesktop().browse(new URI("http://ethermine.org/miners/" + publicKey));
			} catch (IOException | URISyntaxException e1) {
				e1.printStackTrace();
			}
		} else if (e.getSource().equals(maximizeItem)) {
			frame.setState(JFrame.NORMAL);
			frame.setVisible(true);
			frame.toFront();
			SystemTray.getSystemTray().remove(icon);
		} else if (e.getSource().equals(quitItem)) {
			SystemTray.getSystemTray().remove(icon);
			System.exit(0);
		}
	}

	protected static Image createImage(String path, String description) {
		URL imageURL = Ethermine.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		if (e.getSource().equals(frame)) {
			if (e.getNewState() == 1) {
				frame.setVisible(false);
				try {
					SystemTray.getSystemTray().add(icon);
				} catch (AWTException e1) {
					e1.printStackTrace();
				}
				icon.displayMessage("Minimized to System Tray",
						"I'll continue to alert you about changes.\nRight click the icon to maximize.",
						MessageType.INFO);
			}
		}
	}

	private static String formatTiming(long timing, long maximum) {
		timing = Math.min(timing, maximum) / 1000;

		long seconds = timing % 60;
		timing /= 60;
		long minutes = timing % 60;
		timing /= 60;
		long hours = timing;

		if (maximum >= 3600000L) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%d:%02d", minutes, seconds);
		}
	}
}
